package com.hawk.keycloak.resources.lookup;

import com.hawk.keycloak.util.ResultWindow;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.models.UserModel;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class SharedResourceFinder {
    private final PermissionTicketStore ticketStore;

    public Stream<String> getSharedWithUser(ResourceServer resourceServer, UserModel user, Integer first, Integer max) {
        return toPermissions(
                resourceServer,
                ticketStore.findGrantedResources(
                        user.getId(),
                        null,
                        ResultWindow.limitFirst(first),
                        ResultWindow.limitMax(max)
                ),
                resource -> ticketStore.findGranted(
                        resource.getResourceServer(),
                        resource.getName(),
                        user.getId()
                )
        );
    }

    public Stream<String> getSharedByUser(ResourceServer resourceServer, UserModel user, Integer first, Integer max) {
        return toPermissions(
                resourceServer,
                ticketStore.findGrantedOwnerResources(
                        user.getId(),
                        ResultWindow.limitFirst(first),
                        ResultWindow.limitMax(max)
                ),
                resource -> {
                    Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);

                    filters.put(PermissionTicket.FilterOption.OWNER, user.getId());
                    filters.put(PermissionTicket.FilterOption.GRANTED, Boolean.TRUE.toString());
                    filters.put(PermissionTicket.FilterOption.RESOURCE_ID, resource.getId());

                    return ticketStore.find(resource.getResourceServer(), filters, null, null);
                }
        );
    }

    public boolean isSharedByUser(UserModel user, Resource resource) {
        if (resource == null || user == null) {
            return false;
        }

        return ticketStore.findByResource(resource.getResourceServer(), resource)
                .stream()
                .anyMatch(t -> t.isGranted() && t.getOwner().equals(user.getId()));
    }

    public boolean isSharedWithUser(UserModel user, Resource resource) {
        if (resource == null || user == null) {
            return false;
        }

        return !ticketStore.findGranted(resource.getResourceServer(), resource.getName(), user.getId()).isEmpty();
    }

    private Stream<String> toPermissions(ResourceServer resourceServer, List<Resource> resources, Function<Resource, List<PermissionTicket>> ticketFinder) {
        return resources.stream()
                .flatMap(resource -> ticketFinder.apply(resource).stream())
                .filter(ticket -> ticket.getResourceServer().equals(resourceServer))
                .map(ticket -> ticket.getResource().getId())
                .distinct();
    }
}
