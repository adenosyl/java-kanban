import java.util.ArrayList;

public class Epic extends Task {
    private final ArrayList<Integer> subtaskIds = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public void addSubtask(int id) {
        subtaskIds.add(id);
    }

    public ArrayList<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    @Override
    public String toString() {
        return "Эпик № " + getId() + ": " + name + " [" + status + "] - " + description + ". Подзадачи: " + subtaskIds;
    }
}
