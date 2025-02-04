package com.hawk.keycloak.resources;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.resources.lookup.ResourceUserFinder;
import com.hawk.keycloak.resources.model.UserResourcePermission;
import com.hawk.keycloak.resources.service.ResourcePermissionSetter;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ResourceRequestHandler {
    private final ResourceUserFinder resourceUserFinder;
    private final HawkPermissionEvaluator auth;
    private final KeycloakSession session;
    private final ResourceStore resourceStore;
    private final ResourceServer resourceServer;
    private final ResourcePermissionSetter permissionSetter;
    private final AuthorizationProvider authorization;

    public Collection<UserResourcePermission> handleUsersOfResourceRequest(String resourceId) {
        auth.requireViewResourcePermissions();

        Resource resource = resourceStore.findById(resourceServer, resourceId);

        if(resource == null){
            throw new NotFoundException("Resource not found");
        }

        return resourceUserFinder.getUsersOfResource(resource);
    }

    public Response handleSetUserPermissionsRequest(String userId, String resourceId, List<String> scopes) {
        auth.requireManageResourcePermissions();

        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if(user == null){
            throw new NotFoundException("User not found");
        }

        Resource resource = resourceStore.findById(resourceServer, resourceId);
        if(resource == null){
            throw new NotFoundException("Resource not found");
        }

        permissionSetter.setPermissions(user, resource, scopes);

        return Response.noContent().build();
    }

    public Stream<ResourceRepresentation> handleGetResourcesRequest(
            List<String> ids,
            Integer firstResult,
            Integer maxResult
    ) {
        auth.requireViewResourcePermissions();

        Stream<Resource> resources;

        if(ids == null || ids.isEmpty()){
            resources = resourceStore.find(
                    this.resourceServer,
                    Map.of(),
                    firstResult != null ? firstResult : -1,
                    maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS
            ).stream();
        } else {
            resources = ids.stream().map(id -> resourceStore.findById(resourceServer, id));
        }

        return resources
                .distinct()
                .skip(firstResult != null ? firstResult : 0)
                .limit(maxResult != null ? maxResult : Constants.DEFAULT_MAX_RESULTS)
                .map(r -> ModelToRepresentation.toRepresentation(r, resourceServer, authorization, true));
    }
}
