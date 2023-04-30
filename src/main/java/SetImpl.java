import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Function;

public class SetImpl<T extends Comparable<T>> implements Set<T> {

    private class Node {
        public final T value;
        public final AtomicMarkableReference<Node> atom;

        public Node(T value) {
            this.value = value;
            this.atom = new AtomicMarkableReference<>(null, false);
        }

        public Node(T value, AtomicMarkableReference<Node> atom) {
            this.value = value;
            this.atom = atom;
        }

        public int compareValueTo(T otherValue) {
            if (value == null) {
                return -1;
            }
            return value.compareTo(otherValue);
        }
    }

    private final Node head = new Node(null);

    private Node find(T value, Function<Integer, Boolean> checker) {
        Node cur = head;
        Node next = cur.atom.getReference();
        int compareRes = next == null ? 1 : next.compareValueTo(value);
        while (next != null && checker.apply(compareRes)) {
            cur = next;
            next = cur.atom.getReference();
            compareRes = next == null ? 1 : next.compareValueTo(value);
        }
        return cur;
    }

    @Override
    public boolean add(T value) {
        Node cur = find(value, compareRes -> compareRes <= 0);
        if (cur.compareValueTo(value) == 0) {
            return false;
        }
        Node next = cur.atom.getReference();
        if (next != null && (next.atom.isMarked() || next.compareValueTo(value) <= 0)) {
            return add(value);
        }
        Node newNode = new Node(value, new AtomicMarkableReference<>(next, false));
        if (!cur.atom.compareAndSet(next, newNode, false, false)) {
            return add(value);
        }
        return true;
    }

    @Override
    public boolean remove(T value) {
        Node cur = find(value, compareRes -> compareRes < 0);
        Node next = cur.atom.getReference();
        if (next == null || next.compareValueTo(value) > 0) {
            return false;
        }
        if (next.compareValueTo(value) < 0) {
            return remove(value);
        }
        Node nextNext = next.atom.getReference();
        if (!next.atom.compareAndSet(nextNext, nextNext, false, true)) {
            return remove(value);
        }
        if (!cur.atom.compareAndSet(next, nextNext, false, false)) {
            next.atom.set(nextNext, false);
            return remove(value);
        }
        return true;
    }

    @Override
    public boolean contains(T value) {
        Node cur = find(value, compareRes -> compareRes <= 0);
        return cur.compareValueTo(value) == 0;
    }

    @Override
    public boolean isEmpty() {
        return head.atom.getReference() == null;
    }

    private List<Node> getNodes() {
        List<Node> lst = new ArrayList<>();
        Node cur = head;
        Node next = cur.atom.getReference();
        while (next != null) {
            lst.add(cur);
            cur = next;
            next = cur.atom.getReference();
        }
        lst.add(cur);
        return lst;
    }

    @Override
    public Iterator<T> iterator() {
        List<Node> first = getNodes();
        List<Node> second = getNodes();
        while(!first.equals(second)) {
            first = second;
            second = getNodes();
        }
        return first.stream()
                .skip(1)
                .map(node -> node.value)
                .iterator();
    }
}
