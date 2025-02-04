package com.hawk.keycloak.util.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ConnectionInfo {
    private final String keycloakVersion;
    private final String extensionVersion;
    private final String clientId;
    private final String clientUuid;
}
