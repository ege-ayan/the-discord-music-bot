package io.github.egeayan.musicbot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommandRegistryTest {

    private record StubCommand(String name, Set<String> aliases) implements Command {
        @Override
        public void execute(CommandContext ctx) {}
    }

    private final StubCommand play = new StubCommand("play", Set.of("p"));
    private final StubCommand skip = new StubCommand("skip", Set.of("s"));

    @Test
    void findsByNameAndAliasIgnoringCase() {
        var registry = new CommandRegistry(List.of(play, skip));

        assertThat(registry.find("play")).containsSame(play);
        assertThat(registry.find("P")).containsSame(play);
        assertThat(registry.find("SKIP")).containsSame(skip);
        assertThat(registry.find("s")).containsSame(skip);
    }

    @Test
    void unknownNameIsEmpty() {
        assertThat(new CommandRegistry(List.of(play)).find("stop")).isEmpty();
    }

    @Test
    void rejectsConflictingNames() {
        var clash = new StubCommand("pause", Set.of("P"));

        assertThatIllegalStateException()
                .isThrownBy(() -> new CommandRegistry(List.of(play, clash)))
                .withMessageContaining("play")
                .withMessageContaining("pause");
    }
}
