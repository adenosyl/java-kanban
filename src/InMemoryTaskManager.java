import java.util.List;
import java.util.Map;

public class InMemoryTaskManager implements TaskManager {
    private int id = 1;
    private final Map<Integer, Task> tasks = new java.util.HashMap<>();
    private final Map<Integer, Epic> epics = new java.util.HashMap<>();
    private final Map<Integer, Subtask> subtasks = new java.util.HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();

    @Override
    public void addTask(Task task) {
        int finalId = ensureId(task);
        if (tasks.containsKey(finalId) || epics.containsKey(finalId) || subtasks.containsKey(finalId)) {
            throw new IllegalArgumentException("Id уже используется: " + finalId);
        }
        tasks.put(finalId, task);
    }

    @Override
    public void addEpic(Epic epic) {
        int finalId = ensureId(epic);
        if (subtasks.containsKey(finalId) || tasks.containsKey(finalId) || epics.containsKey(finalId)) {
            throw new IllegalArgumentException("Id конфликтует с существующей задачей/подзадачей: " + finalId);
        }
        epics.put(finalId, epic);
    }

    @Override
    public void addSubtask(Subtask subtask) {
        if (subtask.getId() != 0 && subtask.getId() == subtask.getEpicId()) {
            throw new IllegalArgumentException("Эпик не может быть подзадачей самого себя.");
        }
        int finalId = ensureId(subtask);
        if (tasks.containsKey(finalId) || epics.containsKey(finalId) || subtasks.containsKey(finalId)) {
            throw new IllegalArgumentException("Id уже используется: " + finalId);
        }
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) {
            throw new IllegalArgumentException("Эпик не найден: " + subtask.getEpicId());
        }
        subtasks.put(finalId, subtask);
        epic.addSubtask(finalId);
        updateEpicStatus(epic);
    }

    @Override
    public List<Task> getAllTasks() {
        return new java.util.ArrayList<>(tasks.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new java.util.ArrayList<>(epics.values());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new java.util.ArrayList<>(subtasks.values());
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            addToHistory(task);
        }
        return task;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            addToHistory(epic);
        }
        return epic;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            addToHistory(subtask);
        }
        return subtask;
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public void updateTask(Task task) {
        if (task != null && tasks.containsKey(task.getId())) {
            tasks.put(task.getId(), task);
        }
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic != null && epics.containsKey(epic.getId())) {
            Epic existingEpic = epics.get(epic.getId());
            existingEpic.setName(epic.getName());
            existingEpic.setDescription(epic.getDescription());
            existingEpic.setStatus(epic.getStatus());
            updateEpicStatus(existingEpic);
        }
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask != null && subtasks.containsKey(subtask.getId())) {
            subtasks.put(subtask.getId(), subtask);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                updateEpicStatus(epic);
            }
        }
    }

    @Override
    public void deleteTask(int id) {
        tasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void deleteEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            historyManager.remove(id);
            for (int subtaskId : new java.util.ArrayList<>(epic.getSubtaskIds())) {
                subtasks.remove(subtaskId);
                historyManager.remove(subtaskId);
            }
        }
    }

    @Override
    public void deleteSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtask(id);
                updateEpicStatus(epic);
            }
            historyManager.remove(id);
        }
    }

    private int ensureId(Task t) {
        if (t.getId() == 0) {
            t.setId(generateId());
        }
        if (t.getId() >= id) {
            id = t.getId() + 1;
        }
        return t.getId();
    }

    private int generateId() {
        return id++;
    }

    private void addToHistory(Task task) {
        historyManager.add(task);
    }

    private void updateEpicStatus(Epic epic) {
        List<Integer> subtaskIds = epic.getSubtaskIds();
        if (subtaskIds.isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
            return;
        }

        boolean hasNew = false;
        boolean hasDone = false;

        for (int subtaskId : subtaskIds) {
            Subtask subtask = subtasks.get(subtaskId);
            if (subtask != null) {
                TaskStatus status = subtask.getStatus();

                if (status == TaskStatus.IN_PROGRESS) {
                    epic.setStatus(TaskStatus.IN_PROGRESS);
                    return;
                }

                if (status == TaskStatus.NEW) {
                    hasNew = true;
                }

                if (status == TaskStatus.DONE) {
                    hasDone = true;
                }
            }
        }

        if (hasDone && !hasNew) {
            epic.setStatus(TaskStatus.DONE);
        } else if (hasNew && !hasDone) {
            epic.setStatus(TaskStatus.NEW);
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }
}
