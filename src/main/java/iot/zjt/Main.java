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

            GitHubPlatform github = new GitHubPlatform(vertx, githubUser);
            GitLabPlatform gitlab = new GitLabPlatform(vertx, gitlabUser);

            github.mirrorAllRepoTo(gitlab, true, false)
                    .onComplete(ar -> {
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
