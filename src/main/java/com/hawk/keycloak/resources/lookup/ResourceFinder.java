package com.hawk.keycloak.resources.lookup;

import com.hawk.keycloak.util.ResultWindow;
import jakarta.ws.rs.BadRequestException;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceFinder {
    private final ResourceStore resourceStore;
    private final ResourceServer resourceServer;
    private final SharedResourceFinder sharedResourceFinder;
    private final RealmModel realm;
    private final UserProvider userProvider;

    public ResourceFinder(KeycloakSession session, ResourceStore resourceStore, ResourceServer resourceServer, SharedResourceFinder sharedResourceFinder) {
        this.resourceStore = resourceStore;
        this.resourceServer = resourceServer;
        this.sharedResourceFinder = sharedResourceFinder;
        this.realm = session.getContext().getRealm();
        this.userProvider = session.users();
    }

    public Stream<Resource> findResources(
            List<String> ids,
            String sharedWith,
            String name,
            String uri,
            String owner,
            String type,
            Boolean exactName,
            Boolean sharedOnly,
            Integer firstResult,
            Integer maxResults
    ) {
        boolean hasIdFilter = ids != null && !ids.isEmpty();
        boolean hasOwnerFilter = owner != null && !owner.trim().isEmpty();
        boolean hasNameFilter = name != null && !name.trim().isEmpty();
        boolean hasUriFilter = uri != null && !uri.trim().isEmpty();
        boolean hasTypeFilter = type != null && !type.trim().isEmpty();
        boolean hasBasicFiltersWithoutOwner = hasNameFilter || hasUriFilter || hasTypeFilter;
        boolean hasBasicFilters = hasBasicFiltersWithoutOwner || hasOwnerFilter;
        boolean hasOnlySharedFilter = sharedOnly != null && sharedOnly;

        String sharedBy = null;
        if (hasOnlySharedFilter) {
            if(!hasOwnerFilter){
                throw new BadRequestException("When requesting only shared resources, you must provide the owner filter");
            }

            // Inherit the owner of the document as the sharer
            sharedBy = owner;

            // We can look up users for "shared-by" (if only the owner and the shared filter is set) more efficiently
            if (!hasBasicFiltersWithoutOwner) {
                owner = null;
                hasBasicFilters = false;
            }
        }

        boolean hasSharedWithFilter = sharedWith != null;
        boolean hasSharedByFilter = sharedBy != null;

        Stream<Resource> stream;
        String usedStreamGenerator;

        if (hasIdFilter) {
            if (hasBasicFilters) {
                throw new BadRequestException("When requesting a set of ids, you can not define any of the basic filters (name, uri, owner, type)");
            }
            stream = getStreamByIds(ids);
            usedStreamGenerator = "ids";
        } else if (hasSharedWithFilter && !hasBasicFilters) {
            stream = getStreamBySharedWith(sharedWith);
            usedStreamGenerator = "sharedWith";
        } else if (hasSharedByFilter && !hasBasicFilters) {
            stream = getStreamBySharedBy(sharedBy);
            usedStreamGenerator = "sharedBy";
        } else {
            stream = getStreamByBasicFilters(name, uri, owner, type, exactName);
            usedStreamGenerator = "basic";
        }

        if (hasSharedByFilter && !usedStreamGenerator.equals("sharedBy")) {
            String finalSharedBy = sharedBy;
            stream = stream.filter(resource -> isSharedBy(resource, finalSharedBy));
        }

        if (hasSharedWithFilter && !usedStreamGenerator.equals("sharedWith")) {
            stream = stream.filter(resource -> isSharedWith(resource, sharedWith));
        }

        return ResultWindow.limitStream(stream, firstResult, maxResults);
    }

    protected Stream<Resource> getStreamByBasicFilters(String name, String uri, String owner, String type, Boolean exactName) {
        Map<Resource.FilterOption, String[]> search = new EnumMap<>(Resource.FilterOption.class);

        if (name != null && !name.trim().isEmpty()) {
            search.put(exactName != null && exactName ? Resource.FilterOption.EXACT_NAME : Resource.FilterOption.NAME, new String[]{name});
        }

        if (uri != null && !uri.trim().isEmpty()) {
            search.put(Resource.FilterOption.URI, new String[]{uri});
        }

        if (owner != null && !owner.trim().isEmpty()) {
            ClientModel clientModel = realm.getClientByClientId(owner);

            if (clientModel != null) {
                owner = clientModel.getId();
            } else {
                UserModel user = userProvider.getUserByUsername(realm, owner);

                if (user != null) {
                    owner = user.getId();
                }
            }

            search.put(Resource.FilterOption.OWNER, new String[]{owner});
        }

        if (type != null && !type.trim().isEmpty()) {
            search.put(Resource.FilterOption.TYPE, new String[]{type});
        }

        return getChunkedStreamIterator((first, max)
                -> resourceStore.find(resourceServer, search, first, max).stream());
    }

    protected Stream<Resource> getStreamByIds(List<String> ids) {
        return ids.stream().map(id -> resourceStore.findById(resourceServer, id)).distinct().filter(Objects::nonNull);
    }

    protected Stream<Resource> getStreamBySharedWith(String sharedWith) {
        UserModel sharedWithUser = userProvider.getUserById(realm, sharedWith);
        if (sharedWithUser == null) {
            return Stream.empty();
        }

        return getChunkedStreamIterator((first, max)
                -> sharedResourceFinder.getSharedWithUser(resourceServer, sharedWithUser, first, max)
                .map(resourceId -> resourceStore.findById(resourceServer, resourceId))
        );
    }

    protected Stream<Resource> getStreamBySharedBy(String sharedBy) {
        UserModel sharedByUser = userProvider.getUserById(realm, sharedBy);
        if (sharedByUser == null) {
            return Stream.empty();
        }

        return getChunkedStreamIterator((first, max) -> sharedResourceFinder.getSharedByUser(resourceServer, sharedByUser, first, max)
                .map(resourceId -> resourceStore.findById(resourceServer, resourceId))

        );
    }

    protected Boolean isSharedWith(Resource resource, String sharedWith) {
        return sharedResourceFinder.isSharedWithUser(userProvider.getUserById(realm, sharedWith), resource);
    }

    protected Boolean isSharedBy(Resource resource, String sharedBy) {
        return sharedResourceFinder.isSharedByUser(userProvider.getUserById(realm, sharedBy), resource);
    }

    protected Stream<Resource> getChunkedStreamIterator(
            BiFunction<Integer, Integer, Stream<Resource>> streamSupplier
    ) {
        final int CHUNK_SIZE = 100;

        return Stream.iterate(0, offset -> offset + CHUNK_SIZE)
                .map(offset -> streamSupplier.apply(offset, CHUNK_SIZE)
                        .collect(Collectors.toList()))
                .takeWhile(chunk -> !chunk.isEmpty())
                .flatMap(Collection::stream);
    }
}
