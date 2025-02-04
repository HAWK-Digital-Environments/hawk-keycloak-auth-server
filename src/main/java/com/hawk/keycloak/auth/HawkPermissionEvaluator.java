package com.hawk.keycloak.auth;

import jakarta.ws.rs.NotAuthorizedException;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

public class HawkPermissionEvaluator extends AdminAuth {
    public static final String ROLE_HAWK_CLIENT = "hawk-client";
    public static final String ROLE_VIEW_CACHE_BUSTER = "hawk-view-cache-buster";
    public static final String ROLE_VIEW_ROLES = "hawk-view-roles";
    public static final String ROLE_MANAGE_PROFILE_STRUCTURE = "hawk-manage-profile-structure";
    public static final String ROLE_VIEW_PROFILE_STRUCTURE = "hawk-view-profile-structure";
    public static final String ROLE_MANAGE_PROFILE_DATA = "hawk-manage-profile-data";
    public static final String ROLE_VIEW_RESOURCE_PERMISSIONS = "hawk-view-resource-permissions";
    public static final String ROLE_MANAGE_RESOURCE_PERMISSIONS = "hawk-manage-resource-permissions";

    private final AdminPermissionEvaluator admin;
    private final KeycloakSession session;
    private ClientModel realmAdminClient;

    public HawkPermissionEvaluator(RealmModel realm, AccessToken token, UserModel user, ClientModel client, KeycloakSession session) {
        super(realm, token, user, client);
        this.admin = AdminPermissions.evaluator(session, realm, this);
        this.session = session;
    }

    public AdminPermissionEvaluator admin(){
        return this.admin;
    }

    public void requireViewCacheBuster() {
        if(!hasAppRole(getRealmAdminApp(), ROLE_VIEW_CACHE_BUSTER)){
            throw new NotAuthorizedException(ROLE_VIEW_CACHE_BUSTER);
        }
    }

    public void requireHawkClientRole() {
        if(!hasAppRole(getRealmAdminApp(), ROLE_HAWK_CLIENT)){
            throw new NotAuthorizedException(ROLE_HAWK_CLIENT);
        }
    }

    public void requireViewRoles() {
        if(!hasAppRole(getRealmAdminApp(), ROLE_VIEW_ROLES)){
            throw new NotAuthorizedException(ROLE_VIEW_ROLES);
        }
    }

    public void requireViewProfileStructure() {
        if (!hasAppRole(getRealmAdminApp(), ROLE_VIEW_PROFILE_STRUCTURE)
                && !hasAppRole(getRealmAdminApp(), ROLE_MANAGE_PROFILE_STRUCTURE)) {
            throw new NotAuthorizedException(ROLE_VIEW_PROFILE_STRUCTURE);
        }
    }

    public void requireManageProfileStructure() {
        if (!hasAppRole(getRealmAdminApp(), ROLE_MANAGE_PROFILE_STRUCTURE)) {
            throw new NotAuthorizedException(ROLE_MANAGE_PROFILE_STRUCTURE);
        }
    }

    public void requireManageProfileData() {
        if (!hasAppRole(getRealmAdminApp(), ROLE_MANAGE_PROFILE_DATA)) {
            throw new NotAuthorizedException(ROLE_MANAGE_PROFILE_DATA);
        }
    }

    public void requireViewResourcePermissions() {
        if (!hasAppRole(getRealmAdminApp(), ROLE_VIEW_RESOURCE_PERMISSIONS)
                && !hasAppRole(getRealmAdminApp(), ROLE_MANAGE_RESOURCE_PERMISSIONS)) {
            throw new NotAuthorizedException(ROLE_VIEW_RESOURCE_PERMISSIONS);

        }
    }

    public void requireManageResourcePermissions() {
        if (!hasAppRole(getRealmAdminApp(), ROLE_MANAGE_RESOURCE_PERMISSIONS)) {
            throw new NotAuthorizedException(ROLE_MANAGE_RESOURCE_PERMISSIONS);
        }
    }

    private ClientModel getRealmAdminApp() {
        if (realmAdminClient == null) {
            RealmManager manager = new RealmManager(session);
            RealmModel realm = getRealm();
            realmAdminClient = realm.getClientByClientId(manager.getRealmAdminClientId(realm));
        }
        return realmAdminClient;
    }
}
