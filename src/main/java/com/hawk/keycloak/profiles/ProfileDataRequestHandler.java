package com.hawk.keycloak.profiles;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.messages.Messages;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.userprofile.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ProfileDataRequestHandler {
    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;
    private final EventBuilder event;

    public AbstractUserRepresentation handleProfileRequest(UserModel user, ProfileMode mode) {
        auth.admin().users().requireView();

        if(user == null){
            throw new NotFoundException("User not found");
        }

        return getProfile(user, mode, null).toRepresentation();
    }

    public Response handleProfileUpdateRequest(UserModel user, ProfileMode mode, UserRepresentation rep) {
        auth.requireManageProfileData();

        if(user == null){
            throw new NotFoundException("User not found");
        }

        event.event(EventType.UPDATE_PROFILE).detail(Details.CONTEXT, UserProfileContext.ACCOUNT.name());

        UserProfile profile = getProfile(user, mode, rep);

        try {
            profile.update(new EventAuditingAttributeChangeListener(profile, event));

            event.success();

            return Response.noContent().build();
        } catch (ValidationException pve) {
            List<ErrorRepresentation> errors = new ArrayList<>();
            for(ValidationException.Error err: pve.getErrors()) {
                errors.add(new ErrorRepresentation(err.getAttribute(), err.getMessage(), validationErrorParamsToString(err.getMessageParameters(), profile.getAttributes())));
            }
            throw ErrorResponse.errors(errors, pve.getStatusCode(), false);
        } catch (ReadOnlyException e) {
            throw ErrorResponse.error(Messages.READ_ONLY_USER, Response.Status.BAD_REQUEST);
        }
    }

    private UserProfile getProfile(UserModel user, ProfileMode mode, UserRepresentation rep) {
        UserProfileContext context = mode == ProfileMode.USER ? UserProfileContext.ACCOUNT : UserProfileContext.USER_API;
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);

        if(rep == null){
            return profileProvider.create(context, user);
        }

        return profileProvider.create(context, rep.getRawAttributes(), user);
    }

    private String[] validationErrorParamsToString(Object[] messageParameters, Attributes userProfileAttributes) {
        if(messageParameters == null)
            return null;
        String[] ret = new String[messageParameters.length];
        int i = 0;
        for(Object p: messageParameters) {
            if(p != null) {
                //first parameter is user profile attribute name, we have to take Display Name for it
                if(i==0) {
                    AttributeMetadata am = userProfileAttributes.getMetadata(p.toString());
                    if(am != null)
                        ret[i++] = am.getAttributeDisplayName();
                    else
                        ret[i++] = p.toString();
                } else {
                    ret[i++] = p.toString();
                }
            } else {
                i++;
            }
        }
        return ret;
    }

}
