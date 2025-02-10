package com.hawk.keycloak;

import lombok.RequiredArgsConstructor;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserRealmRoleMappingMapper;

import java.util.stream.Stream;

@RequiredArgsConstructor
public class ClientScopeRegistration {
    public final static String CLIENT_SCOPE_NAME = "hawk-client";

    final private KeycloakSessionFactory sessionFactory;

    public void register() {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
            Stream<RealmModel> realms = session.realms().getRealmsStream();
            realms.forEach(this::registerInRealm);
        });
    }

    public void registerInRealm(RealmModel realm) {

        ClientScopeModel scope = realm.getClientScopesStream()
                .filter(s -> s.getName().equals(CLIENT_SCOPE_NAME))
                .findFirst()
                .orElse(null);

        if (scope != null) {
            return;
        }

        // Create the scope
        scope = realm.addClientScope(CLIENT_SCOPE_NAME);
        scope.setDescription("Required claims for the hawk-client library");
        scope.setProtocol("openid-connect");
        scope.setIncludeInTokenScope(false);
        scope.setDisplayOnConsentScreen(false);

        // Realm Roles
        ProtocolMapperModel realmRoleMapper = UserRealmRoleMappingMapper.create(
                null,
                "realm roles",
                "hawk.roles.realm",
                false,
                false,
                false,
                true
        );
        realmRoleMapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        scope.addProtocolMapper(realmRoleMapper);

        // Client Roles
        ProtocolMapperModel clientRoleMapper = UserClientRoleMappingMapper.create(
                null,
                null,
                "client roles",
                "hawk.roles.client.${client_id}",
                false,
                false,
                false,
                true
        );
        clientRoleMapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        scope.addProtocolMapper(clientRoleMapper);

        // Groups
        ProtocolMapperModel groupMapper = GroupMembershipMapper.create(
                "groups",
                "hawk.groups",
                false,
                null,
                false,
                false,
                false
        );
        groupMapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        groupMapper.getConfig().put("full.path", "true");
        scope.addProtocolMapper(groupMapper);
    }
}
