package io.github.egeayan.musicbot.music;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FairQueueTest {

    private record Item(String name) {
        long owner() {
            return name.charAt(0);
        }
    }

    private final FairQueue<Item> queue = new FairQueue<>(Item::owner);

    private void addAll(String... names) {
        for (String name : names) {
            queue.add(new Item(name));
        }
    }

    private String order() {
        return String.join(" ", queue.snapshot().stream().map(Item::name).toList());
    }

    @Test
    void singleOwnerKeepsInsertionOrder() {
        addAll("A1", "A2", "A3");

        assertThat(order()).isEqualTo("A1 A2 A3");
    }

    @Test
    void newOwnerJumpsAheadOfBacklog() {
        addAll("A1", "A2", "A3", "B1");

        assertThat(order()).isEqualTo("A1 B1 A2 A3");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "A1 A2 A3 B1 B2, A1 B1 A2 B2 A3",
        "A1 A2 B1 C1 C2 B2, A1 B1 C1 A2 B2 C2",
        "A1 B1 A2 B2, A1 B1 A2 B2",
        "A1 A2 A3 B1 C1, A1 B1 C1 A2 A3",
    })
    void interleavesOwnersRoundRobin(String added, String expected) {
        addAll(added.split(" "));

        assertThat(order()).isEqualTo(expected);
    }

    @Test
    void addReturnsInsertionIndex() {
        assertThat(queue.add(new Item("A1"))).isZero();
        assertThat(queue.add(new Item("A2"))).isEqualTo(1);
        assertThat(queue.add(new Item("B1"))).isEqualTo(1);
    }

    @Test
    void pollRemovesFromHead() {
        addAll("A1", "B1");

        assertThat(queue.poll()).contains(new Item("A1"));
        assertThat(queue.poll()).contains(new Item("B1"));
        assertThat(queue.poll()).isEmpty();
    }

    @Test
    void removeByIndex() {
        addAll("A1", "A2", "A3");

        assertThat(queue.remove(1)).isEqualTo(new Item("A2"));
        assertThat(order()).isEqualTo("A1 A3");
    }

    @Test
    void keepsAllItemsUnderLoad() {
        IntStream.range(0, 100).forEach(i -> queue.add(new Item((char) ('A' + i % 7) + "" + i)));

        assertThat(queue.size()).isEqualTo(100);
    }

    @Test
    void snapshotIsDetachedFromQueue() {
        addAll("A1");
        var snapshot = queue.snapshot();

        queue.clear();

        assertThat(snapshot).hasSize(1);
        assertThat(queue.isEmpty()).isTrue();
    }
}
