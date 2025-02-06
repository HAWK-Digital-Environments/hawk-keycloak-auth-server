package com.hawk.keycloak.users.lookup;

import com.hawk.keycloak.users.model.ClientIdSessionType;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.hawk.keycloak.users.model.ClientIdSessionType.SessionType.REGULAR;

@RequiredArgsConstructor
public class OnlineUserIdResolver {
    final private UserSessionProvider sessionProvider;
    final private RealmModel realm;

    /**
     * Find a list of user ids that currently have at least one active session
     * @param firstResult Offset from the beginning of the list for pagination
     * @param maxResults The number of results to return
     * @param userIdFilter An optional list of user ids to filter the results by
     * @return A stream of user id strings
     */
    public Stream<String> getOnlineUserIds(
            Integer firstResult,
            Integer maxResults,
            String[] userIdFilter
    ) {

        final Map<String, Long> clientSessionStats = sessionProvider.getActiveClientSessionStats(realm, false);
        Stream<ClientIdSessionType> sessionIdStream = clientSessionStats.keySet().stream()
                .map(i -> new ClientIdSessionType(i, REGULAR));

        // 10 minutes
        final int CONSIDER_OFFLINE_AFTER = 600;
        final int considerOfflineThreshold = (int) System.currentTimeMillis() - (CONSIDER_OFFLINE_AFTER * 1000);

        Stream<String> result = sessionIdStream.flatMap((clientIdSessionType) -> {
            ClientModel clientModel = realm.getClientById(clientIdSessionType.getClientId());
            if (clientModel == null) {
                // client has been removed in the meantime
                return Stream.empty();
            }
            return sessionProvider.getUserSessionsStream(realm, clientModel)
                    // Only include sessions that have been accessed in the last CONSIDER_OFFLINE_AFTER minutes
                    .filter(s -> s.getLastSessionRefresh() > considerOfflineThreshold)
                    .map(s -> s.getUser().getId());
        });

        if(userIdFilter != null) {
            List<String> userIdFilterList = List.of(userIdFilter);
            result = result.filter(userIdFilterList::contains);
        }

        return result.distinct().skip(firstResult).limit(maxResults);
    }
}







