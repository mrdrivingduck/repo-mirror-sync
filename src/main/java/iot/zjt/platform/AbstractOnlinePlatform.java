package iot.zjt.platform;

import io.vertx.core.Vertx;
import iot.zjt.repo.Repository;

import java.util.List;

public abstract class AbstractOnlinePlatform implements OnlinePlatform {

    private final Vertx vertx;
    private final PlatformUser user;

    public AbstractOnlinePlatform(final Vertx vertx, final PlatformUser user) {
        this.user = user;
        this.vertx = vertx;
    }

    @Override
    public abstract boolean createRepository(Repository repo);

    @Override
    public abstract boolean deleteRepository(Repository repo);

    @Override
    public abstract List<Repository> getRepositories(boolean includePrivate);

    @Override
    public abstract String getPlatform();

    public boolean mirrorAllRepoTo(AbstractOnlinePlatform targetPlatform,
                                   boolean includePrivate, boolean removeNonExist) {
        List<Repository> repos = this.getRepositories(includePrivate);
        if (repos == null) {
            System.err.println("Fail to fetch repositories.");
            return false;
        }

        List<Repository> targetPlatformRepos = targetPlatform.getRepositories(true);
        if (removeNonExist) {
            System.out.println("Dangerous option, skip it.");
        }

        needCreate:
        for (Repository from : repos) {
            for (Repository existRepo : targetPlatformRepos) {
                if (existRepo.getName().equals(from.getName())) {
                    // found matched repo with names
                    continue needCreate;
                }
            }

            // create
            if (!targetPlatform.createRepository(from)) {
                System.err.println("Fail to create mirror repo on " + targetPlatform.getPlatform());
                return false;
            }
        }

        for (Repository from : repos) {
            // mirror to
        }

        return true;
    }
}
