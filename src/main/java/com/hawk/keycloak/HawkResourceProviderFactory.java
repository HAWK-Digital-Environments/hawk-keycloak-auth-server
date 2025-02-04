package com.hawk.keycloak;

import com.hawk.keycloak.auth.RoleRegistration;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class HawkResourceProviderFactory implements RealmResourceProviderFactory {

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new HawkResourceProvider(
                new RequestHandlerFactory(session),
                session
        );
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(
                () -> new RoleRegistration(keycloakSessionFactory).register(),
                1, // delay duration
                java.util.concurrent.TimeUnit.SECONDS // delay unit
        );
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "hawk";
    }
}
