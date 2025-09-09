import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpTaskServerHistoryTest {
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
    void getHistory_returns200() throws Exception {
        Task a = new Task("A", "d");
        Task b = new Task("B", "d");
        manager.addTask(a);
        manager.addTask(b);
        manager.getTaskById(a.getId());
        manager.getTaskById(b.getId());

        var get = HttpRequest.newBuilder(URI.create("http://localhost:8080/history"))
                .GET().build();
        var resp = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertEquals(2, manager.getHistory().size());
    }
}