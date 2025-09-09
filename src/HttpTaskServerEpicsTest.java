import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpTaskServerEpicsTest {
    private TaskManager manager;
    private HttpTaskServer server;
    private HttpClient client;
    private com.google.gson.Gson gson;

    @BeforeEach
    void setUp() throws Exception {
        manager = new InMemoryTaskManager();
        server = new HttpTaskServer(manager);
        client = HttpClient.newHttpClient();
        gson = HttpTaskServer.getGson();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void postAdd_and_getList_returns201_and_200() throws Exception {
        Epic e = new Epic("E", "d");
        String body = gson.toJson(e);
        var post = HttpRequest.newBuilder(URI.create("http://localhost:8080/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var r = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, r.statusCode());

        var get = HttpRequest.newBuilder(URI.create("http://localhost:8080/epics"))
                .GET().build();
        var g = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, g.statusCode());
        assertEquals(1, manager.getAllEpics().size());
    }

    @Test
    void getById_or_404() throws Exception {
        var r404 = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:8080/epics?id=999"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r404.statusCode());

        Epic e = new Epic("E", "d");
        manager.addEpic(e);
        var r200 = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:8080/epics?id=" + e.getId()))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, r200.statusCode());
    }

    @Test
    void deleteById_returns201_or_404() throws Exception {
        Epic e = new Epic("E", "d");
        manager.addEpic(e);

        var del = HttpRequest.newBuilder(URI.create("http://localhost:8080/epics?id=" + e.getId()))
                .DELETE().build();
        var r = client.send(del, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, r.statusCode());
        assertTrue(manager.getAllEpics().isEmpty());

        var del404 = HttpRequest.newBuilder(URI.create("http://localhost:8080/epics?id=999"))
                .DELETE().build();
        var r404 = client.send(del404, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r404.statusCode());
    }
}