public class Task {

    protected String name;
    protected String description;
    protected int id;
    protected TaskStatus status;

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = TaskStatus.NEW;
    }

    public Task(Task other) {
        this.name = other.name;
        this.description = other.description;
        this.id = other.id;
        this.status = other.status;
    }

    public static Task copyOf(Task t) {
        if (t == null) return null;
        if (t instanceof Subtask s) {
            Subtask copy = new Subtask(s.getName(), s.getDescription(), s.getEpicId());
            copy.setId(s.getId());
            copy.setStatus(s.getStatus());
            return copy;
        } else if (t instanceof Epic e) {
            Epic copy = new Epic(e.getName(), e.getDescription());
            copy.setId(e.getId());
            copy.setStatus(e.getStatus());

            for (Integer subId : e.getSubtaskIds()) {
                copy.addSubtask(subId);
            }
            return copy;
        } else {
            return new Task(t);
        }
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Задача №" + id + ": " + name + " [" + status + "] - " + description;
    }
}
