import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {

    private final List<Task> history = new java.util.ArrayList<>();

    @Override
    public void add(Task task) {
        if (history.size() >= 10) {
            history.remove(0);
        }
        history.add(task);
    }

    @Override
    public List<Task> getHistory() {
        return new java.util.ArrayList<>(history);
    }
}