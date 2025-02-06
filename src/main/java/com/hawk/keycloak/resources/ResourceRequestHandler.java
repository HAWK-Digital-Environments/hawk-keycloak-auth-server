package com.hawk.keycloak.resources;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.resources.lookup.ResourceFinder;
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
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ResourceRequestHandler {
    private final ResourceUserFinder resourceUserFinder;
    private final HawkPermissionEvaluator auth;
    private final ResourceStore resourceStore;
    private final ResourceServer resourceServer;
    private final ResourcePermissionSetter permissionSetter;
    private final AuthorizationProvider authorization;
    private final ResourceFinder resourceFinder;
    private final RealmModel realm;
    private final UserProvider userProvider;

    public Collection<UserResourcePermission> handleUsersOfResourceRequest(String resourceId) {
        auth.requireViewResourcePermissions();

        Resource resource = resourceStore.findById(resourceServer, resourceId);

        if (resource == null) {
            throw new NotFoundException("Resource not found");
        }

        return resourceUserFinder.getUsersOfResource(resource);
    }

    public Response handleSetUserPermissionsRequest(String userId, String resourceId, List<String> scopes) {
        auth.requireManageResourcePermissions();

        UserModel user = userProvider.getUserById(realm, userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }

        Resource resource = resourceStore.findById(resourceServer, resourceId);
        if (resource == null) {
            throw new NotFoundException("Resource not found");
        }

        permissionSetter.setPermissions(user, resource, scopes);

        return Response.noContent().build();
    }

    public Response handleGetResourcesRequest(
            List<String> ids,
            String sharedWith,
            String name,
            String uri,
            String owner,
            String type,
            Boolean exactName,
            Boolean idsOnly,
            Boolean sharedOnly,
            Integer firstResult,
            Integer maxResult
    ) {
        auth.requireViewResourcePermissions();
        Stream<Resource> resources = resourceFinder.findResources(ids, sharedWith, name, uri, owner, type, exactName, sharedOnly, firstResult, maxResult);

        if(idsOnly != null && idsOnly) {
            return Response.ok(resources.map(Resource::getId)).build();
        }

        return Response.ok(resources.map(r -> ModelToRepresentation.toRepresentation(r, resourceServer, authorization, true))).build();
    }
}
