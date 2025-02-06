package com.hawk.keycloak.users.lookup;

import com.hawk.keycloak.users.lookup.query.FindAny;
import com.hawk.keycloak.users.lookup.query.FindByAttributes;
import com.hawk.keycloak.users.lookup.query.FindByIds;
import com.hawk.keycloak.util.ResultWindow;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.*;
import org.keycloak.utils.SearchQueryUtils;

import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class UserFinder {
    private final UserProvider userProvider;
    private final UserSessionProvider sessionProvider;
    private final RealmModel realm;

    public Stream<UserModel> findByFilters(
            String search,
            String attributes,
            List<String> ids,
            Boolean onlineOnly,
            Integer firstResult,
            Integer maxResults
    ) {
        onlineOnly = onlineOnly != null ? onlineOnly : false;
        firstResult = ResultWindow.limitFirst(firstResult);
        maxResults = ResultWindow.limitMax(maxResults);

        OnlineUserIdResolver onlineUserIdResolver = new OnlineUserIdResolver(sessionProvider, realm);

        if (ids != null && !ids.isEmpty()) {
            if(search != null){
                throw new BadRequestException("When requesting a set of ids, you can not define an additional search parameter");
            }
            if(attributes != null){
                throw new BadRequestException("When requesting a set of ids, you can not define an additional attributes parameter");
            }

            return new FindByIds(
                    ids.toArray(new String[0]),
                    onlineOnly,
                    realm,
                    firstResult,
                    maxResults
            ).execute(this, onlineUserIdResolver);
        }

        if(search != null || attributes != null) {
            Map<String, String> attributeMap = new HashMap<>(attributes == null
                    ? Collections.emptyMap()
                    : SearchQueryUtils.getFields(attributes));

            if (search != null) {
                attributeMap.put(UserModel.SEARCH, search.trim());
            }

            return new FindByAttributes(
                    attributeMap,
                    onlineOnly,
                    realm,
                    firstResult,
                    maxResults
            ).execute(this, onlineUserIdResolver);
        }

        return new FindAny(
                onlineOnly,
                realm,
                firstResult,
                maxResults
        ).execute(this, onlineUserIdResolver);
    }

    public Stream<UserModel> findByIds(
            String[] userIds,
            RealmModel realm,
            Integer firstResult,
            Integer maxResults
    ) {
        return ResultWindow.limitStream(
                Arrays.stream(userIds)
                        .map(userId -> userProvider.getUserById(realm, userId))
                        .filter(Objects::nonNull),
                firstResult,
                maxResults
        );
    }

    public Stream<UserModel> findByAttributes(
            Map<String, String> attributes,
            RealmModel realm,
            Integer firstResult,
            Integer maxResults
    ) {
        return userProvider.searchForUserStream(realm, attributes, firstResult, maxResults);
    }
}
