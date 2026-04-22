package com.example.delivery.user.domain.entity;

public enum UserRole {
    CUSTOMER(true),
    OWNER(true),
    MANAGER(false),
    MASTER(false);

    private final boolean signupAllowed;

    UserRole(boolean signupAllowed) {
        this.signupAllowed = signupAllowed;
    }

    public boolean isSignupAllowed() {
        return signupAllowed;
    }

    public boolean isPrivileged() {
        return this == MANAGER || this == MASTER;
    }
}
