import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpTaskServerSubtasksTest {
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
    void postAdd_returns201_and_404_on_missing_epic() throws Exception {
        // 404 если эпика нет
        Subtask bad = new Subtask("S", "d", 999);
        String badBody = gson.toJson(bad);
        var badPost = HttpRequest.newBuilder(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(badBody)).build();
        var r404 = client.send(badPost, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r404.statusCode());

        Epic e = new Epic("E", "d");
        manager.addEpic(e);
        Subtask s = new Subtask("S", "d", e.getId());
        s.setStartTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        s.setDuration(Duration.ofMinutes(30));
        String body = gson.toJson(s);

        var post = HttpRequest.newBuilder(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var resp = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp.statusCode());
        assertEquals(1, manager.getAllSubtasks().size());
    }

    @Test
    void getList_and_getById_returns200_or_404() throws Exception {
        Epic e = new Epic("E", "d");
        manager.addEpic(e);
        Subtask s = new Subtask("S", "d", e.getId());
        manager.addSubtask(s);

        var getAll = HttpRequest.newBuilder(URI.create("http://localhost:8080/subtasks"))
                .GET().build();
        var all = client.send(getAll, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, all.statusCode());

        var get404 = HttpRequest.newBuilder(URI.create("http://localhost:8080/subtasks?id=999"))
                .GET().build();
        var r404 = client.send(get404, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r404.statusCode());
    }

    @Test
    void deleteById_returns201_or_404() throws Exception {
        Epic e = new Epic("E", "d");
        manager.addEpic(e);
        Subtask s = new Subtask("S", "d", e.getId());
        manager.addSubtask(s);

        var del = HttpRequest.newBuilder(URI.create("http://localhost:8080/subtasks?id=" + s.getId()))
                .DELETE().build();
        var r = client.send(del, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, r.statusCode());
        assertTrue(manager.getAllSubtasks().isEmpty());

        var del404 = HttpRequest.newBuilder(URI.create("http://localhost:8080/subtasks?id=999"))
                .DELETE().build();
        var r404 = client.send(del404, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r404.statusCode());
    }

    @Test
    void postAdd_overlapping_returns406() throws Exception {
        Epic e = new Epic("E", "d");
        manager.addEpic(e);

        Subtask a = new Subtask("A", "d", e.getId());
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(60));
        manager.addSubtask(a);

        Subtask b = new Subtask("B", "d", e.getId());
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 30));
        b.setDuration(Duration.ofMinutes(30));
        String body = gson.toJson(b);

        var post = HttpRequest.newBuilder(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var resp = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(406, resp.statusCode());
    }
}