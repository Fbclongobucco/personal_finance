package br.com.longobucco.personal_finance_app.application.security;

import br.com.longobucco.personal_finance_app.application.exception.ForbiddenException;
import br.com.longobucco.personal_finance_app.core.domain.User;

import java.util.UUID;

public final class AccessGuard {

    private AccessGuard() {
    }

    public static boolean isOwnerOrAdmin(User currentUser, UUID resourceOwnerId) {
        return currentUser.getId().equals(resourceOwnerId) || currentUser.isAdmin();
    }

    public static void requireOwnerOrAdmin(User currentUser, UUID resourceOwnerId) {
        if (!isOwnerOrAdmin(currentUser, resourceOwnerId)) {
            throw ForbiddenException.notOwner();
        }
    }

    public static void requireOwner(User currentUser, UUID resourceOwnerId) {
        if (!currentUser.getId().equals(resourceOwnerId)) {
            throw ForbiddenException.notOwner();
        }
    }

    public static void requireAdmin(User currentUser) {
        if (!currentUser.isAdmin()) {
            throw ForbiddenException.notAdmin();
        }
    }
}
