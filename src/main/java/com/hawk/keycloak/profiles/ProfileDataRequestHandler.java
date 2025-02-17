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
import java.util.function.BiFunction;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ProfileDataRequestHandler {
    private final KeycloakSession session;
    private final HawkPermissionEvaluator auth;
    private final EventBuilder event;

    public AbstractUserRepresentation handleProfileRequest(UserModel user, ProfileMode mode) {
        auth.admin().users().requireView();
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        return getProfileForRead(user, mode).toRepresentation();
    }

    public Response handleProfilePutRequest(UserModel user, ProfileMode mode, UserRepresentation rep) {
        return handleProfileUpdateRequest(
                user,
                () -> getProfileForWrite(user, mode, rep),
                (profile, event) -> {
                    profile.update(new EventAuditingAttributeChangeListener(getProfileForRead(user, mode), event));
                    return null;
                });
    }

    public Response handleProfilePatchRequest(UserModel user, ProfileMode mode, UserRepresentation rep) {
        return handleProfileUpdateRequest(
                user,
                () -> getProfileForPartialWrite(user, mode, rep),
                (profile, event) -> {
                    profile.update(true, new EventAuditingAttributeChangeListener(getProfileForRead(user, mode), event));
                    return null;
                }
        );
    }

    private Response handleProfileUpdateRequest(
            UserModel user,
            Supplier<UserProfile> getProfile,
            BiFunction<UserProfile, EventBuilder, Void> callback) {
        auth.requireManageProfileData();
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        UserProfile profile = getProfile.get();
        event.event(EventType.UPDATE_PROFILE).detail(Details.CONTEXT, UserProfileContext.ACCOUNT.name());
        try {
            callback.apply(profile, event);
            event.success();
            return Response.noContent().build();
        } catch (ValidationException pve) {
            List<ErrorRepresentation> errors = new ArrayList<>();
            for (ValidationException.Error err : pve.getErrors()) {
                errors.add(new ErrorRepresentation(err.getAttribute(), err.getMessage(), validationErrorParamsToString(err.getMessageParameters(), profile.getAttributes())));
            }
            throw ErrorResponse.errors(errors, pve.getStatusCode(), false);
        } catch (ReadOnlyException e) {
            throw ErrorResponse.error(Messages.READ_ONLY_USER, Response.Status.BAD_REQUEST);
        }
    }

    private UserProfile getProfileForRead(UserModel user, ProfileMode mode) {
        UserProfileContext context = mode == ProfileMode.USER ? UserProfileContext.ACCOUNT : UserProfileContext.USER_API;
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
        return profileProvider.create(context, user);
    }

    private UserProfile getProfileForWrite(UserModel user, ProfileMode mode, UserRepresentation rep) {
        UserProfileContext context = mode == ProfileMode.USER ? UserProfileContext.ACCOUNT : UserProfileContext.USER_API;
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
        return profileProvider.create(context, rep.getRawAttributes(), user);
    }

    private UserProfile getProfileForPartialWrite(UserModel user, ProfileMode mode, UserRepresentation rep) {
        UserProfileContext context = mode == ProfileMode.USER ? UserProfileContext.ACCOUNT : UserProfileContext.USER_API;
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class);
        if (profileProvider instanceof PartialUpdateUserProfileProvider) {
            try {
                ((PartialUpdateUserProfileProvider) profileProvider).onlyWriteFieldsOf(rep);
                return profileProvider.create(context, rep.getRawAttributes(), user);
            } finally {
                ((PartialUpdateUserProfileProvider) profileProvider).onlyWriteFieldsOf(null);
            }
        }
        return profileProvider.create(context, rep.getRawAttributes(), user);
    }

    private String[] validationErrorParamsToString(Object[] messageParameters, Attributes userProfileAttributes) {
        if (messageParameters == null)
            return null;
        String[] ret = new String[messageParameters.length];
        int i = 0;
        for (Object p : messageParameters) {
            if (p != null) {
                //first parameter is user profile attribute name, we have to take Display Name for it
                if (i == 0) {
                    AttributeMetadata am = userProfileAttributes.getMetadata(p.toString());
                    if (am != null)
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
