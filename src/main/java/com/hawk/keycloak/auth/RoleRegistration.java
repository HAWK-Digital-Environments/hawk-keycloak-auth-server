package com.hawk.keycloak.auth;

import lombok.RequiredArgsConstructor;
import org.keycloak.Config;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;

import java.util.stream.Stream;

@RequiredArgsConstructor
public class RoleRegistration {
    // The list of roles to automatically register when they do not yet exist in the system
    final private String[] HAWK_ROLES = new String[]{
            HawkPermissionEvaluator.ROLE_VIEW_CACHE_BUSTER,
            HawkPermissionEvaluator.ROLE_VIEW_ROLES,
            HawkPermissionEvaluator.ROLE_VIEW_PROFILE_STRUCTURE,
            HawkPermissionEvaluator.ROLE_VIEW_RESOURCE_PERMISSIONS,

            // Manage roles should NOT be added to the "HAWK-CLIENT" role automatically.
            // This should be decided by the admin.
            HawkPermissionEvaluator.ROLE_MANAGE_PROFILE_STRUCTURE,
            HawkPermissionEvaluator.ROLE_MANAGE_PROFILE_DATA,
            HawkPermissionEvaluator.ROLE_MANAGE_RESOURCE_PERMISSIONS
    };

    // The list of roles that should be automatically be added to the "HAWK-CLIENT" role
    final private String[] CLIENT_ROLES = new String[]{
            HawkPermissionEvaluator.ROLE_VIEW_CACHE_BUSTER,
            HawkPermissionEvaluator.ROLE_VIEW_ROLES,
            HawkPermissionEvaluator.ROLE_VIEW_PROFILE_STRUCTURE,
            HawkPermissionEvaluator.ROLE_VIEW_RESOURCE_PERMISSIONS,
            AdminRoles.VIEW_AUTHORIZATION,
            AdminRoles.QUERY_USERS,
            AdminRoles.QUERY_GROUPS,
            AdminRoles.VIEW_USERS,
            AccountRoles.VIEW_GROUPS,
            AccountRoles.VIEW_PROFILE
    };

    final private KeycloakSessionFactory sessionFactory;

    public void register() {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, (KeycloakSession session) -> {
            Stream<RealmModel> realms = session.realms().getRealmsStream();
            RealmManager manager = new RealmManager(session);
            realms.forEach(realm -> {
                final boolean isMasterRealm = realm.getName().equals(Config.getAdminRealm());

                if(isMasterRealm){
                    addHawkRolesToMasterRealm(realm);
                } else {
                    ClientModel client = realm.getClientByClientId(manager.getRealmAdminClientId(realm));
                    addHawkRolesToNormalRealm(client);
                    addClientRoleToRealm(client);
                }
            });
        });
    }

    private void addHawkRolesToMasterRealm(RealmModel master) {
        RoleModel admin = master.getRole(AdminRoles.ADMIN);
        addRoles(master.getMasterAdminClient(), admin, HAWK_ROLES);
    }

    private void addHawkRolesToNormalRealm(ClientModel client) {
        RoleModel admin = client.getRole(AdminRoles.REALM_ADMIN);
        addRoles(client, admin, HAWK_ROLES);
    }

    private void addClientRoleToRealm(ClientModel client) {
        if(client.getRole(HawkPermissionEvaluator.ROLE_HAWK_CLIENT) != null){
            // Role exists already...
            return;
        }

        // Add client role
        RoleModel role = client.addRole(HawkPermissionEvaluator.ROLE_HAWK_CLIENT);
        role.setDescription("${role_" + HawkPermissionEvaluator.ROLE_HAWK_CLIENT + "}");

        // Assign roles if they exist
        ClientModel accountClient = client.getRealm().getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        for (String roleName : CLIENT_ROLES) {
            RoleModel roleModel = client.getRole(roleName);

            // If the "realm-management" account does not know the role, it might be an "account" role.
            if(roleModel == null && accountClient != null){
                roleModel = accountClient.getRole(roleName);
            }

            if (roleModel != null) {
                role.addCompositeRole(roleModel);
            }
        }
    }

    private void addRoles(ClientModel client, RoleModel parent, String[] names) {
        for (String name : names) {
            if(client.getRole(name) != null){
                // Role exists already...
                continue;
            }

            RoleModel role = client.addRole(name);
            role.setDescription("${role_" + name + "}");
            parent.addCompositeRole(role);
        }
    }
}
