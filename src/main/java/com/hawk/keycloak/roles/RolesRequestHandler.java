package com.hawk.keycloak.roles;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.util.ResultWindow;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class RolesRequestHandler {
    private final List<String> IGNORED_ROLES = List.of(
            "offline_access",
            "uma_authorization",
            "uma_protection"
    );

    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;

    public Stream<RoleRepresentation> handleRolesRequest(
            Integer firstResult,
            Integer maxResults
    ) {
        auth.requireViewRoles();

        RealmModel realm = session.getContext().getRealm();

        return ResultWindow.limitStream(
                Stream.concat(
                                session.roles().getRealmRolesStream(realm, firstResult, maxResults),
                                session.getContext().getClient().getRolesStream(firstResult, maxResults)
                        ).filter(r -> !IGNORED_ROLES.contains(r.getName()) && !r.getName().startsWith("default-roles-"))
                        .distinct(),
                firstResult,
                maxResults
        ).map(ModelToRepresentation::toRepresentation);

    }

    public Stream<String> handleRoleMembersRequest(
            String roleId,
            Integer firstResult,
            Integer maxResults
    ) {
        auth.requireViewRoles();
        auth.admin().users().requireView();

        RealmModel realm = session.getContext().getRealm();
        RoleModel roleModel = realm.getRoleById(roleId);

        if (roleModel == null) {
            throw new NotFoundException("Could not find role with id");
        }

        if (roleModel.isClientRole()) {
            RoleModel clientRoleModel = session.getContext().getClient().getRole(roleModel.getName());
            if (clientRoleModel == null || !Objects.equals(clientRoleModel.getId(), roleModel.getId())) {
                throw new ForbiddenException("Requested members of a role outside the scope");
            }
        }

        return session.users().getRoleMembersStream(realm, roleModel, firstResult, maxResults)
                .map(UserModel::getId);
    }
}
