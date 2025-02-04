package com.hawk.keycloak.users.lookup.query;

import com.hawk.keycloak.users.lookup.OnlineUserIdResolver;
import com.hawk.keycloak.users.lookup.UserSearcher;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class FindByAttributes {
    final private Map<String, String> attributes;
    final private boolean onlineOnly;
    final private RealmModel realm;
    final private int firstResult;
    final private int maxResults;

    public Stream<UserModel> execute(UserSearcher searcher, OnlineUserIdResolver onlineUserIdResolver) {
        if (!onlineOnly) {
            return searcher.searchByAttributes(attributes, realm, firstResult, maxResults);
        }

        return findOnlineUsersUntilMaxIsReached(searcher, onlineUserIdResolver);
    }

    private Stream<UserModel> findOnlineUsersUntilMaxIsReached(UserSearcher searcher, OnlineUserIdResolver onlineUserIdResolver) {
        List<String> onlineUserIdsList = onlineUserIdResolver
                .getOnlineUserIds(firstResult, maxResults, null)
                .toList();

        if (onlineUserIdsList.isEmpty()) {
            return Stream.empty();
        }

        List<UserModel> resultList = new ArrayList<>();

        int offset = 0;

        while (true) {
            AtomicBoolean streamHasAtLeastOneElement = new AtomicBoolean(false);

            searcher
                    .searchByAttributes(attributes, realm, offset, maxResults)
                    .forEach(u -> {
                        streamHasAtLeastOneElement.set(true);
                        if (onlineUserIdsList.contains(u.getId())) {
                            resultList.add(u);
                        }
                    });

            // Break if either the number of results is reached or the stream did not return any more results.
            if (resultList.size() >= maxResults || !streamHasAtLeastOneElement.get()) {
                break;
            }

            offset += maxResults;
        }

        return resultList.stream();
    }
}
