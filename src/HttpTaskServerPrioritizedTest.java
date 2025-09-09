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

public class HttpTaskServerPrioritizedTest {
    private TaskManager manager;
    private HttpTaskServer server;
    private HttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        manager = new InMemoryTaskManager();
        server = new HttpTaskServer(manager);
        client = HttpClient.newHttpClient();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void getPrioritized_returns200_and_sorted() throws Exception {
        Task a = new Task("A", "d");
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(60));
        Task b = new Task("B", "d");
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        b.setDuration(Duration.ofMinutes(30));
        manager.addTask(a);
        manager.addTask(b);

        var get = HttpRequest.newBuilder(URI.create("http://localhost:8080/prioritized"))
                .GET().build();
        var resp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        var p = manager.getPrioritizedTasks();
        assertEquals(2, p.size());
        assertEquals(b.getId(), p.get(0).getId());
        assertEquals(a.getId(), p.get(1).getId());
    }
}