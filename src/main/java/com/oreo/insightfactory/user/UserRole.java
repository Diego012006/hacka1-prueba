package com.oreo.insightfactory.user;

public enum UserRole {
    CENTRAL,
    BRANCH;

    public boolean isCentral() {
        return this == CENTRAL;
    }
}
