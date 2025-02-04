package com.hawk.keycloak.users.lookup;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class UserResultProcessor {
    final private boolean idsOnly;
    final private RealmModel realm;
    final private UserPermissionEvaluator usersEvaluator;
    final private KeycloakSession session;
    final private UserInfoGenerator userInfoGenerator;

    public Stream<Map<String, Object>> process(Stream<UserModel> userModels) {
        Stream<UserModel> filteredUserModels = filterModels(userModels);

        if (idsOnly) {
            return toIdObject(filteredUserModels);
        }

        return toUserInfo(filteredUserModels);
    }

    private Stream<UserModel> filterModels(Stream<UserModel> userModels) {
        boolean canViewGlobal = usersEvaluator.canView();

        usersEvaluator.grantIfNoPermission(session.getAttribute(UserModel.GROUPS) != null);

        return userModels.filter(user -> canViewGlobal || usersEvaluator.canView(user));
    }

    private Stream<Map<String, Object>> toUserInfo(Stream<UserModel> userModels) {
        return userModels.map(user -> userInfoGenerator.getUserinfo(realm, user));
    }

    private Stream<Map<String, Object>> toIdObject(Stream<UserModel> userModels) {
        return userModels.map(user -> Map.of("id", user.getId()));
    }
}
