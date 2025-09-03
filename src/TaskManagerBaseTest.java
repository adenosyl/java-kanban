import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class TaskManagerBaseTest<T extends TaskManager> {

    protected T manager;

    /**
     * Наследники должны вернуть конкретную реализацию менеджера
     */
    protected abstract T createManager();

    @BeforeEach
    void setup() {
        manager = createManager();
    }

    @Test
    void addAndFindDifferentTypes() {
        Epic e = new Epic("E", "d");
        manager.addEpic(e);

        Subtask s = new Subtask("S", "d", e.getId());
        manager.addSubtask(s);

        Task t = new Task("T", "d");
        manager.addTask(t);

        assertNotNull(manager.getEpicById(e.getId()));
        assertNotNull(manager.getSubtaskById(s.getId()));
        assertNotNull(manager.getTaskById(t.getId()));
    }

    @Test
    void touchingIntervalsAllowed() {
        Task a = new Task("A", "d");
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(60)); // 10:00-11:00
        manager.addTask(a);

        Task b = new Task("B", "d");
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 11, 0)); // стык, не пересечение
        b.setDuration(Duration.ofMinutes(30));

        assertDoesNotThrow(() -> manager.addTask(b));
    }
}
