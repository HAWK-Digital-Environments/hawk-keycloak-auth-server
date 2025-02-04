package com.hawk.keycloak.users.lookup;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class UserSearcher {
    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;

    public Stream<UserModel> searchByIds(
            String[] userIds,
            RealmModel realm,
            Integer firstResult,
            Integer maxResults
    ) {
        Stream<UserModel> userModels = Stream.empty();

        for (String userId : userIds) {
            UserModel userModel = session.users().getUserById(realm, userId);
            if (userModel != null) {
                userModels = Stream.concat(userModels, Stream.of(userModel));
            }
        }

        return userModels.distinct()
                .skip(firstResult).limit(maxResults);
    }

    public Stream<UserModel> searchByAttributes(
            Map<String, String> attributes,
            RealmModel realm,
            Integer firstResult,
            Integer maxResults
    ) {
        attributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, "false");

        if (!auth.admin().users().canView()) {
            Set<String> groupModels = auth.admin().groups().getGroupsWithViewPermission();

            if (!groupModels.isEmpty()) {
                session.setAttribute(UserModel.GROUPS, groupModels);
            }
        }

        return session.users().searchForUserStream(realm, attributes, firstResult, maxResults);
    }
}
