package iot.zjt.platform;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import iot.zjt.Repository;
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
 * @since 2021/01/02
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
    public abstract Future<Void> updateRepository(Repository repo);

    @Override
    public abstract Future<List<Repository>> getRepositories(boolean includePrivate);

    @Override
    public abstract String getPlatform();

    @Override
    public abstract String getRepositoryHttpsUrl(Repository repo);

    @SuppressWarnings("rawtypes")
    public Future<?> mirrorAllRepoTo(AbstractOnlinePlatform targetPlatform,
                                     boolean includePrivate, boolean removeNonExist) {
        /*
         * Step 1:
         * Fetch repositories from platforms.
         */
        logger.warn("Step 1: fetching repositories from {" + getPlatform() +
                "} and {" + targetPlatform.getPlatform() + "} ...");
        Future<List<Repository>> fromReposFuture = this.getRepositories(includePrivate);
        Future<List<Repository>> toReposFuture = targetPlatform.getRepositories(true);

        return CompositeFuture.all(fromReposFuture, toReposFuture).compose(v -> {
            /*
             * Step 2:
             * Create empty repositories if necessary.
             */
            logger.warn("Step 2: Creating repositories on {" + targetPlatform.getPlatform() + "} ...");
            List<Future> createRepoFutures = new ArrayList<>();
            Map<Repository, Repository> repoMapper = new HashMap<>();

            needToCreate:
            for (Repository from : fromReposFuture.result()) {
                for (Repository existRepo : toReposFuture.result()) {
                    if (existRepo.getName().equals(from.getName())) {
                        // found matched repo with names.
                        repoMapper.put(from, existRepo);
                        continue needToCreate;
                        // no need to create repo.
                    }
                }

                // create a new repository.
                // the id of new repository shall be overwrite lately.
                Repository newRepo = new Repository(from);
                createRepoFutures.add(targetPlatform.createRepository(newRepo));
                repoMapper.put(from, newRepo);
            }

            return CompositeFuture
                    .all(createRepoFutures)
                    .compose(createComplete -> Future.succeededFuture(repoMapper));
        }).compose(repoMapper -> {
            /*
             * Step 3:
             * Update repository visibility if necessary.
             */
            logger.warn("Step 3: updating repository visibility ...");
            List<Future> updateFutures = new ArrayList<>();

            for (Map.Entry<Repository, Repository> entry : repoMapper.entrySet()) {
                Repository from = entry.getKey();
                Repository to = entry.getValue();

                if (from.getVisibilityPrivate() != to.getVisibilityPrivate()) {
                    to.setVisibilityPrivate(from.getVisibilityPrivate());
                    updateFutures.add(targetPlatform.updateRepository(to));
                }
            }

            return CompositeFuture.all(updateFutures)
                    .compose(updateComplete -> Future.succeededFuture(repoMapper));
        }).compose(repoMapper -> {
            /*
             * Step 4:
             * Start to mirror the repository list.
             */
            logger.warn("Step 4: Start mirroring repositories ...");

            FileSystem fs = vertx.fileSystem();
            Future<?> mirrorRepoFuture = Future.succeededFuture(); // one by one.

            for (Map.Entry<Repository, Repository> entry : repoMapper.entrySet()) {
                Repository from = entry.getKey();
                Repository to = entry.getValue();

                mirrorRepoFuture
                        .compose(v -> fs.createTempDirectory("")) // no need to remove by hand.
                        .compose(dirStr -> vertx.executeBlocking(promise -> {
                                logger.warn("Start to mirror [" + from.getName() + "] from {" +
                                        getPlatform() + "} to {" + targetPlatform.getPlatform() +
                                        "} at " + dirStr);

                                File dir = new File(dirStr);
                                try {
                                    logger.warn("Cloning from: [" + from.getName() +
                                            "] on " + getPlatform());
                                    // $ git clone --bare {from.git.url}
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
                                    logger.info("Clone [" + from.getName() + "] success");

                                    logger.warn("Pushing mirror to: [" + to.getName() +
                                            "] on " + targetPlatform.getPlatform());
                                    // $ git push --mirror {to.git.url}
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
                                    logger.info("Push [" + to.getName() + "] success");

                                } catch (GitAPIException | IOException e) {
                                    logger.error(e.getMessage());
                                    promise.fail(e.getMessage());
                                }
                                logger.info("Mirroring [" + from.getName() + "] complete.");
                                promise.complete();
                            }));
            }

            return mirrorRepoFuture;
        });
    }
}
