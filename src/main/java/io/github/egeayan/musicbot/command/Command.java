package io.github.egeayan.musicbot.command;

import java.util.Set;

public interface Command {

    String name();

    default Set<String> aliases() {
        return Set.of();
    }

    void execute(CommandContext ctx);
}
