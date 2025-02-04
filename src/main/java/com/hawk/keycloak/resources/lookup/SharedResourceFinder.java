package com.hawk.keycloak.resources.lookup;

import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class SharedResourceFinder {
    private final PermissionTicketStore ticketStore;

    public Stream<String> getSharedWithUser(ResourceServer resourceServer, UserModel user, Integer first, Integer max) {
        first = first != null ? first : 0;
        max = max != null ? max : Constants.DEFAULT_MAX_RESULTS;

        return toPermissions(
                resourceServer,
                ticketStore.findGrantedResources(user.getId(), null, first, max),
                resource -> ticketStore.findGranted(resource.getResourceServer(), resource.getName(), user.getId())
        ).stream();
    }

    public Stream<String> getSharedByUser(ResourceServer resourceServer, UserModel user, Integer first, Integer max) {
        first = first != null ? first : 0;
        max = max != null ? max : Constants.DEFAULT_MAX_RESULTS;

        return toPermissions(
                resourceServer,
                ticketStore.findGrantedOwnerResources(user.getId(), first, max),
                resource -> {
                    Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

                    filters.put(PermissionTicket.FilterOption.OWNER, user.getId());
                    filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
                    filters.put(PermissionTicket.FilterOption.RESOURCE_ID, resource.getId());

                    return ticketStore.find(resource.getResourceServer(), filters, null, null);
                }
        ).stream();
    }

    private Collection<String> toPermissions(ResourceServer resourceServer, List<Resource> resources, Function<Resource, List<PermissionTicket>> ticketFinder) {
        Collection<String> permissions = new ArrayList<>();

        for (org.keycloak.authorization.model.Resource resource : resources) {
            for (PermissionTicket ticket : ticketFinder.apply(resource)) {
                if(ticket.getResourceServer().equals(resourceServer)){
                    permissions.add(ticket.getResource().getId());
                    break;
                }
            }
        }

        return permissions;
    }
}
