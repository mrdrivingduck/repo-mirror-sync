package iot.zjt.platform.online;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
 * @since 2020/12/30
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
        logger.info("Trying to create repository on " + getPlatform());
        WebClient client = WebClient.create(getVertx());

        MultiMap form = MultiMap.caseInsensitiveMultiMap();
        form.set("name", repo.getName());
        form.set("visibility", repo.getVisibilityPrivate() ? "private" : "public");

        return client
                .postAbs("https://gitlab.com/api/v4/projects")
                .bearerTokenAuthentication(getUser().getToken())
                .sendForm(form)
                .onFailure(err -> {
                    // network failure.
                    logger.error(new StringBuilder()
                            .append(getPlatform())
                            .append(" responses: ")
                            .append(err.getMessage())
                    );
                })
                .compose(response -> {
                    // network success, but the result is unknown.
                    JsonObject body = response.bodyAsJsonObject();
                    String log = getPlatform() + " responses " + response.statusCode() +
                            " : " + (body != null ? body.toString() : "");

                    if (response.statusCode() == 201) {
                        logger.info(log);
                    } else {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }

                    return Future.succeededFuture();
                });
    }

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
        logger.info("Trying to delete repository on " + getPlatform());
        WebClient client = WebClient.create(getVertx());

        return client
                .deleteAbs("https://gitlab.com/api/v4/projects/" + repo.getOwner() + "%2F" + repo.getName())
                .bearerTokenAuthentication(getUser().getToken())
                .send()
                .onFailure(err -> {
                    // network failure.
                    logger.error(new StringBuilder()
                            .append(getPlatform())
                            .append(" responses: ")
                            .append(err.getMessage())
                    );
                })
                .compose(response -> {
                    // network success, but the result is unknown.
                    JsonObject body = response.bodyAsJsonObject();
                    String log = getPlatform() + " responses " + response.statusCode() +
                            " : " + (body != null ? body.toString() : "");

                    if (response.statusCode() == 202) {
                        logger.info(log);
                    } else {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }

                    return Future.succeededFuture();
                });
    }

    /**
     * To get all GitLab repositories of a user through GitLab API.
     * The end point is "https://gitlab.com/api/v4/users/:user_id/projects"
     * with GET method. 200 should be returned.
     *
     * @param includePrivate Whether or not to get private repositories.
     * @return The future object containing all repository information.
     */
    @Override
    public Future<List<Repository>> getRepositories(boolean includePrivate) {
        logger.info("Trying to get repositories from " + getPlatform());

        List<Repository> repos = new ArrayList<>();

        WebClient client = WebClient.create(getVertx());
        return client
                .getAbs("https://gitlab.com/api/v4/users/" + getUser().getUsername() + "/projects")
                .bearerTokenAuthentication(getUser().getToken())
                .send()
                .onFailure(err -> {
                    // network failure.
                    logger.error(new StringBuilder()
                            .append(getPlatform())
                            .append(" responses: ")
                            .append(err.getMessage())
                    );
                })
                .compose(response -> {
                    // network success, but the result is unknown.
                    JsonArray body = response.bodyAsJsonArray();
                    if (body == null) {
                        return Future.failedFuture(getPlatform() +
                                "responses with empty response body.");
                    }

                    String log = getPlatform() + " responses " + response.statusCode() +
                            " : " + body.toString();

                    if (response.statusCode() != 200) {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }
                    
                    logger.info(log);

                    for (int i = 0; i < body.size(); i++) {
                        JsonObject gitlabRepo = body.getJsonObject(i);
                        Repository repo = new Repository();
                        repo.setId(gitlabRepo.getInteger("id"));
                        repo.setName(gitlabRepo.getString("name"));
                        repo.setOwner(gitlabRepo.getJsonObject("owner").getString("username"));
                        repo.setVisibilityPrivate(gitlabRepo.getString("visibility").equals("private"));

                        if (includePrivate || !repo.getVisibilityPrivate()) {
                            repos.add(repo);
                        }
                    }

                    return Future.succeededFuture(repos);
                });
    }

    /**
     * Return the platform name.
     *
     * @return The platform name of GitLab.
     */
    @Override
    public String getPlatform() {
        return "GitLab";
    }
}
