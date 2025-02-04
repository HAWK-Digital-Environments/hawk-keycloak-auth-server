package com.hawk.keycloak.resources.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class UserResourcePermission {
    final private String id;
    private List<String> scopes;

    public UserResourcePermission(String userId) {
        this.id = userId;
    }

    public void addScope(String... scope) {
        if (scopes == null) {
            scopes = new ArrayList<>();
        }
        scopes.addAll(Arrays.asList(scope));
    }
}
