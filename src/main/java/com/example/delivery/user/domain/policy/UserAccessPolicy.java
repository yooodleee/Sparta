package com.example.delivery.user.domain.policy;

import com.example.delivery.global.common.auth.LoginUser;
import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;
import com.example.delivery.user.domain.command.UserUpdateCommand;
import com.example.delivery.user.domain.entity.UserRole;

public final class UserAccessPolicy {

    private UserAccessPolicy() {
    }

    public static void assertReadable(LoginUser actor, String targetUsername, UserRole targetRole) {
        if (actor.isSelf(targetUsername)) {
            return;
        }
        assertManageableByNonSelf(actor, targetRole);
    }

    public static void assertUpdatable(LoginUser actor, String targetUsername, UserRole targetRole,
            UserUpdateCommand command) {
        boolean self = actor.isSelf(targetUsername);
        if (!self) {
            assertManageableByNonSelf(actor, targetRole);
        }
        if (command.newPasswordHash().isPresent() && !self) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public static void assertDeletable(LoginUser actor, String targetUsername, UserRole targetRole) {
        if (actor.isSelf(targetUsername)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }
        assertManageableByNonSelf(actor, targetRole);
    }

    public static void assertRoleChangeable(LoginUser actor, String targetUsername) {
        if (!actor.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (actor.isSelf(targetUsername)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void assertManageableByNonSelf(LoginUser actor, UserRole targetRole) {
        if (!actor.isManagerOrMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (targetRole.isPrivileged() && !actor.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
