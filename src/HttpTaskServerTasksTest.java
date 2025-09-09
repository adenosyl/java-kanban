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

public class HttpTaskServerTasksTest {
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
    void postAdd_and_listGet_returns201_and200() throws Exception {
        Task t = new Task("T", "d");
        t.setStatus(TaskStatus.NEW);
        t.setStartTime(LocalDateTime.now());
        t.setDuration(Duration.ofMinutes(15));
        String body = gson.toJson(t);

        var post = HttpRequest.newBuilder(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var resp = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp.statusCode());

        var get = HttpRequest.newBuilder(URI.create("http://localhost:8080/tasks"))
                .GET().build();
        var list = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, list.statusCode());

        assertEquals(1, manager.getAllTasks().size());
        assertEquals("T", manager.getAllTasks().get(0).getName());
    }

    @Test
    void postUpdate_returns201_and_changes_state() throws Exception {
        Task t = new Task("A", "d");
        manager.addTask(t);
        t.setName("A2");
        String body = gson.toJson(t);

        var post = HttpRequest.newBuilder(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var resp = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp.statusCode());
        assertEquals("A2", manager.getTaskById(t.getId()).getName());
    }

    @Test
    void deleteById_returns201_or_404() throws Exception {
        Task t = new Task("X", "d");
        manager.addTask(t);

        var del404 = HttpRequest.newBuilder(URI.create("http://localhost:8080/tasks?id=999"))
                .DELETE().build();
        var r404 = client.send(del404, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, r404.statusCode());

        var del = HttpRequest.newBuilder(URI.create("http://localhost:8080/tasks?id=" + t.getId()))
                .DELETE().build();
        var r = client.send(del, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, r.statusCode());
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    void postAdd_overlapping_returns406() throws Exception {
        Task a = new Task("A", "d");
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(60));
        manager.addTask(a);

        Task b = new Task("B", "d");
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 30)); // пересечение
        b.setDuration(Duration.ofMinutes(30));
        String body = gson.toJson(b);

        var post = HttpRequest.newBuilder(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var resp = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(406, resp.statusCode());
    }
}