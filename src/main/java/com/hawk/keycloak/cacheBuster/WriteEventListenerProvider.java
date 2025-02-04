package com.hawk.keycloak.cacheBuster;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class WriteEventListenerProvider implements EventListenerProvider {
    private final RealmProvider realmProvider;

    @Override
    public void onEvent(Event event) {
        if (isRelevantUserEvent(event.getType())) {
            updateCacheBusterTimestamp(event.getRealmId());
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {
        if (isRelevantAdminEvent(adminEvent.getOperationType())) {
            updateCacheBusterTimestamp(adminEvent.getRealmId());
        }
    }

    @Override
    public void close() {
    }

    private boolean isRelevantUserEvent(EventType eventType) {
        return Set.of(
                EventType.UPDATE_PROFILE,
                EventType.CLIENT_INITIATED_ACCOUNT_LINKING,
                EventType.DELETE_ACCOUNT,
                EventType.UNREGISTER_NODE,
                EventType.REGISTER_NODE,
                EventType.REVOKE_GRANT
                // Add more event types as needed
        ).contains(eventType);
    }

    private boolean isRelevantAdminEvent(OperationType operationType) {
        // Define which admin events should trigger the cache buster update
        return operationType == OperationType.CREATE
                || operationType == OperationType.UPDATE
                || operationType == OperationType.DELETE;
    }

    private void updateCacheBusterTimestamp(String realmId) {
        RealmModel realm = realmProvider.getRealm(realmId);
        if (realm == null) {
            log.error("Failed to update cache buster for realm {}\n", realmId);
            return;
        }

        long currentTimestamp = System.currentTimeMillis();
        String oldValue = realm.getAttribute("custom.hawk.cache-buster");
        realm.setAttribute("custom.hawk.cache-buster", String.valueOf(currentTimestamp));

        log.info(
                "Updated cache buster for realm {} from {} to {}\n",
                realmId,
                oldValue != null ? oldValue : "null",
                currentTimestamp
        );
    }
}
