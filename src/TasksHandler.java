import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class TasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public TasksHandler(TaskManager manager, Gson gson) {
        super(gson);
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            String method = h.getRequestMethod();
            String path = h.getRequestURI().getPath();
            String query = h.getRequestURI().getQuery();
            Integer id = parseId(query);

            switch (method) {
                case "GET" -> {
                    if (id == null) {
                        sendOk(h, manager.getAllTasks());
                    } else {
                        Task t = manager.getTaskById(id);
                        if (t == null) {
                            sendNotFound(h, "Задача не найдена: №" + id);
                        } else {
                            sendOk(h, t);
                        }
                    }
                }
                case "POST" -> {
                    String body = readBody(h);
                    Task t = gson.fromJson(body, Task.class);
                    if (t == null) {
                        sendError(h, "Неправильный JSON");
                        return;
                    }
                    if (t.getId() == 0) {
                        try {
                            manager.addTask(t);
                            sendCreated(h);
                        } catch (IllegalStateException overlap) {
                            sendHasOverlaps(h, overlap.getMessage());
                        } catch (IllegalArgumentException iae) {
                            sendError(h, iae.getMessage());
                        }
                    } else {
                        try {
                            manager.updateTask(t);
                            sendCreated(h);
                        } catch (IllegalStateException overlap) {
                            sendHasOverlaps(h, overlap.getMessage());
                        } catch (IllegalArgumentException iae) {
                            sendNotFound(h, iae.getMessage());
                        }
                    }
                }
                case "DELETE" -> {
                    if (id == null) {
                        sendError(h, "Для удаления Задачи необходим №");
                        return;
                    }
                    Task existing = manager.getTaskById(id);
                    if (existing == null) {
                        sendNotFound(h, "Задача не найдена: №" + id);
                        return;
                    }
                    manager.deleteTask(id);
                    sendCreated(h);
                }
                default -> sendError(h, "Неподдерживаемый метод: " + method);
            }
        } catch (Exception ex) {
            sendError(h, "Внутренняя ошибка: " + ex.getMessage());
        }
    }
}