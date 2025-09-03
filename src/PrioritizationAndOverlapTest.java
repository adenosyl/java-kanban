import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrioritizationAndOverlapTest {

    private InMemoryTaskManager manager;

    @BeforeEach
    void setup() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void prioritizedOrderByStartTime() {
        Task a = new Task("A", "d");
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(60)); // 10:00-11:00
        manager.addTask(a);

        Task b = new Task("B", "d");
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        b.setDuration(Duration.ofMinutes(30)); // 09:00-09:30
        manager.addTask(b);

        Task c = new Task("C", "d"); // без startTime -> не попадает в приоритизацию
        manager.addTask(c);

        List<Task> p = manager.getPrioritizedTasks();
        assertEquals(2, p.size(), "Задачи без startTime не должны попадать в приоритизированный список");
        assertEquals(b.getId(), p.get(0).getId());
        assertEquals(a.getId(), p.get(1).getId());
    }

    @Test
    void rejectOverlappingIntervals() {
        Task a = new Task("A", "d");
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(120)); // 10:00-12:00
        manager.addTask(a);

        Task overlap = new Task("B", "d");
        overlap.setStartTime(LocalDateTime.of(2025, 1, 1, 11, 0)); // пересекается 11:00-12:30
        overlap.setDuration(Duration.ofMinutes(90));

        assertThrows(IllegalStateException.class, () -> manager.addTask(overlap),
                "Пересекающиеся интервалы должны отклоняться");
    }

    @Test
    void touchingAtBorderIsAllowed() {
        Task a = new Task("A", "d");
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(60)); // до 11:00
        manager.addTask(a);

        Task b = new Task("B", "d");
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 11, 0)); // стык без пересечения
        b.setDuration(Duration.ofMinutes(30));

        assertDoesNotThrow(() -> manager.addTask(b),
                "Примыкание [aEnd == bStart) не считается пересечением");
    }

    @Test
    void updateChecksOverlapToo() {
        Task a = new Task("A", "d");
        a.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        a.setDuration(Duration.ofMinutes(60)); // 10-11
        manager.addTask(a);

        Task b = new Task("B", "d");
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 8, 0));
        b.setDuration(Duration.ofMinutes(60)); // 8-9
        manager.addTask(b);

        // переместим b на пересечение с a
        b.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 30)); // 10:30-11:30
        b.setDuration(Duration.ofMinutes(60));
        assertThrows(IllegalStateException.class, () -> manager.updateTask(b),
                "Update тоже должен проверять пересечения");
    }
}