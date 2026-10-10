package io.github.egeayan.musicbot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CommandParserTest {

    private final CommandParser parser = new CommandParser("!");

    @ParameterizedTest(name = "\"{0}\" -> {1} / \"{2}\"")
    @CsvSource(
            delimiter = '|',
            value = {
                "!play never gonna give you up | play | never gonna give you up",
                "!PLAY Never Gonna             | play | Never Gonna",
                "!p song                       | p    | song",
                "!P song                       | p    | song",
                "!skip                         | skip | ''",
                "!Fs                           | fs   | ''",
                "!remove 3                     | remove | 3",
                "  !remove    3                | remove | 3",
            })
    void parsesNameCaseInsensitivelyAndKeepsArgs(String content, String name, String args) {
        assertThat(parser.parse(content)).contains(new ParsedCommand(name, args));
    }

    @Test
    void argumentsKeepInnerWhitespaceAndNewlines() {
        assertThat(parser.parse("!play a  b\nc")).contains(new ParsedCommand("play", "a  b\nc"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"play song", "!", "! play", "?play", "", "   "})
    void ignoresMessagesThatAreNotCommands(String content) {
        assertThat(parser.parse(content)).isEmpty();
    }

    @Test
    void supportsMultiCharacterPrefixCaseInsensitively() {
        var mb = new CommandParser("mb!");

        assertThat(mb.parse("MB!skip")).contains(new ParsedCommand("skip", ""));
        assertThat(mb.parse("!skip")).isEmpty();
    }

    @Test
    void rejectsBlankPrefix() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CommandParser(" "));
    }
}
