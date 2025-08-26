import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadEmpty() {
        // Пустой менеджер сохраняется и загружается корректно
        File f = tempDir.resolve("empty.csv").toFile();
        FileBackedTaskManager m = new FileBackedTaskManager(f);

        m.save();

        FileBackedTaskManager copy = FileBackedTaskManager.loadFromFile(f);

        assertTrue(copy.getAllTasks().isEmpty());
        assertTrue(copy.getAllEpics().isEmpty());
        assertTrue(copy.getAllSubtasks().isEmpty());
    }

    @Test
    void saveAndLoadSeveral() {
        // Несколько задач разных типов сохраняются и восстанавливаются
        File f = tempDir.resolve("several.csv").toFile();
        FileBackedTaskManager m = new FileBackedTaskManager(f);

        Task t = new Task("Учёба", "Спринт 7");
        m.addTask(t);

        Epic e = new Epic("Переезд", "Собрать вещи");
        m.addEpic(e);

        Subtask s = new Subtask("Коробки", "Кухня", e.getId());
        m.addSubtask(s);

        FileBackedTaskManager copy = FileBackedTaskManager.loadFromFile(f);

        assertEquals(1, copy.getAllTasks().size());
        assertEquals(1, copy.getAllEpics().size());
        assertEquals(1, copy.getAllSubtasks().size());
        assertEquals(e.getId(), copy.getAllSubtasks().get(0).getEpicId());
    }

    @Test
    void keepStatusesAndIdsAfterReload() {
        // Статусы и id сохраняются и восстанавливаются правильно
        File f = tempDir.resolve("statuses.csv").toFile();
        FileBackedTaskManager m = new FileBackedTaskManager(f);

        Task t = new Task("T", "d");
        m.addTask(t);
        t.setStatus(TaskStatus.DONE);
        m.updateTask(t);

        Epic e = new Epic("E", "d");
        m.addEpic(e);

        Subtask s = new Subtask("S", "d", e.getId());
        m.addSubtask(s);
        s.setStatus(TaskStatus.IN_PROGRESS);
        m.updateSubtask(s);

        FileBackedTaskManager copy = FileBackedTaskManager.loadFromFile(f);

        Task t2 = copy.getAllTasks().get(0);
        assertEquals(t.getId(), t2.getId());
        assertEquals(TaskStatus.DONE, t2.getStatus());

        Subtask s2 = copy.getAllSubtasks().get(0);
        assertEquals(s.getId(), s2.getId());
        assertEquals(TaskStatus.IN_PROGRESS, s2.getStatus());
        assertEquals(e.getId(), s2.getEpicId());
    }

    @Test
    void nextIdMovesAfterLoad() {
        // После загрузки новый id не конфликтует со старыми
        File f = tempDir.resolve("ids.csv").toFile();
        FileBackedTaskManager m = new FileBackedTaskManager(f);

        Task t = new Task("Old", "d");
        m.addTask(t); // пусть id = 1 (или больше)

        FileBackedTaskManager copy = FileBackedTaskManager.loadFromFile(f);

        Task newTask = new Task("New", "d");
        copy.addTask(newTask);

        assertNotEquals(t.getId(), newTask.getId());
    }
}