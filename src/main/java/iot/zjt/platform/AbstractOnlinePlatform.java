package iot.zjt.platform;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import iot.zjt.repo.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract concept of online SCM platform.
 *
 * @author Mr Dk.
 * @since 2021/01/01
 */
public abstract class AbstractOnlinePlatform implements OnlinePlatform {

    private final static Logger logger = LogManager.getLogger(AbstractOnlinePlatform.class);

    private final Vertx vertx;
    private final PlatformUser user;

    public AbstractOnlinePlatform(final Vertx vertx, final PlatformUser user) {
        this.user = user;
        this.vertx = vertx;
    }

    protected PlatformUser getUser() {
        return user;
    }

    protected Vertx getVertx() {
        return vertx;
    }

    @Override
    public abstract Future<Void> createRepository(Repository repo);

    @Override
    public abstract Future<Void> deleteRepository(Repository repo);

    @Override
    public abstract Future<List<Repository>> getRepositories(boolean includePrivate);

    @Override
    public abstract String getPlatform();

    @Override
    public abstract String getRepositoryHttpsUrl(Repository repo);

    @SuppressWarnings("rawtypes")
    public Future<?> mirrorAllRepoTo(AbstractOnlinePlatform targetPlatform,
                                     boolean includePrivate, boolean removeNonExist) {
        Future<List<Repository>> fromReposFuture = this.getRepositories(includePrivate);
        Future<List<Repository>> toReposFuture = targetPlatform.getRepositories(true);

        return CompositeFuture.all(fromReposFuture, toReposFuture).compose(v -> {
            List<Future> createRepoFutures = new ArrayList<>();
            Map<Repository, Repository> repoMapper = new HashMap<>();

            needToCreate:
            for (Repository from : fromReposFuture.result()) {
                for (Repository existRepo : toReposFuture.result()) {
                    if (existRepo.getName().equals(from.getName())) {
                        // found matched repo with names
                        repoMapper.put(from, existRepo);
                        continue needToCreate;
                    }
                }

                // create a new repository
                // the id of new repository shall be overwrite lately.
                Repository newRepo = new Repository(from);
                createRepoFutures.add(targetPlatform.createRepository(newRepo));
                repoMapper.put(from, newRepo);
            }

            return CompositeFuture
                    .all(createRepoFutures)
                    .compose(createComplete -> Future.succeededFuture(repoMapper));
        }).compose(repoMapper -> {
            // start to mirror the repository list
            FileSystem fs = vertx.fileSystem();
            // List<Future> mirrorRepoFutures = new ArrayList<>();
            Future<?> mirrorRepoFuture = Future.succeededFuture(); // one by one

            for (Map.Entry<Repository, Repository> entry : repoMapper.entrySet()) {
                Repository from = entry.getKey();
                Repository to = entry.getValue();

                mirrorRepoFuture.compose(v -> fs.createTempDirectory("")).compose(dirStr -> {
                    logger.warn("Start to mirror " + from.getName() + " from " +
                            getPlatform() + " to " + targetPlatform.getPlatform() +
                            " at " + dirStr);

                    return vertx.executeBlocking(promise -> {
                        File dir = new File(dirStr);
                        try {
                            logger.warn("Cloning from: " + from.getName() +
                                    " on " + getPlatform());
                            Git.cloneRepository()
                                    .setCredentialsProvider(
                                            new UsernamePasswordCredentialsProvider(
                                                    getUser().getUsername(),
                                                    getUser().getToken()
                                            ))
                                    .setURI(getRepositoryHttpsUrl(from))
                                    .setBare(true)
                                    .setDirectory(dir)
                                    .call();
                            logger.info("Clone success");

                            logger.warn("Pushing mirror to: " + to.getName() +
                                    " on " + targetPlatform.getPlatform());
                            Git.open(dir).push()
                                    .setCredentialsProvider(
                                            new UsernamePasswordCredentialsProvider(
                                                    targetPlatform.getUser().getUsername(),
                                                    targetPlatform.getUser().getToken()
                                            ))
                                    .setRemote(targetPlatform.getRepositoryHttpsUrl(to))
                                    .setForce(true)
                                    .add("refs/*:refs/*")
                                    .call();
                            logger.info("Push success");

                        } catch (GitAPIException | IOException e) {
                            logger.error(e.getMessage());
                            promise.fail(e.getMessage());
                        }
                        logger.info("Mirroring " + from.getName() + " complete.");
                        promise.complete();
                    });
                });

                // mirrorRepoFutures.add(future);
                // break; // only for debug
            }

            return mirrorRepoFuture;
        });
    }
}
