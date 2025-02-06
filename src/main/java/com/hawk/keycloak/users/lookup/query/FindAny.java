package com.hawk.keycloak.users.lookup.query;

import com.hawk.keycloak.users.lookup.OnlineUserIdResolver;
import com.hawk.keycloak.users.lookup.UserFinder;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class FindAny {
    final private boolean onlineOnly;
    final private RealmModel realm;
    final private int firstResult;
    final private int maxResults;

    public Stream<UserModel> execute(UserFinder finder, OnlineUserIdResolver onlineUserIdResolver) {
        if (!onlineOnly) {
            return finder.findByAttributes(new HashMap<>(), realm, firstResult, maxResults);
        }

        String[] userIds = onlineUserIdResolver
                .getOnlineUserIds(firstResult, maxResults, null)
                .toArray(String[]::new);

        if(userIds.length == 0){
            return Stream.empty();
        }

        return finder.findByIds(userIds, realm, firstResult, maxResults);
    }
}
