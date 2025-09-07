import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadEmpty() {

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

    @Test
    void saveAndLoad_TimeFieldsPersisted() {
        // Время (startTime, duration) сохраняются и восстанавливаются; endTime совпадает.
        File f = tempDir.resolve("with_time.csv").toFile();
        FileBackedTaskManager m = new FileBackedTaskManager(f);

        Task t = new Task("Учёба", "Спринт 8");
        t.setStartTime(LocalDateTime.of(2025, 2, 10, 12, 30));
        t.setDuration(Duration.ofMinutes(90)); // 12:30-14:00
        m.addTask(t);

        Epic e = new Epic("Эпик", "Агрегация");
        m.addEpic(e);

        Subtask s = new Subtask("Саб", "d", e.getId());
        s.setStartTime(LocalDateTime.of(2025, 2, 10, 9, 0));
        s.setDuration(Duration.ofMinutes(30)); // 09:00-09:30
        m.addSubtask(s);

        FileBackedTaskManager copy = FileBackedTaskManager.loadFromFile(f);

        Task t2 = copy.getAllTasks().get(0);
        assertEquals(t.getStartTime(), t2.getStartTime(), "startTime задачи должен сохраниться");
        assertEquals(t.getDuration(), t2.getDuration(), "duration задачи должен сохраниться");
        assertEquals(t.getEndTime(), t2.getEndTime(), "endTime рассчитывается одинаково");

        Subtask s2 = copy.getAllSubtasks().get(0);
        assertEquals(s.getStartTime(), s2.getStartTime(), "startTime сабтаска должен сохраниться");
        assertEquals(s.getDuration(), s2.getDuration(), "duration сабтаска должен сохраниться");
        assertEquals(s.getEndTime(), s2.getEndTime(), "endTime сабтаска рассчитывается одинаково");

        Epic e2 = copy.getAllEpics().get(0);
        // у эпика время расчётное — должно быть посчитано из сабтасков после загрузки
        assertTrue(e2.getStartTime().isPresent(), "У эпика должен быть вычислен startTime");
        assertTrue(e2.getEndTime().isPresent(), "У эпика должен быть вычислен endTime");
        assertTrue(e2.getDuration().isPresent(), "У эпика должна быть вычислена duration");
    }

    @Test
    void backwardCompatibility_loadOldCsvFormat() throws Exception {
        // Старый CSV без колонок времени должен читаться (поля времени пустые)
        File f = tempDir.resolve("old_format.csv").toFile();
        String oldHeader = "id,type,name,status,description,epic";
        String content = oldHeader + "\n" +
                "1,TASK,Old,NEW,desc,\n" +
                "2,EPIC,Epic,NEW,desc,\n" +
                "3,SUBTASK,Sub,NEW,desc,2\n";
        Files.writeString(f.toPath(), content);

        FileBackedTaskManager copy = FileBackedTaskManager.loadFromFile(f);
        assertEquals(1, copy.getAllTasks().size());
        assertEquals(1, copy.getAllEpics().size());
        assertEquals(1, copy.getAllSubtasks().size());

        // у задач из старого файла поля времени должны быть пустыми
        assertTrue(copy.getAllTasks().get(0).getStartTime().isEmpty());
        assertTrue(copy.getAllTasks().get(0).getDuration().isEmpty());
    }
}