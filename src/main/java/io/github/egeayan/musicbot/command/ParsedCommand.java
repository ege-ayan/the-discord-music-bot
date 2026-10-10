package io.github.egeayan.musicbot.command;

import java.util.Objects;

public record ParsedCommand(String name, String args) {

    public ParsedCommand {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(args, "args");
    }
}
