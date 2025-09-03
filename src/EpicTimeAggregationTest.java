import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EpicTimeAggregationTest {

    private InMemoryTaskManager manager;

    @BeforeEach
    void setup() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void aggregateStartEndAndDurationFromSubtasks() {
        Epic e = new Epic("E", "d");
        manager.addEpic(e);

        Subtask s1 = new Subtask("S1", "d", e.getId());
        s1.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        s1.setDuration(Duration.ofMinutes(30)); // 10:00–10:30
        manager.addSubtask(s1);

        Subtask s2 = new Subtask("S2", "d", e.getId());
        // без пересечения: начинаем ровно после s1
        s2.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 30));
        s2.setDuration(Duration.ofMinutes(90)); // 10:30–12:00
        manager.addSubtask(s2);

        Epic reloaded = manager.getEpicById(e.getId());
        // start = минимальный старт сабтасков
        assertEquals(LocalDateTime.of(2025, 1, 1, 10, 0),
                reloaded.getStartTime().orElseThrow());
        // end = максимальный конец сабтасков
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0),
                reloaded.getEndTime().orElseThrow());
        // duration эпика = сумма длительностей сабтасков (30 + 90 = 120)
        assertEquals(Duration.ofMinutes(120),
                reloaded.getDuration().orElseThrow());
    }

    @Test
    void epicTimesAreEmptyWhenNoSubtasks() {
        Epic e = new Epic("E", "d");
        manager.addEpic(e);
        Epic got = manager.getEpicById(e.getId());
        assertTrue(got.getStartTime().isEmpty());
        assertTrue(got.getEndTime().isEmpty());
        assertTrue(got.getDuration().isEmpty());
    }
}