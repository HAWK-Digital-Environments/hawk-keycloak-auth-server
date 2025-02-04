package com.hawk.keycloak.util;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.util.model.ConnectionInfo;
import lombok.RequiredArgsConstructor;
import org.keycloak.common.Version;
import org.keycloak.models.KeycloakSession;

@RequiredArgsConstructor
public class ConnectionInfoRequestHandler {
    final private HawkPermissionEvaluator auth;
    final private KeycloakSession session;

    public ConnectionInfo handleRequest() {
        auth.requireHawkClientRole();

        return new ConnectionInfo(
                Version.VERSION,
                "1.0.0",
                session.getContext().getClient().getClientId(),
                session.getContext().getClient().getId()
        );
    }
}
