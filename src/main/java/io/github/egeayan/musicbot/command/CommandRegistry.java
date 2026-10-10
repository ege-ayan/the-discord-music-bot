package io.github.egeayan.musicbot.command;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CommandRegistry {

    private final Map<String, Command> byName;

    public CommandRegistry(List<? extends Command> commands) {
        Map<String, Command> map = new HashMap<>();
        for (Command command : commands) {
            register(map, command.name(), command);
            command.aliases().forEach(alias -> register(map, alias, command));
        }
        this.byName = Map.copyOf(map);
    }

    public Optional<Command> find(String name) {
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    private static void register(Map<String, Command> map, String name, Command command) {
        Command previous = map.putIfAbsent(name.toLowerCase(Locale.ROOT), command);
        if (previous != null) {
            throw new IllegalStateException(
                    "Command name '%s' is used by both %s and %s".formatted(name, previous.name(), command.name()));
        }
    }
}
