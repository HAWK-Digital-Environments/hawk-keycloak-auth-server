package com.hawk.keycloak.resources.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserResourcePermissionList {
    private Map<String, UserResourcePermission> permissions;

    public UserResourcePermissionList() {
    }

    public Collection<UserResourcePermission> getPermissions() {
        if (permissions == null) {
            return null;
        }
        return permissions.values();
    }

    public void addPermission(String requester, UserResourcePermission permission) {
        if (permissions == null) {
            permissions = new HashMap<>();
        }
        permissions.put(requester, permission);
    }

    public UserResourcePermission getPermission(String requester) {
        if (permissions == null) {
            return null;
        }
        return permissions.get(requester);
    }
}