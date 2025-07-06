public class Main {
    public static void main(String[] args) {
        System.out.println("Поехали!");
        System.out.println();
        TaskManager taskManager = Managers.getDefault();

        Task task1 = new Task("Поучиться", "Открыть Яндекс.Практикум и завершить спринт 4");
        Task task2 = new Task("Отдохнуть", "Запустить Dota2 и выиграть мид");
        taskManager.addTask(task1);
        taskManager.addTask(task2);

        Epic epic1 = new Epic("Переезд", "Переехать в следующем месяце в новую квартиру");
        taskManager.addEpic(epic1);
        Subtask subtask1 = new Subtask("Собрать коробки", "Упаковать все свои вещи по коробкам", epic1.getId());
        Subtask subtask2 = new Subtask("Упаковать кошку", "Положить кошку в переноску", epic1.getId());
        Subtask subtask3 = new Subtask("Сказать слова прощания", "Попрощаться со своей квартирой", epic1.getId());
        taskManager.addSubtask(subtask1);
        taskManager.addSubtask(subtask2);
        taskManager.addSubtask(subtask3);

        Epic epic2 = new Epic("Важный эпик 2", "Сделать что-то в рамках этого эпика");
        taskManager.addEpic(epic2);
        Subtask subtask4 = new Subtask("Задача подзадача 1", "Сделать что-то", epic2.getId());
        taskManager.addSubtask(subtask4);

        System.out.println("->Задачи:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println(task);
        }

        System.out.println("->Эпики:");
        for (Epic epic : taskManager.getAllEpics()) {
            System.out.println(epic);
        }

        System.out.println("->Подзадачи:");
        for (Subtask subtask : taskManager.getAllSubtasks()) {
            System.out.println(subtask);
        }

        subtask1.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateSubtask(subtask1);

        subtask2.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask2);

        subtask3.setStatus(TaskStatus.DONE);
        taskManager.updateSubtask(subtask3);

        System.out.println();
        System.out.println("После изменения статусов:");
        System.out.println(taskManager.getEpicById(epic1.getId()));
        System.out.println(taskManager.getEpicById(epic2.getId()));

        taskManager.deleteTask(task1.getId());
        taskManager.deleteEpic(epic2.getId());

        System.out.println();
        System.out.println("После удаления:");
        System.out.println("->Задачи:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println(task);
        }
        System.out.println("->Эпики:");
        for (Epic epic : taskManager.getAllEpics()) {
            System.out.println(epic);
        }
        System.out.println("->Подзадачи:");
        for (Subtask subtask : taskManager.getAllSubtasks()) {
            System.out.println(subtask);
        }
    }
}