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
    void historyHasNoDuplicatesAndMovesToTail() {
        // История должна быть без дублей; повторный просмотр переносит задачу в хвост
        Task a = new Task("A", "d");
        taskManager.addTask(a);
        Task b = new Task("B", "d");
        taskManager.addTask(b);
        Task c = new Task("C", "d");
        taskManager.addTask(c);

        taskManager.getTaskById(a.getId()); // A
        taskManager.getTaskById(b.getId()); // A,B
        taskManager.getTaskById(c.getId()); // A,B,C

        taskManager.getTaskById(b.getId()); // A,C,B

        List<Task> h = taskManager.getHistory();
        assertEquals(3, h.size(), "История не должна содержать дубликаты.");
        assertEquals(a.getId(), h.get(0).getId(), "Должен сохраняться порядок просмотров.");
        assertEquals(c.getId(), h.get(1).getId());
        assertEquals(b.getId(), h.get(2).getId(), "Повторно просмотренная задача должна быть в конце.");
    }

    @Test
    void historyRemovesOnDelete_head_middle_tail() {
        // Удаление задач из менеджера должно удалять их из истории (голова/середина/хвост)
        Task t1 = new Task("T1", "d");
        taskManager.addTask(t1);
        Task t2 = new Task("T2", "d");
        taskManager.addTask(t2);
        Task t3 = new Task("T3", "d");
        taskManager.addTask(t3);

        taskManager.getTaskById(t1.getId()); // head
        taskManager.getTaskById(t2.getId()); // middle
        taskManager.getTaskById(t3.getId()); // tail

        taskManager.deleteTask(t2.getId()); // remove middle
        List<Task> h = taskManager.getHistory();
        assertEquals(2, h.size());
        assertEquals(t1.getId(), h.get(0).getId());
        assertEquals(t3.getId(), h.get(1).getId());

        taskManager.deleteTask(t1.getId()); // remove head
        h = taskManager.getHistory();
        assertEquals(1, h.size());
        assertEquals(t3.getId(), h.getFirst().getId());

        taskManager.deleteTask(t3.getId()); // remove tail
        h = taskManager.getHistory();
        assertTrue(h.isEmpty(), "История должна очищаться при удалении задач.");
    }

    @Test
    void historyIsUnlimited() {
        // История должна быть неограниченной по размеру
        int n = 25;
        for (int i = 0; i < n; i++) {
            Task t = new Task("T" + i, "d");
            taskManager.addTask(t);
            taskManager.getTaskById(t.getId());
        }
        assertEquals(n, taskManager.getHistory().size(), "История должна быть неограниченной.");
    }

    @Test
    void deletingEpicRemovesItsSubtasksFromManagerAndHistory() {
        // Удаление эпика должно удалять его и его подзадачи из менеджера и из истории
        Epic e = new Epic("E", "d");
        taskManager.addEpic(e);
        Subtask s1 = new Subtask("S1", "d", e.getId());
        taskManager.addSubtask(s1);
        Subtask s2 = new Subtask("S2", "d", e.getId());
        taskManager.addSubtask(s2);

        taskManager.getEpicById(e.getId());
        taskManager.getSubtaskById(s1.getId());
        taskManager.getSubtaskById(s2.getId());

        taskManager.deleteEpic(e.getId());

        assertTrue(taskManager.getHistory().isEmpty(), "История должна очиститься от эпика и его подзадач.");
        assertNull(taskManager.getSubtaskById(s1.getId()), "Подзадача должна быть удалена вместе с эпиком.");
        assertNull(taskManager.getSubtaskById(s2.getId()), "Подзадача должна быть удалена вместе с эпиком.");
    }

    @Test
    void deletingSubtaskRemovesIdFromEpicAndUpdatesStatus() {
        // При удалении подзадачи её id не должен оставаться внутри эпика; статус эпика пересчитывается
        Epic e = new Epic("E", "d");
        taskManager.addEpic(e);
        Subtask s1 = new Subtask("S1", "d", e.getId());
        taskManager.addSubtask(s1);
        Subtask s2 = new Subtask("S2", "d", e.getId());
        taskManager.addSubtask(s2);

        // Смешанные статусы -> IN_PROGRESS
        s1.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(s1);
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getEpicById(e.getId()).getStatus());

        // Удаляем DONE-подзадачу -> остаётся NEW -> статус NEW
        taskManager.deleteSubtask(s1.getId());
        Epic after = taskManager.getEpicById(e.getId());
        assertFalse(after.getSubtaskIds().contains(s1.getId()), "В эпике не должно оставаться неактуальных id подзадач.");
        assertEquals(TaskStatus.NEW, after.getStatus(), "Статус эпика должен перерассчитаться после удаления подзадачи.");
    }

    @Test
    void epicStatusCalculation_AllCases() {
        // Проверка всех веток расчёта статуса эпика
        Epic e = new Epic("E", "d");
        taskManager.addEpic(e);
        Subtask a = new Subtask("A", "d", e.getId());
        taskManager.addSubtask(a);
        Subtask b = new Subtask("B", "d", e.getId());
        taskManager.addSubtask(b);

        // оба NEW -> NEW
        assertEquals(TaskStatus.NEW, taskManager.getEpicById(e.getId()).getStatus());

        // один IN_PROGRESS -> IN_PROGRESS
        a.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateSubtask(a);
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getEpicById(e.getId()).getStatus());

        // DONE + NEW -> IN_PROGRESS (смешанные)
        a.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(a);
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getEpicById(e.getId()).getStatus());

        // оба DONE -> DONE
        b.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(b);
        assertEquals(TaskStatus.DONE, taskManager.getEpicById(e.getId()).getStatus());
    }
    
    @Test
    void shouldRejectDuplicateIdsOnAdd() {
        // Попытки добавить сущности с занятым id должны приводить к исключению
        Task t1 = new Task("T1", "d");
        taskManager.addTask(t1);
        Task t2 = new Task("T2", "d");
        t2.setId(t1.getId());
        assertThrows(IllegalArgumentException.class, () -> taskManager.addTask(t2),
                "Добавление задачи с занятым id должно бросать исключение.");

        Epic e1 = new Epic("E1", "d");
        taskManager.addEpic(e1);
        Epic e2 = new Epic("E2", "d");
        e2.setId(e1.getId());
        assertThrows(IllegalArgumentException.class, () -> taskManager.addEpic(e2),
                "Добавление эпика с занятым id должно бросать исключение.");

        Subtask s1 = new Subtask("S1", "d", e1.getId());
        taskManager.addSubtask(s1);
        Subtask s2 = new Subtask("S2", "d", e1.getId());
        s2.setId(s1.getId());
        assertThrows(IllegalArgumentException.class, () -> taskManager.addSubtask(s2),
                "Добавление подзадачи с занятым id должно бросать исключение.");
    }

    @Test
    void addingSubtaskForMissingEpicShouldFail() {
        // Добавление подзадачи к несуществующему эпику должно бросать исключение
        Subtask s = new Subtask("S", "d", 9999);
        assertThrows(IllegalArgumentException.class, () -> taskManager.addSubtask(s),
                "Эпик не найден: должна быть ошибка при добавлении подзадачи.");
    }
}
