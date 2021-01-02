package iot.zjt.platform.online;

import io.vertx.core.CompositeFuture;
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
 * Platform operation of GitHub.
 *
 * @author Mr Dk.
 * @since 2021/01/01
 */
public class GitHubPlatform extends AbstractOnlinePlatform {

    private final static Logger logger = LogManager.getLogger(GitHubPlatform.class);

    /**
     * Since GitHub API need cannot show all repositories because of pagination,
     * the user need to provide approximate number of repository numbers so that
     * we can calculate how many pages should be requested.
     * A bigger number should be provided to make sure all of the repositories
     * are fetched.
     */
    private final int approximateRepoCount;

    public GitHubPlatform(final Vertx vertx, final PlatformUser user, final int approximateRepoCount) {
        super(vertx, user);
        this.approximateRepoCount = approximateRepoCount;
    }

    /**
     * To create a GitHub repository through GitHub API v3.
     * The end point is "https://api.github.com/user/repos" with POST method,
     * 201 should be returned.
     *
     * @param repo The repository to be created.
     * @return The future object of result.
     */
    @Override
    public Future<Void> createRepository(Repository repo) {
        logger.warn("Trying to create repository [" + repo.getName() + "] on " + getPlatform());
        WebClient client = WebClient.create(getVertx());

        return client
                .postAbs("https://api.github.com/user/repos")
                .bearerTokenAuthentication(getUser().getToken())
                .putHeader("accept", "application/vnd.github.v3+json")
                .sendJsonObject(new JsonObject()
                        .put("name", repo.getName())
                        .put("private", repo.getVisibilityPrivate())
                )
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
                    if (body == null) {
                        return Future.failedFuture(getPlatform() +
                                "responses with empty response body.");
                    }

                    String log = getPlatform() + " responses " + response.statusCode();

                    if (response.statusCode() == 201) {
                        logger.info(log);
                        repo.setId(body.getInteger("id"));
                    } else {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }

                    logger.info("Successfully create " + repo.getName() + " on " + getPlatform());
                    return Future.succeededFuture();
                });
    }

    /**
     * To delete a GitHub repository through GitHub API v3.
     * The end point is "https://api.github.com/repos/{owner}/{repo}"
     * with DELETE method, 204 should be returned.
     *
     * @param repo The repository to be deleted.
     * @return The future object of result.
     */
    @Override
    public Future<Void> deleteRepository(Repository repo) {
        logger.warn("Trying to delete repository [" + repo.getName() + "] on " + getPlatform());

        WebClient client = WebClient.create(getVertx());

        return client
                .deleteAbs("https://api.github.com/repos/" + repo.getOwner() + "/" + repo.getName())
                .bearerTokenAuthentication(getUser().getToken())
                .putHeader("accept", "application/vnd.github.v3+json")
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
                    String log = getPlatform() + " responses " + response.statusCode();

                    if (response.statusCode() == 204) {
                        logger.info(log);
                    } else {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }

                    logger.info("Successfully delete " + repo.getName() + " on " + getPlatform());
                    return Future.succeededFuture();
                });
    }

    @Override
    public Future<Void> updateRepository(Repository repo) {
        logger.warn("Trying to update repository [" + repo.getName() + "] on " + getPlatform());

        WebClient client = WebClient.create(getVertx());

        return client
                .patchAbs("https://api.github.com/repos/" + repo.getOwner() + "/" + repo.getName())
                .bearerTokenAuthentication(getUser().getToken())
                .putHeader("accept", "application/vnd.github.v3+json")
                .sendJsonObject(new JsonObject()
                        .put("private", repo.getVisibilityPrivate())
                )
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
                    if (body == null) {
                        return Future.failedFuture(getPlatform() +
                                "responses with empty response body.");
                    }

                    String log = getPlatform() + " responses " + response.statusCode();

                    if (response.statusCode() == 200) {
                        logger.info(log);
                    } else {
                        logger.error(log);
                        return Future.failedFuture(log);
                    }

                    logger.info("Successfully update " + repo.getName() + " on " + getPlatform());
                    return Future.succeededFuture();
                });
    }

    /**
     * To get all GitHub repositories of a user through GitHub API v3.
     * The end point is "https://api.github.com/user/repos" with GET method.
     * The identity of user is implied in token. 200 should be returned.
     *
     * @param includePrivate Whether or not to get private repositories.
     * @return The future object containing all repository information.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Future<List<Repository>> getRepositories(boolean includePrivate) {
        logger.warn("Trying to get repositories from " + getPlatform());

        WebClient client = WebClient.create(getVertx());
        List<Repository> repos = new ArrayList<>();
        List<Future> futures = new ArrayList<>();

        for (int page = 1; page <= Math.ceil(this.approximateRepoCount / 30d); page++) {
            logger.warn("Fetching the " + page + "(-th) page from " + getPlatform());
            Future<Void> future = client
                    .getAbs("https://api.github.com/user/repos")
                    .bearerTokenAuthentication(getUser().getToken())
                    .putHeader("accept", "application/vnd.github.v3+json")
                    .addQueryParam("type", "owner")
                    .addQueryParam("page", Integer.toString(page))
                    .addQueryParam("per_page", "30") // GitHub default
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

                        String log = getPlatform() + " responses " + response.statusCode();

                        if (response.statusCode() != 200) {
                            logger.error(log);
                            return Future.failedFuture(log);
                        }

                        logger.info(log);

                        for (int i = 0; i < body.size(); i++) {
                            JsonObject githubRepo = body.getJsonObject(i);
                            Repository repo = new Repository();
                            repo.setId(githubRepo.getInteger("id"));
                            repo.setOwner(githubRepo.getJsonObject("owner").getString("login"));
                            repo.setName(githubRepo.getString("name"));
                            repo.setVisibilityPrivate(githubRepo.getBoolean("private"));

                            if (includePrivate || !repo.getVisibilityPrivate()) {
                                repos.add(repo);
                            }
                        }

                        return Future.succeededFuture();
                    });
            futures.add(future);
        }

        return CompositeFuture.all(futures).onFailure(err -> {
            logger.error(err.getMessage());
        }).compose(compositeFuture -> {
            logger.info("Successfully get all repositories from " + getPlatform());
            return Future.succeededFuture(repos);
        });
    }

    /**
     * Return the platform name.
     *
     * @return The platform name of GitHub.
     */
    @Override
    public String getPlatform() {
        return "GitHub";
    }

    /**
     * Return the HTTPS URL for Git remote operation.
     *
     * @param repo The repository on the platform.
     * @return The HTTPS URL of the repository on this platform.
     */
    @Override
    public String getRepositoryHttpsUrl(Repository repo) {
        return "https://github.com/" + repo.getOwner() + "/" + repo.getName() + ".git";
    }
}
