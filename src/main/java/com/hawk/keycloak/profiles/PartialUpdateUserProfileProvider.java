package com.hawk.keycloak.profiles;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;

import java.util.Map;

public class PartialUpdateUserProfileProvider extends DeclarativeUserProfileProvider {
    private UserRepresentation partialRep;

    public PartialUpdateUserProfileProvider(
            KeycloakSession session,
            PartialUpdateUserProfileProviderFactory factory
    ) {
        super(session, factory);
    }

    /**
     * Setting this will make the provider only write fields that are present in the provided representation.
     * This allows for partial updates of user profiles.
     * @param rep The given update request.
     */
    public void onlyWriteFieldsOf(UserRepresentation rep) {
        partialRep = rep;
    }

    /**
     * Overrides the default attribute creation, by defining all attributes that are not present in the partial representation as read-only.
     */
    @Override
    protected Attributes createAttributes(UserProfileContext context, Map<String, ?> attributes, UserModel user, UserProfileMetadata metadata) {
        if (partialRep == null) {
            return super.createAttributes(context, attributes, user, metadata);
        }

        UserProfileMetadata metadataClone = metadata.clone();
        metadataClone.getAttributes().forEach((value) -> {
            switch (value.getName()) {
                case "emailVerified":
                    if (partialRep.isEmailVerified() == null) {
                        value.addWriteCondition(context1 -> false);
                    }
                    break;
                case "username":
                    if (partialRep.getUsername() == null || partialRep.getUsername().isEmpty()) {
                        value.addWriteCondition(context1 -> false);
                    }
                    break;
                case "email":
                    if (partialRep.getEmail() == null || partialRep.getEmail().isEmpty()) {
                        value.addWriteCondition(context1 -> false);
                    }
                    break;
                case "firstName":
                    if (partialRep.getFirstName() == null || partialRep.getFirstName().isEmpty()) {
                        value.addWriteCondition(context1 -> false);
                    }
                    break;
                case "lastName":
                    if (partialRep.getLastName() == null || partialRep.getLastName().isEmpty()) {
                        value.addWriteCondition(context1 -> false);
                    }
                    break;
                default:
                    if (!partialRep.getAttributes().containsKey(value.getName())) {
                        value.addWriteCondition(context1 -> false);
                    }
                    break;
            }

        });

        return super.createAttributes(context, attributes, user, metadataClone);
    }
}
