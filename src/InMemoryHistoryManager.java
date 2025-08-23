import java.util.*;

public class InMemoryHistoryManager implements HistoryManager {

    private static class Node {
        Task task;
        Node prev;
        Node next;
        int id;
        Node(Task task) {
            this.task = task;
            this.id = task.getId();
        }
    }

    private final Map<Integer, Node> index = new HashMap<>(); // id -> node
    private Node head;
    private Node tail;

    @Override
    public void add(Task task) {
        if (task == null) return;
        Task snapshot = Task.copyOf(task);
        Node old = index.get(snapshot.getId());
        if (old != null) {
            removeNode(old);
        }
        linkLast(snapshot);
    }

    @Override
    public void remove(int id) {
        Node node = index.get(id);
        if (node != null) {
            removeNode(node);
        }
    }
    @Override
    public List<Task> getHistory() {
        List<Task> list = new ArrayList<>();
        Node cur = head;
        while (cur != null) {
            list.add(Task.copyOf(cur.task));
            cur = cur.next;
        }
        return list;
    }

    private void linkLast(Task task) {
        Node node = new Node(task);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        index.put(node.id, node);
    }

    private void removeNode(Node node) {
        Node p = node.prev;
        Node n = node.next;
        if (p != null) p.next = n; else head = n;
        if (n != null) n.prev = p; else tail = p;
        node.prev = node.next = null;
        index.remove(node.id);
    }
}