import java.util.ArrayList;
import java.util.List;

public interface TaskManager {

    void addTask(Task task);
    void addEpic(Epic epic);
    void addSubtask(Subtask subtask);

    ArrayList<Task> getAllTasks();
    ArrayList<Epic> getAllEpics();
    ArrayList<Subtask> getAllSubtasks();

    Task getTaskById(int id);
    Epic getEpicById(int id);
    Subtask getSubtaskById(int id);

    void updateTask(Task task);
    void updateEpic(Epic epic);
    void updateSubtask(Subtask subtask);

    void deleteTask(int id);
    void deleteEpic(int id);
    void deleteSubtask(int id);

    void updateEpicStatus(Epic epic);

    List<Task> getHistory();
}