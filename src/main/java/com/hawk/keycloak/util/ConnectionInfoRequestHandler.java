package com.hawk.keycloak.util;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.util.model.ConnectionInfo;
import lombok.RequiredArgsConstructor;
import org.keycloak.common.Version;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

@RequiredArgsConstructor
public class ConnectionInfoRequestHandler {
    final private HawkPermissionEvaluator auth;
    final private KeycloakSession session;

    public ConnectionInfo handleRequest() {
        auth.requireHawkClientRole();

        ClientModel client = session.getContext().getClient();

        if(!client.isServiceAccountsEnabled()){
            throw new RuntimeException("Service accounts are not enabled for this client, but this is required for the clients to work well");
        }

        return new ConnectionInfo(
                Version.VERSION,
                VersionInfo.getPackageVersion(),
                client.getClientId(),
                client.getId(),
                session.users().getServiceAccount(client).getId()
        );
    }
}
