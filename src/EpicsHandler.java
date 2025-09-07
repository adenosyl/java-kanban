import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class EpicsHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public EpicsHandler(TaskManager manager, Gson gson) {
        super(gson);
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            String method = h.getRequestMethod();
            Integer id = parseId(h.getRequestURI().getQuery());

            switch (method) {
                case "GET" -> {
                    if (id == null) sendOk(h, manager.getAllEpics());
                    else {
                        Epic e = manager.getEpicById(id);
                        if (e == null) sendNotFound(h, "Эпик не найден: №" + id);
                        else sendOk(h, e);
                    }
                }
                case "POST" -> {
                    String body = readBody(h);
                    Epic e = gson.fromJson(body, Epic.class);
                    if (e == null) {
                        sendError(h, "Неправильный JSON");
                        return;
                    }
                    if (e.getId() == 0) {
                        try {
                            manager.addEpic(e);
                            sendCreated(h);
                        } catch (IllegalArgumentException iae) {
                            sendNotFound(h, iae.getMessage());
                        }
                    } else {
                        try {
                            manager.updateEpic(e);
                            sendCreated(h);
                        } catch (IllegalArgumentException iae) {
                            sendNotFound(h, iae.getMessage());
                        }
                    }
                }
                case "DELETE" -> {
                    if (id == null) {
                        sendError(h, "Для удаления Эпика необходим №");
                        return;
                    }
                    Epic e = manager.getEpicById(id);
                    if (e == null) {
                        sendNotFound(h, "Эпик не найден: №" + id);
                        return;
                    }
                    manager.deleteEpic(id);
                    sendCreated(h);
                }
                default -> sendError(h, "Неподдерживаемый метод: " + method);
            }
        } catch (Exception ex) {
            sendError(h, "Внутренняя ошибка: " + ex.getMessage());
        }
    }
}