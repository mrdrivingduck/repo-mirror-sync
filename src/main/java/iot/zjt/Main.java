package iot.zjt;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
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
        } catch (IOException e) {
            e.printStackTrace();
        }

//        WebClient client = WebClient.create(vertx);

        PlatformUser user = new PlatformUser("mrdrivingduck", "123");

        GitHubPlatform github = new GitHubPlatform(vertx, user);
        GitLabPlatform gitlab = new GitLabPlatform(vertx, user);

        github.mirrorAllRepoTo(gitlab, true, false)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        System.out.println("Mirroring success");
                    } else {
                        System.err.println("Mirroring failed");
                    }
                });

// Send a GET request
//        client
//            .get(8080, "myserver.mycompany.com", "/some-uri")
//            .send()
//            .onSuccess(response -> System.out
//                .println("Received response with status code" + response.statusCode()))
//            .onFailure(err ->
//                System.out.println("Something went wrong " + err.getMessage()));

    }
}
