package com.hawk.keycloak;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

@RequiredArgsConstructor
public class HawkResourceProvider implements RealmResourceProvider {

    final private RequestHandlerFactory requestHandlerFactory;
    final private KeycloakSession session;

    @Override
    public Object getResource() {
        return new ApiRoot(requestHandlerFactory, this.session);
    }

    @Override
    public void close() {
    }
}
