import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class HistoryHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public HistoryHandler(TaskManager manager, Gson gson) {
        super(gson);
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            if (!"GET".equals(h.getRequestMethod())) {
                sendError(h, "Метод неподдерживается: " + h.getRequestMethod());
                return;
            }
            sendOk(h, manager.getHistory());
        } catch (Exception ex) {
            sendError(h, "Внутренняя ошибка: " + ex.getMessage());
        }
    }
}