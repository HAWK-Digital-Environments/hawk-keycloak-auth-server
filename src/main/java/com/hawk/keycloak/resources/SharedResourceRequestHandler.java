package com.hawk.keycloak.resources;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.resources.lookup.SharedResourceFinder;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class SharedResourceRequestHandler {
    private final SharedResourceFinder sharedResourceFinder;
    private final HawkPermissionEvaluator auth;
    private final KeycloakSession session;
    private final ResourceServer resourceServer;

    public Response handleSharedWithUserRequest(String userId, Integer first, Integer max) {
        auth.requireViewResourcePermissions();

        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if(user == null){
            throw new NotFoundException("User not found");
        }

        return queryResponse(this.sharedResourceFinder.getSharedWithUser(resourceServer, user, first, max), first, max);
    }

    public Response handleSharedByUserRequest(String userId, Integer first, Integer max) {
        auth.requireViewResourcePermissions();

        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if(user == null){
            throw new NotFoundException("User not found");
        }

        return queryResponse(this.sharedResourceFinder.getSharedByUser(resourceServer, user, first, max), first, max);
    }

    private Response queryResponse(Stream<String> stream, Integer first, Integer max) {
        List result = stream.collect(Collectors.toList());

        if (first != null && max != null) {
            int size = result.size();

            if (size > max) {
                result = result.subList(0, size - 1);
            }
        }

        return Response.ok().entity(result).build();
    }
}
