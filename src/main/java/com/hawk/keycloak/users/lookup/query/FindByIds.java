package com.hawk.keycloak.users.lookup.query;

import com.hawk.keycloak.users.lookup.OnlineUserIdResolver;
import com.hawk.keycloak.users.lookup.UserFinder;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.stream.Stream;

@RequiredArgsConstructor
public class FindByIds {
    final private String[] ids;
    final private boolean onlineOnly;
    final private RealmModel realm;
    final private int firstResult;
    final private int maxResults;

    public Stream<UserModel> execute(UserFinder finder, OnlineUserIdResolver onlineUserIdResolver) {
        String[] userIds = ids;
        if (onlineOnly) {
            userIds = onlineUserIdResolver
                    .getOnlineUserIds(firstResult, maxResults, userIds)
                    .toArray(String[]::new);
        }

        return finder.findByIds(userIds, realm, firstResult, maxResults);
    }
}
