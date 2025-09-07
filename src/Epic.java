import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Epic extends Task {
    private final List<Integer> subtaskIds = new java.util.ArrayList<>();

    private Duration calcDuration;
    private LocalDateTime calcStartTime;
    private LocalDateTime calcEndTime;

    public Epic(String name, String description) {
        super(name, description);
    }

    public void addSubtask(int id) {
        subtaskIds.add(id);
    }

    public void removeSubtask(int id) {
        subtaskIds.remove((Integer) id);
    }

    public List<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    void recalcTimes(java.util.Map<Integer, Subtask> allSubtasks) {
        if (subtaskIds.isEmpty()) {
            calcDuration = null;
            calcStartTime = null;
            calcEndTime = null;
            return;
        }

        long minutes = subtaskIds.stream()
                .map(allSubtasks::get)
                .filter(java.util.Objects::nonNull)
                .map(Subtask::getDuration)
                .flatMap(Optional::stream)
                .mapToLong(Duration::toMinutes)
                .sum();
        boolean allWithoutDuration = subtaskIds.stream()
                .map(allSubtasks::get)
                .filter(java.util.Objects::nonNull)
                .map(Subtask::getDuration)
                .allMatch(Optional::isEmpty);
        calcDuration = allWithoutDuration ? null : Duration.ofMinutes(minutes);

        calcStartTime = subtaskIds.stream()
                .map(allSubtasks::get)
                .filter(java.util.Objects::nonNull)
                .map(Subtask::getStartTime).flatMap(Optional::stream)
                .min(Comparator.naturalOrder())
                .orElse(null);

        calcEndTime = subtaskIds.stream()
                .map(allSubtasks::get)
                .filter(java.util.Objects::nonNull)
                .map(Subtask::getEndTime).flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Override
    public Optional<Duration> getDuration() {
        return Optional.ofNullable(calcDuration);
    }

    @Override
    public Optional<LocalDateTime> getStartTime() {
        return Optional.ofNullable(calcStartTime);
    }

    @Override
    public Optional<LocalDateTime> getEndTime() {
        return Optional.ofNullable(calcEndTime);
    }

    @Override
    public String toString() {
        return "Эпик № " + getId() + ": " + name + " [" + status + "] - " + description + ". Подзадачи: " + subtaskIds;
    }
}