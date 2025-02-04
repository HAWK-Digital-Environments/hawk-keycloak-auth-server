package com.hawk.keycloak.resources.lookup;

import com.hawk.keycloak.resources.model.UserResourcePermission;
import com.hawk.keycloak.resources.model.UserResourcePermissionList;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PermissionTicketStore;

import java.util.*;

@RequiredArgsConstructor
public class ResourceUserFinder {
    protected final PermissionTicketStore ticketStore;

    public Collection<UserResourcePermission> getUsersOfResource(org.keycloak.authorization.model.Resource resource) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

        final ResourceServer resourceServer = resource.getResourceServer();

        filters.put(PermissionTicket.FilterOption.OWNER, resource.getOwner());
        filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
        filters.put(PermissionTicket.FilterOption.RESOURCE_ID, resource.getId());

        Collection<UserResourcePermissionList> resources = toPermissions(ticketStore.find(resourceServer, filters, null, null));
        Collection<UserResourcePermission> permissions = Collections.EMPTY_LIST;

        if (!resources.isEmpty()) {
            permissions = resources.iterator().next().getPermissions();
        }

        return permissions;
    }

    private Collection<UserResourcePermissionList> toPermissions(List<PermissionTicket> tickets) {
        Map<String, UserResourcePermissionList> permissions = new HashMap<>();

        for (PermissionTicket ticket : tickets) {
            UserResourcePermissionList resource = permissions
                    .computeIfAbsent(ticket.getResource().getId(), s -> new UserResourcePermissionList());

            UserResourcePermission user = resource.getPermission(ticket.getRequester());

            if (user == null) {
                resource.addPermission(ticket.getRequester(), user = new UserResourcePermission(ticket.getRequester()));
            }

            user.addScope(ticket.getScope().getName());
        }

        return permissions.values();
    }
}
