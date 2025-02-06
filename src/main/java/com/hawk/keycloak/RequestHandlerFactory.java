package com.hawk.keycloak;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.cacheBuster.CacheBusterRequestHandler;
import com.hawk.keycloak.profiles.ProfileDataRequestHandler;
import com.hawk.keycloak.profiles.ProfileStructureRequestHandler;
import com.hawk.keycloak.resources.ResourceRequestHandler;
import com.hawk.keycloak.resources.lookup.ResourceFinder;
import com.hawk.keycloak.resources.lookup.ResourceUserFinder;
import com.hawk.keycloak.resources.lookup.SharedResourceFinder;
import com.hawk.keycloak.resources.service.ResourcePermissionSetter;
import com.hawk.keycloak.roles.RolesRequestHandler;
import com.hawk.keycloak.users.lookup.UserFinder;
import com.hawk.keycloak.users.lookup.UserInfoGenerator;
import com.hawk.keycloak.users.UsersRequestHandler;
import com.hawk.keycloak.util.ConnectionInfoRequestHandler;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resources.admin.AdminEventBuilder;

@RequiredArgsConstructor
public class RequestHandlerFactory {
    final private KeycloakSession session;

    public CacheBusterRequestHandler cacheBusterRequestHandler(HawkPermissionEvaluator auth) {
        return new CacheBusterRequestHandler(session, auth);
    }

    public UsersRequestHandler usersRequestHandler(HawkPermissionEvaluator auth) {
        return new UsersRequestHandler(
                new UserFinder(
                        session.users(),
                        session.sessions(),
                        session.getContext().getRealm()
                ),
                session,
                auth,
                new UserInfoGenerator(
                        session,
                        session.getContext().getClient(),
                        session.getContext().getUri(),
                        session.getContext().getConnection()
                )
        );
    }

    public ConnectionInfoRequestHandler connectionInfoRequestHandler(HawkPermissionEvaluator auth) {
        return new ConnectionInfoRequestHandler(auth, session);
    }

    public RolesRequestHandler rolesRequestHandler(HawkPermissionEvaluator auth) {
        return new RolesRequestHandler(session, auth);
    }

    public ProfileStructureRequestHandler profileStructureRequestHandler(HawkPermissionEvaluator auth) {
        return new ProfileStructureRequestHandler(session, auth, adminEventBuilder(auth));
    }

    public ProfileDataRequestHandler profileDataRequestHandler(HawkPermissionEvaluator auth) {
        return new ProfileDataRequestHandler(session, auth, eventBuilder());
    }

    public ResourceRequestHandler resourceRequestHandler(HawkPermissionEvaluator auth) {
        AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);
        ResourceServer resourceServer = authorizationProvider.getStoreFactory().getResourceServerStore().findByClient(
                session.getContext().getClient()
        );
        PermissionTicketStore ticketStore = authorizationProvider.getStoreFactory().getPermissionTicketStore();
        ResourceStore resourceStore = authorizationProvider.getStoreFactory().getResourceStore();
        return new ResourceRequestHandler(
                new ResourceUserFinder(ticketStore),
                auth,
                resourceStore,
                resourceServer,
                new ResourcePermissionSetter(
                        ticketStore,
                        resourceServer,
                        authorizationProvider.getStoreFactory().getScopeStore(),
                        adminEventBuilder(auth),
                        session.getContext().getUri()
                ),
                authorizationProvider,
                new ResourceFinder(
                        session,
                        resourceStore,
                        authorizationProvider.getStoreFactory().getResourceServerStore().findByClient(
                                session.getContext().getClient()
                        ),
                        new SharedResourceFinder(ticketStore)
                ),
                session.getContext().getRealm(),
                session.users()
        );
    }

    private AdminEventBuilder adminEventBuilder(HawkPermissionEvaluator auth) {
        return new AdminEventBuilder(
                session.getContext().getRealm(),
                auth.admin().adminAuth(),
                session,
                session.getContext().getConnection()
        );
    }

    private EventBuilder eventBuilder() {
        return new EventBuilder(
                session.getContext().getRealm(),
                session,
                session.getContext().getConnection()
        );
    }
}
