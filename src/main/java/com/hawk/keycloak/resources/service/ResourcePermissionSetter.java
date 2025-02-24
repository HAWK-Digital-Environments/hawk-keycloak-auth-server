package com.hawk.keycloak.resources.service;

import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import java.util.*;

@RequiredArgsConstructor
public class ResourcePermissionSetter {
    private final PermissionTicketStore ticketStore;
    private final ResourceServer resourceServer;
    private final ScopeStore scopeStore;
    private final AdminEventBuilder adminEvent;
    private final KeycloakUriInfo uri;

    public void setPermissions(
            UserModel user,
            Resource resource,
            List<String> scopes
    ) {
        if (scopes == null) {
            scopes = List.of();
        }

        if(user.getId().equals(resource.getOwner())){
            throw new BadRequestException("Owner cannot have permissions on their own resource");
        }

        List<String> knownScopesOfResource = resource.getScopes().stream().map(Scope::getName).toList();

        for (String scope : scopes) {
            if(!knownScopesOfResource.contains(scope)){
                throw new BadRequestException("The scope \"" + scope + "\" is not allowed for the resource");
            }
        }

        List<PermissionTicket> tickets = findTickets(resource, user);

        boolean triggerEvent = false;

        if (tickets.isEmpty()) {
            // No tickets exist and there are no scopes -> grant all requested scopes
            scopes.forEach(scope -> grantPermission(resource, user, scope));
            triggerEvent = !scopes.isEmpty();
        } else {
            // Create a copy of the scopes to allow modification
            ArrayList<String> _scopes = new ArrayList<>(scopes);
            Iterator<String> scopesIterator = _scopes.iterator();

            while (scopesIterator.hasNext()) {
                String scopeName = scopesIterator.next();
                org.keycloak.authorization.model.Scope scope = getScope(scopeName, resourceServer);

                if(scope == null){
                    throw new BadRequestException("The scope \"" + scopeName + "\" does not exist");
                }

                Iterator<PermissionTicket> ticketIterator = tickets.iterator();

                while (ticketIterator.hasNext()) {
                    PermissionTicket ticket = ticketIterator.next();

                    if(ticket.getScope() == null){
                        continue;
                    }

                    if (scope.getId().equals(ticket.getScope().getId())) {
                        if (!ticket.isGranted()) {
                            ticket.setGrantedTimestamp(System.currentTimeMillis());
                        }
                        // permission exists, remove from the list to avoid deletion
                        ticketIterator.remove();
                        // scope already granted, remove from the list to avoid creating it again
                        scopesIterator.remove();
                    }
                }
            }

            // only create permissions for the scopes that don't have a token
            for (String scope : _scopes) {
                grantPermission(resource, user, scope);
                triggerEvent = true;
            }

            // remove all tickets that are not within the requested permissions
            for (PermissionTicket ticket : tickets) {
                ticketStore.delete(ticket.getId());
                triggerEvent = true;
            }
        }

        if(triggerEvent){
            adminEvent.operation(OperationType.UPDATE)
                    .resourcePath(uri)
                    .representation(Map.of("userId", user.getId(), "resourceId", resource.getId(), "scopes", scopes))
                    .success();
        }
    }

    private List<PermissionTicket> findTickets(Resource resource, UserModel user) {
        Map<PermissionTicket.FilterOption, String> filters = new EnumMap<>(PermissionTicket.FilterOption.class);
        filters.put(PermissionTicket.FilterOption.RESOURCE_ID, resource.getId());
        filters.put(PermissionTicket.FilterOption.REQUESTER, user.getId());
        return ticketStore.find(resourceServer, filters, null, null);
    }

    private void grantPermission(Resource resource, UserModel user, String scopeId) {
        org.keycloak.authorization.model.Scope scope = getScope(scopeId, resourceServer);
        PermissionTicket ticket = ticketStore.create(resourceServer, resource, scope, user.getId());
        ticket.setGrantedTimestamp(Calendar.getInstance().getTimeInMillis());
    }

    private org.keycloak.authorization.model.Scope getScope(String scopeId, ResourceServer resourceServer) {
        org.keycloak.authorization.model.Scope scope = scopeStore.findByName(resourceServer, scopeId);

        if (scope == null) {
            scope = scopeStore.findById(resourceServer, scopeId);
        }

        return scope;
    }
}
