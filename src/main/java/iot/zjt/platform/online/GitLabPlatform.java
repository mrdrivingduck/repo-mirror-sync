package iot.zjt.platform.online;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import iot.zjt.platform.AbstractOnlinePlatform;
import iot.zjt.platform.PlatformUser;
import iot.zjt.repo.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform operation of GitLab.
 *
 * @author Mr Dk.
 * @since 2020/12/28
 */
public class GitLabPlatform extends AbstractOnlinePlatform {

    private final static Logger logger = LogManager.getLogger(GitLabPlatform.class);

    public GitLabPlatform(final Vertx vertx, final PlatformUser user) {
        super(vertx, user);
    }

    // curl --header "Authorization: Bearer bc_x3rjszZx1dbguqEvr" -X POST -d 'name=hahaha&visibility=private' "https://gitlab.com/api/v4/projects"
    @Override
    public Future<Void> createRepository(Repository repo) {
        logger.info(getPlatform() + " create repo");
        return Future.succeededFuture();
    }

    // curl --header "Authorization: Bearer bc_x3rjszZx1dbguqEvr" -X DELETE "https://gitlab.com/api/v4/projects/mrdrivingduck%2Fcare-model-manager"
    @Override
    public Future<Void> deleteRepository(Repository repo) {
        logger.info(getPlatform() + " delete repo");
        return Future.succeededFuture();
    }

    @Override
    public Future<List<Repository>> getRepositories(boolean includePrivate) {
        logger.info(getPlatform() + " get repo");
        List<Repository> repos = new ArrayList<>();
        Repository repo = new Repository();
        repo.setOwner("mrdrivingduck");
        repo.setName("emotions");
        repo.setVisibilityPrivate(true);
        repos.add(repo);
        return Future.succeededFuture(repos);
    }

    @Override
    public String getPlatform() {
        return "GitLab";
    }
}
