package br.com.longobucco.personal_finance_app.infra.rest.security;

import br.com.longobucco.personal_finance_app.application.exception.ForbiddenException;
import br.com.longobucco.personal_finance_app.core.domain.User;

import java.util.UUID;


public final class AccessGuard {

    private AccessGuard() {
    }

    public static void requireOwnerOrAdmin(User currentUser, UUID resourceOwnerId) {
        boolean isOwner = currentUser.getId().equals(resourceOwnerId);
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw ForbiddenException.notOwner();
        }
    }

    public static void requireOwner(User currentUser, UUID resourceOwnerId) {
        if (!currentUser.getId().equals(resourceOwnerId)) {
            throw ForbiddenException.notOwner();
        }
    }
}
