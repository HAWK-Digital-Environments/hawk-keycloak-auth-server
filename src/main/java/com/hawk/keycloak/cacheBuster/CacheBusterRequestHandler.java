package com.hawk.keycloak.cacheBuster;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;


@RequiredArgsConstructor
public class CacheBusterRequestHandler {

    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;

    public Response getCacheBuster() {
        auth.requireViewCacheBuster();
        String attributeValue = session.getContext().getRealm().getAttribute("custom.hawk.cache-buster");
        return Response.ok(attributeValue).build();
    }
}
