package com.hawk.keycloak.profiles;

import java.util.Objects;

public enum ProfileMode {
    USER,
    ADMIN;

    public static ProfileMode fromString(String mode) {
        if (!Objects.equals(mode, "admin")) {
            return USER;
        }
        return ADMIN;
    }
}
