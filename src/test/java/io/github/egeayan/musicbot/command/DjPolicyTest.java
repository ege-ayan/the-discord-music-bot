package io.github.egeayan.musicbot.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DjPolicyTest {

    private static final long OWNER = 10;
    private static final long DJ_ROLE = 500;
    private final DjPolicy policy = new DjPolicy(OWNER, DJ_ROLE);

    @Test
    void ownerIsAlwaysDj() {
        assertThat(policy.isDj(OWNER, false, Set.of())).isTrue();
    }

    @Test
    void manageServerGrantsDj() {
        assertThat(policy.isDj(1, true, Set.of())).isTrue();
    }

    @Test
    void djRoleGrantsDj() {
        assertThat(policy.isDj(1, false, Set.of(7L, DJ_ROLE))).isTrue();
    }

    @Test
    void regularMemberIsNotDj() {
        assertThat(policy.isDj(1, false, Set.of(7L))).isFalse();
    }

    @Test
    void unsetIdsNeverMatch() {
        var unset = new DjPolicy(0, 0);

        assertThat(unset.isDj(0, false, Set.of(0L))).isFalse();
    }
}
