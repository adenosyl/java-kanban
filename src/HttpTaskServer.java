import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpTaskServer {
    private final HttpServer server;
    private final TaskManager manager;
    private static final Gson GSON = GsonAdapters.create();

    public HttpTaskServer(TaskManager manager) throws IOException {
        this.manager = manager;
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", new TasksHandler(manager, GSON));
        server.createContext("/subtasks", new SubtasksHandler(manager, GSON));
        server.createContext("/epics", new EpicsHandler(manager, GSON));
        server.createContext("/history", new HistoryHandler(manager, GSON));
        server.createContext("/prioritized", new PrioritizedHandler(manager, GSON));
    }

    public void start() {
        server.start();
        System.out.println("HTTP server started on http://localhost:8080");
    }

    public void stop() {
        server.stop(0);
        System.out.println("HTTP server stopped");
    }

    public static Gson getGson() {
        return GSON;
    }

    public static void main(String[] args) throws Exception {
        TaskManager m = Managers.getDefault();
        HttpTaskServer s = new HttpTaskServer(m);
        s.start();
    }
}