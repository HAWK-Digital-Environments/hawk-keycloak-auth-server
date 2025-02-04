package com.hawk.keycloak.users.lookup;

import jakarta.ws.rs.core.UriInfo;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.Map;
import java.util.function.BiFunction;

public class UserInfoGenerator {
    private final KeycloakSession session;
    private final ClientModel client;
    private final UriInfo uriInfo;
    private final ClientConnection clientConnection;

    public UserInfoGenerator(KeycloakSession session){
        this.session = session;
        this.client = session.getContext().getClient();
        this.clientConnection = session.getContext().getConnection();
        this.uriInfo = session.getContext().getUri();
    }

    public Map<String, Object> getUserinfo(RealmModel realm, UserModel user) {
        return sessionAware(user, realm, null, (userSession, clientSessionCtx) -> {
            AccessToken userInfo = new AccessToken();
            TokenManager tokenManager = new TokenManager();
            userInfo = tokenManager.transformUserInfoAccessToken(session, userInfo, userSession, clientSessionCtx);

            return tokenManager.generateUserInfoClaims(userInfo, user);
        });
    }

    private<R> R sessionAware(UserModel user, RealmModel realm, String scopeParam, BiFunction<UserSessionModel, ClientSessionContext,R> function) {
        AuthenticationSessionModel authSession = null;
        AuthenticationSessionManager authSessionManager = new AuthenticationSessionManager(session);

        try {
            RootAuthenticationSessionModel rootAuthSession = authSessionManager.createAuthenticationSession(realm, false);
            authSession = rootAuthSession.createAuthenticationSession(client);

            authSession.setAuthenticatedUser(user);
            authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
            authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scopeParam);

            UserSessionModel userSession = new UserSessionManager(session).createUserSession(authSession.getParentSession().getId(), realm, user, user.getUsername(),
                    clientConnection.getRemoteAddr(), "example-auth", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);

            AuthenticationManager.setClientScopesInSession(session, authSession);
            ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);

            return function.apply(userSession, clientSessionCtx);

        } finally {
            if (authSession != null) {
                authSessionManager.removeAuthenticationSession(realm, authSession, false);
            }
        }
    }
}
