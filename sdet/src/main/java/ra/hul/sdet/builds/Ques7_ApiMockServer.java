package ra.hul.sdet.builds;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * API Mock Server - A tiny local mock HTTP server with route->response mappings and request logging.
 * Common SDET question (machine-coding round): "Build a lightweight mock server: register responses for
 * (method + path); return configured status/body; log received requests for later verification."
 *
 * Fully local, NO NETWORK: starts com.sun.net.httpserver.HttpServer on an ephemeral port, hits it with
 * java.net.http.HttpClient, asserts responses and the request log, then stops the server.
 */
public class Ques7_ApiMockServer {

    /** A configured mock response. */
    public record MockResponse(int status, String body, Map<String, String> headers) {
        public static MockResponse json(int status, String body) {
            return new MockResponse(status, body, Map.of("Content-Type", "application/json"));
        }
    }

    /** A logged inbound request (for test verification). */
    public record RecordedRequest(String method, String path) {}

    /** The mock server: register routes as "METHOD /path", start on port 0 (ephemeral), inspect the log. */
    public static final class MockServer {
        private final HttpServer server;
        private final Map<String, MockResponse> routes = new ConcurrentHashMap<>();
        private final List<RecordedRequest> received = new CopyOnWriteArrayList<>();

        public MockServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
        }

        public MockServer on(String method, String path, MockResponse response) {
            routes.put(key(method, path), response);
            return this;
        }

        public void start() { server.start(); }

        public void stop() { server.stop(0); }

        public int port() { return server.getAddress().getPort(); }

        public String baseUrl() { return "http://127.0.0.1:" + port(); }

        public List<RecordedRequest> log() { return received; }

        private void handle(HttpExchange ex) throws IOException {
            received.add(new RecordedRequest(ex.getRequestMethod(), ex.getRequestURI().getPath()));
            MockResponse mock = routes.get(key(ex.getRequestMethod(), ex.getRequestURI().getPath()));
            if (mock == null) {
                byte[] body = "{\"error\":\"no route\"}".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(404, body.length);
                ex.getResponseBody().write(body);
                ex.close();
                return;
            }
            mock.headers().forEach((k, v) -> ex.getResponseHeaders().add(k, v));
            byte[] body = mock.body().getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(mock.status(), body.length);
            ex.getResponseBody().write(body);
            ex.close();
        }

        private static String key(String method, String path) {
            return method.toUpperCase() + " " + path;
        }
    }

    static void main() throws IOException, InterruptedException {
        MockServer mock = new MockServer();
        mock.on("GET", "/api/users/1", MockResponse.json(200, "{\"id\":1,\"name\":\"Alice\"}"))
            .on("POST", "/api/users", MockResponse.json(201, "{\"id\":2,\"name\":\"Bob\"}"));
        mock.start();
        System.out.println("=== API Mock Server ===");
        System.out.println("Started on " + mock.baseUrl());

        try (HttpClient client = HttpClient.newHttpClient()) {
            // 1. GET a mapped route
            HttpResponse<String> get = client.send(
                    HttpRequest.newBuilder(URI.create(mock.baseUrl() + "/api/users/1")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            // 2. POST a mapped route
            HttpResponse<String> post = client.send(
                    HttpRequest.newBuilder(URI.create(mock.baseUrl() + "/api/users"))
                            .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Bob\"}")).build(),
                    HttpResponse.BodyHandlers.ofString());

            // 3. Hit an unmapped route -> 404 from the mock
            HttpResponse<String> miss = client.send(
                    HttpRequest.newBuilder(URI.create(mock.baseUrl() + "/nope")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("GET  /api/users/1 -> " + get.statusCode() + " " + get.body());
            System.out.println("POST /api/users   -> " + post.statusCode() + " " + post.body());
            System.out.println("GET  /nope        -> " + miss.statusCode() + " " + miss.body());
            System.out.println("Request log: " + mock.log());

            boolean ok = get.statusCode() == 200 && get.body().contains("Alice")
                    && get.headers().firstValue("Content-Type").orElse("").contains("application/json")
                    && post.statusCode() == 201 && post.body().contains("Bob")
                    && miss.statusCode() == 404
                    && mock.log().size() == 3
                    && mock.log().contains(new RecordedRequest("POST", "/api/users"));
            System.out.println(ok
                    ? "PASSED: routes matched by method+path, responses correct, requests logged."
                    : "FAILED: mock server behavior mismatch.");
        } finally {
            mock.stop();
            System.out.println("Server stopped.");
        }
    }
}
