package com.hawk.keycloak.users;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.users.lookup.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class UsersRequestHandler {
    private final UserFinder userFinder;
    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;
    private final UserInfoGenerator userInfoGenerator;

    public Response getUserList(
            String search,
            String attributes,
            List<String> ids,
            Boolean onlineOnly,
            Boolean idsOnly,
            Integer firstResult,
            Integer maxResults
    ) {
        idsOnly = idsOnly != null ? idsOnly : false;

        auth.admin().users().requireQuery();
        if (!idsOnly) {
            auth.admin().users().requireView();
        }

        Stream<UserModel> users = userFinder.findByFilters(
                search,
                attributes,
                ids,
                onlineOnly,
                firstResult,
                maxResults
        );

        if (idsOnly) {
            return Response.ok(users.map(UserModel::getId)).build();
        }

        return Response.ok(users.map(user -> userInfoGenerator.getUserinfo(session.getContext().getRealm(), user))).build();
    }

    public Response getUserCount(
            String search,
            String attributes,
            Boolean onlineOnly
    ) {
        auth.admin().users().requireQuery();

        Stream<UserModel> users = userFinder.findByFilters(
                search,
                attributes,
                null,
                onlineOnly,
                0,
                99999
        );

        return Response.ok(users.count()).build();
    }
}
