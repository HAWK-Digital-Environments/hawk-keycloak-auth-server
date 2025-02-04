package com.hawk.keycloak.users;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.users.lookup.OnlineUserIdResolver;
import com.hawk.keycloak.users.lookup.UserInfoGenerator;
import com.hawk.keycloak.users.lookup.UserResultProcessor;
import com.hawk.keycloak.users.lookup.UserSearcher;
import com.hawk.keycloak.users.lookup.query.FindAny;
import com.hawk.keycloak.users.lookup.query.FindByAttributes;
import com.hawk.keycloak.users.lookup.query.FindByIds;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.SearchQueryUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@Slf4j
@RequiredArgsConstructor
public class UsersRequestHandler {
    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;
    private final UserInfoGenerator userInfoGenerator;

    public Stream<Map<String, Object>> getUserList(
            String search,
            String attributes,
            List<String> ids,
            Boolean onlineOnly,
            Boolean idsOnly,
            Integer firstResult,
            Integer maxResults
    ) {
        onlineOnly = onlineOnly != null ? onlineOnly : false;
        idsOnly = idsOnly != null ? idsOnly : false;
        firstResult = firstResult != null ? firstResult : 0;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        auth.admin().users().requireQuery();
        if(!idsOnly){
            auth.admin().users().requireView();
        }

        RealmModel realm = session.getContext().getRealm();
        UserSearcher searcher = new UserSearcher(session, auth);
        OnlineUserIdResolver onlineUserIdResolver = new OnlineUserIdResolver(session, realm);
        UserResultProcessor resultProcessor = new UserResultProcessor(idsOnly, realm, auth.admin().users(), session, userInfoGenerator);

        if (ids != null && !ids.isEmpty()) {
            if(search != null){
                throw new BadRequestException("When requesting a set of ids, you can not define an additional search parameter");
            }
            if(attributes != null){
                throw new BadRequestException("When requesting a set of ids, you can not define an additional attributes parameter");
            }

           return resultProcessor.process(
                   new FindByIds(
                           ids.toArray(new String[0]),
                           onlineOnly,
                           realm,
                           firstResult,
                           maxResults
                   ).execute(searcher, onlineUserIdResolver)
           );
        }

        if(search != null || attributes != null) {
            Map<String, String> attributeMap = new HashMap<>(attributes == null
                    ? Collections.emptyMap()
                    : SearchQueryUtils.getFields(attributes));

            if (search != null) {
                attributeMap.put(UserModel.SEARCH, search.trim());
            }

            return resultProcessor.process(
                    new FindByAttributes(
                            attributeMap,
                            onlineOnly,
                            realm,
                            firstResult,
                            maxResults
                    ).execute(searcher, onlineUserIdResolver)
            );
        }

        return resultProcessor.process(
                new FindAny(
                        onlineOnly,
                        realm,
                        firstResult,
                        maxResults
                ).execute(searcher, onlineUserIdResolver)
        );
    }

    public Integer getUserCount(
            String search,
            String attributes,
            Boolean onlineOnly
        ) {
        return (int) getUserList(
                search,
                attributes,
                null,
                onlineOnly,
                true,
                0,
                99999
        ).count();
    }
}
