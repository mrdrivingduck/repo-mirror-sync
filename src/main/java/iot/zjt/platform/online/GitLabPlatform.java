package iot.zjt.platform.online;

import io.vertx.core.Vertx;
import iot.zjt.platform.AbstractOnlinePlatform;
import iot.zjt.platform.PlatformUser;
import iot.zjt.repo.Repository;

import java.util.List;

public class GitLabPlatform extends AbstractOnlinePlatform {

    public GitLabPlatform(final Vertx vertx, final PlatformUser user) {
        super(vertx, user);
    }

    // curl --header "Authorization: Bearer bc_x3rjszZx1dbguqEvr" -X POST -d 'name=hahaha&visibility=private' "https://gitlab.com/api/v4/projects"
    @Override
    public boolean createRepository(Repository repo) {
        return false;
    }

    // curl --header "Authorization: Bearer bc_x3rjszZx1dbguqEvr" -X DELETE "https://gitlab.com/api/v4/projects/mrdrivingduck%2Fcare-model-manager"
    @Override
    public boolean deleteRepository(Repository repo) {
        return false;
    }

    @Override
    public List<Repository> getRepositories(boolean includePrivate) {
        return null;
    }

    @Override
    public String getPlatform() {
        return "GitLab";
    }
}
