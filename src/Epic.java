import java.util.List;

public class Epic extends Task {
    private final List<Integer> subtaskIds = new java.util.ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public void addSubtask(int id) {
        subtaskIds.add(id);
    }

    public void removeSubtask(int id) {
        subtaskIds.remove((Integer) id);
    }

    public List<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    @Override
    public String toString() {
        return "Эпик № " + getId() + ": " + name + " [" + status + "] - " + description + ". Подзадачи: " + subtaskIds;
    }
}
