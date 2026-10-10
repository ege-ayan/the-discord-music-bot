package io.github.egeayan.musicbot.music;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToLongFunction;

public final class FairQueue<T> {

    private final List<T> items = new ArrayList<>();
    private final ToLongFunction<? super T> ownerOf;

    public FairQueue(ToLongFunction<? super T> ownerOf) {
        this.ownerOf = Objects.requireNonNull(ownerOf, "ownerOf");
    }

    public int add(T item) {
        long owner = ownerOf.applyAsLong(item);
        int index = items.size() - 1;
        while (index >= 0 && ownerOf.applyAsLong(items.get(index)) != owner) {
            index--;
        }
        index++;

        Set<Long> ownersInTurn = new HashSet<>();
        while (index < items.size() && ownersInTurn.add(ownerOf.applyAsLong(items.get(index)))) {
            index++;
        }
        items.add(index, item);
        return index;
    }

    public Optional<T> poll() {
        return items.isEmpty() ? Optional.empty() : Optional.of(items.removeFirst());
    }

    public T get(int index) {
        return items.get(index);
    }

    public T remove(int index) {
        return items.remove(index);
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }

    public List<T> snapshot() {
        return List.copyOf(items);
    }
}
