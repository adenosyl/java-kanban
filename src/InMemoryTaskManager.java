import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    private int id = 1;
    private final Map<Integer, Task> tasks = new java.util.HashMap<>();
    private final Map<Integer, Epic> epics = new java.util.HashMap<>();
    private final Map<Integer, Subtask> subtasks = new java.util.HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();
    private final NavigableSet<Task> prioritized = new TreeSet<>(
            Comparator.<Task, LocalDateTime>comparing(
                    t -> t.getStartTime().orElse(null),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).thenComparingInt(t -> t.id)
    );

    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritized);
    }

    @Override
    public void addTask(Task task) {
        int finalId = ensureId(task);
        if (tasks.containsKey(finalId) || epics.containsKey(finalId) || subtasks.containsKey(finalId)) {
            throw new IllegalArgumentException("Id уже используется: " + finalId);
        }
        if (task.getStartTime().isPresent() && task.getDuration().isPresent()) {
            ensureNoOverlap(task);
        }
        tasks.put(finalId, task);
        indexForPriority(task);
    }

    @Override
    public void addEpic(Epic epic) {
        int finalId = ensureId(epic);
        if (subtasks.containsKey(finalId) || tasks.containsKey(finalId) || epics.containsKey(finalId)) {
            throw new IllegalArgumentException("Id конфликтует с существующей задачей/подзадачей: " + finalId);
        }
        epics.put(finalId, epic);
        epic.recalcTimes(subtasks);
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
        if (subtask.getStartTime().isPresent() && subtask.getDuration().isPresent()) {
            ensureNoOverlap(subtask);
        }
        subtasks.put(finalId, subtask);
        epic.addSubtask(finalId);
        updateEpicStatus(epic);
        indexForPriority(subtask);
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
            deindexForPriority(tasks.get(task.getId()));
            if (task.getStartTime().isPresent() && task.getDuration().isPresent()) {
                ensureNoOverlap(task);
            }
            tasks.put(task.getId(), task);
            indexForPriority(task);
        }
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic != null && epics.containsKey(epic.getId())) {
            Epic existingEpic = epics.get(epic.getId());
            existingEpic.setName(epic.getName());
            existingEpic.setDescription(epic.getDescription());
            existingEpic.setStatus(epic.getStatus());
            existingEpic.recalcTimes(subtasks);
            updateEpicStatus(existingEpic);
        }
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask != null && subtasks.containsKey(subtask.getId())) {
            deindexForPriority(subtasks.get(subtask.getId()));
            if (subtask.getStartTime().isPresent() && subtask.getDuration().isPresent()) {
                ensureNoOverlap(subtask);
            }
            subtasks.put(subtask.getId(), subtask);
            indexForPriority(subtask);

            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                updateEpicStatus(epic);
            }
        }
    }

    @Override
    public void deleteTask(int id) {
        Task old = tasks.remove(id);
        if (old != null) {
            deindexForPriority(old);
            historyManager.remove(id);
        }
    }

    @Override
    public void deleteEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            historyManager.remove(id);
            for (int subtaskId : new java.util.ArrayList<>(epic.getSubtaskIds())) {
                Subtask removed = subtasks.remove(subtaskId);
                if (removed != null) deindexForPriority(removed);
                historyManager.remove(subtaskId);
            }
        }
    }

    @Override
    public void deleteSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            deindexForPriority(subtask);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtask(id);
                updateEpicStatus(epic);
            }
            historyManager.remove(id);
        }
    }

    private void indexForPriority(Task t) {
        t.getStartTime().ifPresent(st -> {
            prioritized.remove(t);
            prioritized.add(t);
        });
    }

    private void deindexForPriority(Task t) {
        prioritized.remove(t);
    }

    private boolean overlaps(Task a, Task b) {
        var aS = a.getStartTime();
        var aE = a.getEndTime();
        var bS = b.getStartTime();
        var bE = b.getEndTime();
        if (aS.isEmpty() || aE.isEmpty() || bS.isEmpty() || bE.isEmpty()) return false;
        return aS.get().isBefore(bE.get()) && bS.get().isBefore(aE.get());
    }

    private void ensureNoOverlap(Task candidate) {
        boolean has = prioritized.stream()
                .filter(t -> t.id != candidate.id)
                .anyMatch(t -> overlaps(t, candidate));
        if (has) throw new IllegalStateException("Интервал задачи пересекается с существующей задачей");
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
            epic.recalcTimes(subtasks);
            return;
        }

        var statuses = subtaskIds.stream()
                .map(subtasks::get)
                .filter(java.util.Objects::nonNull)
                .map(Subtask::getStatus)
                .toList();

        if (statuses.stream().anyMatch(s -> s == TaskStatus.IN_PROGRESS)) {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        } else {
            boolean hasNew = statuses.stream().anyMatch(s -> s == TaskStatus.NEW);
            boolean hasDone = statuses.stream().anyMatch(s -> s == TaskStatus.DONE);

            if (hasDone && !hasNew) {
                epic.setStatus(TaskStatus.DONE);
            } else if (hasNew && !hasDone) {
                epic.setStatus(TaskStatus.NEW);
            } else {
                epic.setStatus(TaskStatus.IN_PROGRESS);
            }
        }
        epic.recalcTimes(subtasks);
    }
}