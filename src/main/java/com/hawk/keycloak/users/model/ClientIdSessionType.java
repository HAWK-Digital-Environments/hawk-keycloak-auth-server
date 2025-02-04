package com.hawk.keycloak.users.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Carbon copy of keycloak/rest/admin-ui-ext/src/main/java/org/keycloak/admin/ui/rest/model/ClientIdSessionType.java
 */
@Getter
@RequiredArgsConstructor
public class ClientIdSessionType {
    public enum SessionType {
        ALL, REGULAR, OFFLINE
    }

    private final String clientId;
    private final SessionType type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientIdSessionType clientIdSessionType = (ClientIdSessionType) o;
        return Objects.equals(clientId, clientIdSessionType.clientId) && type == clientIdSessionType.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, type);
    }
}