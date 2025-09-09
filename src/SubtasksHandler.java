import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class SubtasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public SubtasksHandler(TaskManager manager, Gson gson) {
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
                    if (id == null) {
                        sendOk(h, manager.getAllSubtasks());
                    } else {
                        Subtask s = manager.getSubtaskById(id);
                        if (s == null) sendNotFound(h, "Подзадача не найдена: №" + id);
                        else sendOk(h, s);
                    }
                }
                case "POST" -> {
                    String body = readBody(h);
                    Subtask s = gson.fromJson(body, Subtask.class);
                    if (s == null) {
                        sendError(h, "Неправильный JSON");
                        return;
                    }
                    if (s.getId() == 0) {
                        try {
                            manager.addSubtask(s);
                            sendCreated(h);
                        } catch (IllegalArgumentException notFoundEpic) {
                            sendNotFound(h, notFoundEpic.getMessage());
                        } catch (IllegalStateException overlap) {
                            sendHasOverlaps(h, overlap.getMessage());
                        }
                    } else {
                        try {
                            manager.updateSubtask(s);
                            sendCreated(h);
                        } catch (IllegalArgumentException iae) {
                            sendNotFound(h, iae.getMessage());
                        } catch (IllegalStateException overlap) {
                            sendHasOverlaps(h, overlap.getMessage());
                        }
                    }
                }
                case "DELETE" -> {
                    if (id == null) {
                        sendError(h, "Для удаления Подзадачи необходим №");
                        return;
                    }
                    Subtask s = manager.getSubtaskById(id);
                    if (s == null) {
                        sendNotFound(h, "Подзадача не найдена: №" + id);
                        return;
                    }
                    manager.deleteSubtask(id);
                    sendCreated(h);
                }
                default -> sendError(h, "Неподдерживаемый метод: " + method);
            }
        } catch (Exception ex) {
            sendError(h, "Внутренняя ошибка: " + ex.getMessage());
        }
    }
}
