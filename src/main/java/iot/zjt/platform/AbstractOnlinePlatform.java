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
 * @since 2020/12/31
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

            noNeedToCreate:
            for (Repository from : fromReposFuture.result()) {
                for (Repository existRepo : toReposFuture.result()) {
                    if (existRepo.getName().equals(from.getName())) {
                        // found matched repo with names
                        repoMapper.put(from, existRepo);
                        continue noNeedToCreate;
                    }
                }

                // create a new repository
                // the id of new repository shall be overwrite lately.
                Repository newRepo = new Repository(from);
                createRepoFutures.add(targetPlatform.createRepository(newRepo));
                repoMapper.put(from, newRepo);
                break;
            }

            return CompositeFuture
                    .all(createRepoFutures)
                    .compose(createComplete -> Future.succeededFuture(repoMapper));

//            return CompositeFuture.all(createRepoFutures).compose(temp -> {
//                return Future.failedFuture("ok");
//            });
        }).compose(repoMapper -> {
            // mirror repo list
            List<Future> mirrorRepoFutures = new ArrayList<>();
            FileSystem fs = vertx.fileSystem();

            for (Map.Entry<Repository, Repository> entry : repoMapper.entrySet()) {
                Repository from = entry.getKey();
                Repository to = entry.getValue();

                logger.info("Start to mirror " + from.getName() + " from " +
                        getPlatform() + " to " + targetPlatform.getPlatform());

                Future<?> future = fs.createTempDirectory("").compose(dirStr -> {
                    logger.warn(dirStr);

                    return vertx.executeBlocking(promise -> {
                        File dir = new File(dirStr);
                        try {
                            logger.info("Cloning from: ");
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

                            logger.info("Pushing mirror to: ");
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

                        } catch (GitAPIException | IOException e) {
                            logger.error(e.getMessage());
                            promise.fail(e.getMessage());
                        }
                        logger.info("Complete.");
                        promise.complete();
                    });
                });

                mirrorRepoFutures.add(future);
                break; // only for debug
            }

            return CompositeFuture.all(mirrorRepoFutures);
        }).onFailure(throwable -> {
            logger.error(throwable.getMessage());
        });
    }
}
