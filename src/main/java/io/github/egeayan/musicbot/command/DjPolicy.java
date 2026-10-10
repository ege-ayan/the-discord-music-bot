package io.github.egeayan.musicbot.command;

import java.util.Set;

public record DjPolicy(long ownerId, long djRoleId) {

    public boolean isDj(long userId, boolean canManageServer, Set<Long> roleIds) {
        return (ownerId != 0 && userId == ownerId) || canManageServer || (djRoleId != 0 && roleIds.contains(djRoleId));
    }
}
