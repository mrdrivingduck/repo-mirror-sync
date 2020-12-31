package iot.zjt.platform;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import iot.zjt.repo.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
        Future<List<Repository>> reposFuture = this.getRepositories(includePrivate);
        Future<List<Repository>> targetReposFuture = targetPlatform.getRepositories(true);

        return CompositeFuture.all(reposFuture, targetReposFuture).compose(v -> {
            List<Future> createRepoFutures = new ArrayList<>();

            needCreate:
            for (Repository from : reposFuture.result()) {
                for (Repository existRepo : targetReposFuture.result()) {
                    if (existRepo.getName().equals(from.getName())) {
                        // found matched repo with names
                        continue needCreate;
                    }
                }

                // create
                createRepoFutures.add(targetPlatform.createRepository(from));
            }

            return CompositeFuture.all(createRepoFutures);
        }).compose(v -> {
            // mirror repo list

            for (Repository from : reposFuture.result()) {
                logger.info(from.getName());
            }

            return Future.succeededFuture();
        }).onFailure(throwable -> {
            logger.error(throwable.getMessage());
        });
    }
}
