import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {

    private static final String HEADER = "id,type,name,status,description,epic,start,durationMinutes";
    private final File file;
    private boolean suppressSave = false;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    @Override
    public void addTask(Task task) {
        super.addTask(task);
        save();
    }

    @Override
    public void addEpic(Epic epic) {
        super.addEpic(epic);
        save();
    }

    @Override
    public void addSubtask(Subtask subtask) {
        super.addSubtask(subtask);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
        save();
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save();
    }

    @Override
    public void deleteSubtask(int id) {
        super.deleteSubtask(id);
        save();
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        if (!file.exists()) {
            return manager;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return manager;
            }

            manager.suppressSave = true;

            List<Task> tasks = new ArrayList<>();
            List<Epic> epics = new ArrayList<>();
            List<Subtask> subtasks = new ArrayList<>();

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] p = line.split(",", -1);
                int id = Integer.parseInt(p[0]);
                String type = p[1];
                String name = p[2];
                TaskStatus status = TaskStatus.valueOf(p[3]);
                String description = p[4];
                String epicStr = (p.length > 5) ? p[5] : "";
                String startStr = (p.length > 6) ? p[6] : "";
                String durStr = (p.length > 7) ? p[7] : "";

                LocalDateTime start = (startStr == null || startStr.isEmpty()) ? null : LocalDateTime.parse(startStr);
                Duration duration = (durStr == null || durStr.isEmpty()) ? null : Duration.ofMinutes(Long.parseLong(durStr));

                if ("TASK".equals(type)) {
                    Task t = new Task(name, description);
                    t.setId(id);
                    t.setStatus(status);
                    t.setStartTime(start);
                    t.setDuration(duration);
                    tasks.add(t);
                } else if ("EPIC".equals(type)) {
                    Epic e = new Epic(name, description);
                    e.setId(id);
                    e.setStatus(status);
                    epics.add(e);
                } else if ("SUBTASK".equals(type)) {
                    int epicId = epicStr.isEmpty() ? 0 : Integer.parseInt(epicStr);
                    Subtask s = new Subtask(name, description, epicId);
                    s.setId(id);
                    s.setStatus(status);
                    s.setStartTime(start);
                    s.setDuration(duration);
                    subtasks.add(s);
                }
            }

            for (Task t : tasks) {
                manager.addTask(t);
            }
            for (Epic e : epics) {
                manager.addEpic(e);
            }
            for (Subtask s : subtasks) {
                manager.addSubtask(s);
            }

            return manager;
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка загрузки из файла: " + file, e);
        } finally {
            manager.suppressSave = false;
            manager.save();
        }
    }

    protected void save() {
        if (suppressSave) return;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append(HEADER).append('\n');
            for (Task t : getAllTasks()) {
                sb.append(toCsv(t)).append('\n');
            }
            for (Epic e : getAllEpics()) {
                sb.append(toCsv(e)).append('\n');
            }
            for (Subtask s : getAllSubtasks()) {
                sb.append(toCsv(s)).append('\n');
            }

            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка сохранения в файл: " + file, e);
        }
    }

    private static String esc(String s) {
        return (s == null) ? "" : s.replace("\n", " ");
    }

    private static String toCsv(Task t) {
        String type;
        String epicId = "";
        String start = "";
        String durationMin = "";

        if (t instanceof Epic) {
            type = "EPIC";
            // у эпика время расчётное — не пишем
        } else if (t instanceof Subtask s) {
            type = "SUBTASK";
            epicId = String.valueOf(s.getEpicId());
            start = s.getStartTime().map(LocalDateTime::toString).orElse("");
            durationMin = s.getDuration().map(d -> Long.toString(d.toMinutes())).orElse("");
        } else {
            type = "TASK";
            start = t.getStartTime().map(LocalDateTime::toString).orElse("");
            durationMin = t.getDuration().map(d -> Long.toString(d.toMinutes())).orElse("");
        }

        return String.join(",",
                String.valueOf(t.getId()),
                type,
                esc(t.getName()),
                t.getStatus().name(),
                esc(t.getDescription()),
                epicId,
                start,
                durationMin
        );
    }
}