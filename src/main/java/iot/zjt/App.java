package iot.zjt;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        WebClient client = WebClient.create(vertx);

// Send a GET request
        client
            .get(8080, "myserver.mycompany.com", "/some-uri")
            .send()
            .onSuccess(response -> System.out
                .println("Received response with status code" + response.statusCode()))
            .onFailure(err ->
                System.out.println("Something went wrong " + err.getMessage()));

    }
}
