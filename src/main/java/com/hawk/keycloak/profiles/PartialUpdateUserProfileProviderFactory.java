package com.hawk.keycloak.profiles;

import org.keycloak.models.KeycloakSession;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;

public class PartialUpdateUserProfileProviderFactory extends DeclarativeUserProfileProviderFactory {
    @Override
    public int order() {
        return 10;
    }

    @Override
    public DeclarativeUserProfileProvider create(KeycloakSession session) {
        return new PartialUpdateUserProfileProvider(session, this);
    }
}
