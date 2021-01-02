package iot.zjt;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iot.zjt.platform.PlatformUser;
import iot.zjt.platform.online.GitHubPlatform;
import iot.zjt.platform.online.GitLabPlatform;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;

/**
 * Hello world!
 */
public class Main {

    private final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        try {
            Logger.init();
            logger.info("Logger init success.");
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(
                new ConfigStoreOptions().setType("file").setFormat("properties")
                        .setConfig(new JsonObject().put("path", "application-dev.properties"))
        );
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, options);

        configRetriever.getConfig(json -> {
            logger.info("Successfully load user configurations.");

            PlatformUser githubUser = new PlatformUser(
                    json.result().getString("github.username"),
                    json.result().getString("github.token"));
            PlatformUser gitlabUser = new PlatformUser(
                    json.result().getString("gitlab.username"),
                    json.result().getString("gitlab.token")
            );

            GitHubPlatform github = new GitHubPlatform(vertx, githubUser, 100);
            GitLabPlatform gitlab = new GitLabPlatform(vertx, gitlabUser);

//            Repository repo = new Repository();
//            repo.setVisibilityPrivate(false);
//            repo.setOwner(gitlabUser.getUsername());
//            repo.setName("repo-mirror-sync");
//
//            gitlab.updateRepository(repo).onComplete(ar -> {
//                if (ar.succeeded()) {
//                    logger.info("ojbk");
//                } else {
//                    logger.error("fail to update");
//                }
//
//                vertx.close();
//            });

//
//            gitlab.createRepository(repo).onComplete(ar -> {
//                if (ar.succeeded()) {
//                    logger.info("objk");
//                } else {
//                    logger.error("end");
//                }
//                vertx.close();
//            });

//            gitlab.deleteRepository(repo).onComplete(ar -> {
//                if (ar.succeeded()) {
//                    logger.info("objk");
//                } else {
//                    logger.error("end");
//                }
//                vertx.close();
//            });

//            gitlab.getRepositories(true).onComplete(ar -> {
//                if (ar.succeeded()) {
//                    for (Repository onlineRepo : ar.result()) {
//                        logger.info(onlineRepo.getName());
//                    }
//                    logger.info(ar.result().size());
//                } else {
//                    logger.error(ar.cause().getMessage());
//                }
//
//                vertx.close();
//            });

            github.mirrorAllRepoTo(gitlab, true, false).onComplete(ar -> {
                if (ar.succeeded()) {
                    System.out.println("Mirroring success");
                } else {
                    System.err.println("Mirroring failed");
                }

                vertx.close();
            });
        });

    }
}
