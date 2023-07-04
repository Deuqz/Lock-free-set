import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SetImpl<T extends Comparable<T>> implements Set<T> {

    private class Node {
        public final T value;
        public final AtomicStampedReference<Node> atom;

        public Node(T value) {
            this.value = value;
            this.atom = new AtomicStampedReference<>(null, 0);
        }

        public Node(T value, AtomicStampedReference<Node> atom) {
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
            if (next.atom.getStamp() == -1) {
                int version = cur.atom.getStamp();
                cur.atom.compareAndSet(next, next.atom.getReference(), version, version + 1);
            } else {
                cur = next;
            }
            next = cur.atom.getReference();
            compareRes = next == null ? 1 : next.compareValueTo(value);
        }
        return cur;
    }

    @Override
    public boolean add(T value) {
        Node cur = find(value, compareRes -> compareRes <= 0);
        int version = cur.atom.getStamp();
        if (version == -1) {
            return add(value);
        }
        if (cur.compareValueTo(value) == 0) {
            return false;
        }
        Node next = cur.atom.getReference();
        if (next != null && next.compareValueTo(value) <= 0) {
            return add(value);
        }
        Node newNode = new Node(value, new AtomicStampedReference<>(next, 0));
        if (!cur.atom.compareAndSet(next, newNode, version, version + 1)) {
            return add(value);
        }
        return true;
    }

    @Override
    public boolean remove(T value) {
        Node cur = find(value, compareRes -> compareRes < 0);
        int version = cur.atom.getStamp();
        if (version == -1) {
            return remove(value);
        }
        Node next = cur.atom.getReference();
        int versionNext = next == null ? -1 : next.atom.getStamp();
        if (versionNext == -1 || next.compareValueTo(value) > 0) {
            return false;
        }
        if (next.compareValueTo(value) < 0) {
            return remove(value);
        }
        Node nextNext = next.atom.getReference();
        if (!next.atom.compareAndSet(nextNext, nextNext, versionNext, -1)) {
            return remove(value);
        }
        cur.atom.compareAndSet(next, nextNext, version, version + 1);
        return true;
    }

    @Override
    public boolean contains(T value) {
        Node cur = head;
        Node next = cur.atom.getReference();
        while (next != null && next.compareValueTo(value) <= 0) {
            cur = next;
            next = cur.atom.getReference();
        }
        return cur.compareValueTo(value) == 0;
    }

    @Override
    public boolean isEmpty() {
        Node next = head.atom.getReference();
        while (next != null) {
            if (next.atom.getStamp() == -1) {
                int version = head.atom.getStamp();
                head.atom.compareAndSet(next, next.atom.getReference(), version, version + 1);
                next = head.atom.getReference();
            } else {
                return false;
            }
        }
        return true;
    }

    private List<Node> getNodes() {
        List<Node> lst = new ArrayList<>();
        Node cur = head;
        Node next = cur.atom.getReference();
        while (next != null) {
            if (next.atom.getStamp() == -1) {
                int version = cur.atom.getStamp();
                cur.atom.compareAndSet(next, next.atom.getReference(), version, version + 1);
            } else {
                lst.add(cur);
                cur = next;
            }
            next = cur.atom.getReference();
        }
        lst.add(cur);
        return lst;
    }

    @Override
    public Iterator<T> iterator() {
        List<Node> first = getNodes();
        List<Integer> firstVersions = first.stream().map(node -> node.atom.getStamp()).collect(Collectors.toList());
        List<Node> second = getNodes();
        List<Integer> secondVersions = second.stream().map(node -> node.atom.getStamp()).collect(Collectors.toList());
        while (!first.equals(second) || !firstVersions.equals(secondVersions)) {
            first = second;
            firstVersions = secondVersions;
            second = getNodes();
            secondVersions = second.stream().map(node -> node.atom.getStamp()).collect(Collectors.toList());
        }
        return first.stream()
                .skip(1)
                .map(node -> node.value)
                .iterator();
    }
}
