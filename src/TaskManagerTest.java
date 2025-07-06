import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TaskManagerTest {

    private TaskManager taskManager;

    @BeforeEach
    void setup() {
        // инициализация менеджеров перед каждым тестом
        taskManager = Managers.getDefault();
    }

    @Test
    void shouldTasksBeEqualIfIdsMatch() {
        // проверьте, что экземпляры класса Task равны друг другу, если равен их id;
        Task t1 = new Task("Task1", "Desc");
        Task t2 = new Task("Task2", "Desc");

        t1.setId(1);
        t2.setId(1);

        assertEquals(t1, t2, "Задачи с одинаковыми id должны быть равны.");
    }

    @Test
    void shouldEpicsBeEqualIfIdsMatch() {
        // проверьте, что экземпляры класса Epic равны друг другу, если равен их id;
        Epic e1 = new Epic("Epic1", "Desc");
        Epic e2 = new Epic("Epic2", "Desc");

        e1.setId(2);
        e2.setId(2);

        assertEquals(e1, e2, "Эпики с одинаковыми id должны быть равны.");
    }

    @Test
    void shouldSubtasksBeEqualIfIdsMatch() {
        // проверьте, что экземпляры класса Subtask равны друг другу, если равен их id;
        Subtask s1 = new Subtask("Subtask1", "Desc", 5);
        Subtask s2 = new Subtask("Subtask2", "Desc", 5);

        s1.setId(3);
        s2.setId(3);

        assertEquals(s1, s2, "Подзадачи с одинаковыми id должны быть равны.");
    }

    @Test
    void shouldNotAllowEpicToBeItsOwnSubtask() {
        // проверьте, что объект Epic нельзя добавить в самого себя в виде подзадачи;
        InMemoryTaskManager manager = new InMemoryTaskManager();
        Epic epic = new Epic("Epic", "Desc");
        manager.addEpic(epic);

        Subtask invalidSubtask = new Subtask("Subtask", "Desc", epic.getId());
        invalidSubtask.setId(epic.getId());

        assertThrows(IllegalArgumentException.class, () -> manager.addSubtask(invalidSubtask),
                "Эпик не может быть подзадачей самого себя.");
    }

    @Test
    void shouldNotAllowSubtaskToBeItsOwnEpic() {
        // проверьте, что объект Subtask нельзя сделать своим же эпиком;
        InMemoryTaskManager manager = new InMemoryTaskManager();
        Epic epic = new Epic("Epic", "Desc");
        manager.addEpic(epic);

        Subtask subtask = new Subtask("Subtask", "Desc", epic.getId());
        manager.addSubtask(subtask);

        Epic invalidEpic = new Epic("Epic2", "Desc");
        invalidEpic.setId(subtask.getId());

        assertThrows(IllegalArgumentException.class, () -> manager.addEpic(invalidEpic),
                "Подзадача не может быть эпиком самой себя.");
    }

    @Test
    void managersShouldReturnInitializedInstances() {
        // убедитесь, что утилитарный класс всегда возвращает проинициализированные и готовые к работе экземпляры менеджеров;
        assertNotNull(Managers.getDefault(), "TaskManager не должен быть null.");
        assertNotNull(Managers.getDefaultHistory(), "HistoryManager не должен быть null.");
    }

    @Test
    void shouldAddDifferentTypesAndFindById() {
        // проверьте, что InMemoryTaskManager действительно добавляет задачи разного типа и может найти их по id;
        Epic epic = new Epic("Epic", "Desc");
        taskManager.addEpic(epic);

        Subtask subtask = new Subtask("Subtask", "Desc", epic.getId());
        taskManager.addSubtask(subtask);

        Task task = new Task("Task", "Desc");
        taskManager.addTask(task);

        assertNotNull(taskManager.getEpicById(epic.getId()), "Эпик не найден.");
        assertNotNull(taskManager.getSubtaskById(subtask.getId()), "Подзадача не найдена.");
        assertNotNull(taskManager.getTaskById(task.getId()), "Задача не найдена.");
    }

    @Test
    void shouldHandleManualAndAutoIdsWithoutConflict() {
        // проверьте, что задачи с заданным id и сгенерированным id не конфликтуют внутри менеджера;
        InMemoryTaskManager manager = new InMemoryTaskManager();

        Task manualTask = new Task("Manual Task", "Desc");
        manualTask.setId(100);
        manager.addTask(manualTask);

        Task autoTask = new Task("Auto Task", "Desc");
        manager.addTask(autoTask);

        assertNotEquals(manualTask.getId(), autoTask.getId(), "Id не должны конфликтовать.");
    }

    @Test
    void shouldNotChangeTaskAfterAddingToManager() {
        // создайте тест, в котором проверяется неизменность задачи (по всем полям) при добавлении задачи в менеджер
        Task task = new Task("Task", "Desc");
        taskManager.addTask(task);

        task.setName("Modified");
        task.setDescription("Modified Desc");

        Task savedTask = taskManager.getTaskById(task.getId());

        assertNotEquals("Modified", savedTask.getName(), "Данные задачи в менеджере не должны изменяться.");
        assertNotEquals("Modified Desc", savedTask.getDescription(), "Данные задачи в менеджере не должны изменяться.");
    }

    @Test
    void historyShouldStorePreviousTaskVersion() {
        // убедитесь, что задачи, добавляемые в HistoryManager, сохраняют предыдущую версию задачи и её данных.
        Task task = new Task("Task", "Desc");
        taskManager.addTask(task);
        taskManager.getTaskById(task.getId());

        task.setName("Changed");

        List<Task> history = taskManager.getHistory();

        assertNotEquals("Changed", history.get(0).getName(), "История должна хранить старую версию задачи.");
    }
}
