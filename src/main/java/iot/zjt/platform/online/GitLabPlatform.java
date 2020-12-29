package iot.zjt.platform.online;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
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
 * @since 2020/12/29
 */
public class GitLabPlatform extends AbstractOnlinePlatform {

    private final static Logger logger = LogManager.getLogger(GitLabPlatform.class);

    public GitLabPlatform(final Vertx vertx, final PlatformUser user) {
        super(vertx, user);
    }

    /**
     * To create a GitLab repository through GitLab API.
     * The end point is "https://gitlab.com/api/v4/projects" with POST method,
     * 201 should be returned.
     *
     * @param repo The repository to be created.
     * @return The future object of result.
     */
    @Override
    public Future<Void> createRepository(Repository repo) {
        WebClient client = WebClient.create(getVertx());

        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.set("name", repo.getName());
        form.set("visibility", repo.getVisibilityPrivate() ? "private" : "public");

        return client
                .postAbs("https://gitlab.com/api/v4/projects")
                .bearerTokenAuthentication(getUser().getToken())
                .sendForm(form)
                .onFailure(err -> {
                    logger.error(new StringBuilder()
                            .append(getPlatform())
                            .append(" responses: ")
                            .append(err.getMessage())
                    );
                })
                .compose(res -> {
                    JsonObject body = res.bodyAsJsonObject();
                    String log = getPlatform() + " responses " + res.statusCode() +
                            " : " + (body != null ? body.toString() : "");

                    if (res.statusCode() == 201) {
                        logger.info(log);
                    } else {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }

                    return Future.succeededFuture();
                });
    }

    // curl --header "Authorization: Bearer bc_x3rjszZx1dbguqEvr" -X DELETE "https://gitlab.com/api/v4/projects/mrdrivingduck%2Fcare-model-manager"

    /**
     * To delete a GitLab repository through GitLab API.
     * The end point is "https://gitlab.com/api/v4/projects" with DELETE method,
     * 202 should be returned.
     *
     * @param repo The repository to be deleted.
     * @return The future object of result.
     */
    @Override
    public Future<Void> deleteRepository(Repository repo) {
        WebClient client = WebClient.create(getVertx());

        return client
                .deleteAbs("https://gitlab.com/api/v4/projects/" + repo.getOwner() + "%2F" + repo.getName())
                .bearerTokenAuthentication(getUser().getToken())
                .send()
                .onFailure(err -> {
                    logger.error(new StringBuilder()
                            .append(getPlatform())
                            .append(" responses: ")
                            .append(err.getMessage())
                    );
                })
                .compose(res -> {
                    JsonObject body = res.bodyAsJsonObject();
                    String log = getPlatform() + " responses " + res.statusCode() +
                            " : " + (body != null ? body.toString() : "");

                    if (res.statusCode() == 202) {
                        logger.info(log);
                    } else {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }

                    return Future.succeededFuture();
                });
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
