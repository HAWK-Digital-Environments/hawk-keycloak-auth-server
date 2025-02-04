package com.hawk.keycloak.profiles;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * Mostly a carbon copy of {@link  org.keycloak.services.resources.admin.UserProfileResource} with custom permissions
 */
@RequiredArgsConstructor
public class ProfileStructureRequestHandler {
    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public UPConfig handleGetStructure() {
        auth.requireViewProfileStructure();
        return session.getProvider(UserProfileProvider.class).getConfiguration();
    }

    public Response handleUpdateStructure(UPConfig config) {
        auth.requireManageProfileStructure();
        UserProfileProvider t = session.getProvider(UserProfileProvider.class);

        try {
            t.setConfiguration(config);
        } catch (ComponentValidationException e) {
            //show validation result containing details about error
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

        adminEvent.operation(OperationType.UPDATE)
                .resourcePath(session.getContext().getUri())
                .representation(config)
                .success();

        return Response.ok(t.getConfiguration()).type(MediaType.APPLICATION_JSON).build();
    }
}
