package com.hawk.keycloak;

import com.hawk.keycloak.auth.HawkPermissionEvaluator;
import com.hawk.keycloak.resources.model.UserResourcePermission;
import com.hawk.keycloak.resources.model.UserResourcePermissionsRequest;
import com.hawk.keycloak.util.model.ConnectionInfo;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.resources.admin.AdminAuth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ApiRoot extends org.keycloak.services.resources.admin.AdminRoot {
    final private RequestHandlerFactory requestHandlerFactory;

    public ApiRoot(RequestHandlerFactory requestHandlerFactory, KeycloakSession session) {
        this.requestHandlerFactory = requestHandlerFactory;
        this.session = session;
        this.tokenManager = new TokenManager();
    }

    @GET
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Stream<Map<String, Object>> getUsers(
            @Parameter(description = "A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and \"foo\" for exact search.") @QueryParam("search") String search,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("attributes") String attributes,
            @Parameter(description = "List of comma separated user ids, in the format 'id1,id2'") @QueryParam("ids") String ids,
            @Parameter(description = "If true, only users with an active session (in any client of the realm) will be returned") @QueryParam("onlineOnly") Boolean onlineOnly,
            @Parameter(description = "Return only user ids") @QueryParam("idsOnly") Boolean idsOnly,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults
    ) {
        return requestHandlerFactory
                .usersRequestHandler(authenticate())
                .getUserList(
                        search,
                        attributes,
                        commaListToCollection(ids),
                        onlineOnly,
                        idsOnly,
                        firstResult,
                        maxResults
                );
    }

    @GET
    @Path("users/count")
    @Produces(MediaType.TEXT_PLAIN)
    @NoCache
    public String getUsersCount(
            @Parameter(description = "A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and \"foo\" for exact search.") @QueryParam("search") String search,
            @Parameter(description = "A query to search for custom attributes, in the format 'key1:value2 key2:value2'") @QueryParam("attributes") String attributes,
            @Parameter(description = "If true, only users with an active session (in any client of the realm) will be counted") @QueryParam("onlineOnly") Boolean onlineOnly
    ) {
        return String.valueOf(
                requestHandlerFactory
                        .usersRequestHandler(authenticate())
                        .getUserCount(
                                search,
                                attributes,
                                onlineOnly
                        )
        );
    }

    @GET
    @Path("resources")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<ResourceRepresentation> getResources(
            @Parameter(description = "List of comma separated resource ids, in the format 'id1,id2'") @QueryParam("ids") String ids,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults
    ) {
        return requestHandlerFactory
                .resourceRequestHandler(authenticate())
                .handleGetResourcesRequest(
                        commaListToCollection(ids),
                        firstResult,
                        maxResults
                );
    }

    @GET
    @Path("resources/{resource}/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<UserResourcePermission> getResourceUsers(
            @Parameter(description = "The id of the resource to find the users of") @PathParam("resource") String resourceId
    ) {
        return requestHandlerFactory
                .resourceRequestHandler(authenticate())
                .handleUsersOfResourceRequest(resourceId);
    }

    @PUT
    @Path("resources/{resource}/users/{user}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JSON)
    public Response setResourceUserPermissions(
            @Parameter(description = "The id of the resource to set the permissions for") @PathParam("resource") String resourceId,
            @Parameter(description = "The id of the user to set the permissions for") @PathParam("user") String userId,
            UserResourcePermissionsRequest request
    ) {
        return requestHandlerFactory
                .resourceRequestHandler(authenticate())
                .handleSetUserPermissionsRequest(
                        userId,
                        resourceId,
                        request.getScopes()
                );
    }

    @GET
    @Path("resources/shared-with/{user}")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JSON)
    public Response getResourcesSharedWithUser(
            @Parameter(description = "The id of the user to find the resources shared with") @PathParam("user") String userId,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults
    ) {
        return requestHandlerFactory
                .sharedResourceRequestHandler(authenticate())
                .handleSharedWithUserRequest(userId, firstResult, maxResults);
    }

    @GET
    @Path("resources/shared-by/{user}")
    @Produces(org.keycloak.utils.MediaType.APPLICATION_JSON)
    public Response getResourcesSharedByUser(
            @Parameter(description = "The id of the user to find the resources shared by") @PathParam("user") String userId,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults
    ) {
        return requestHandlerFactory
                .sharedResourceRequestHandler(authenticate())
                .handleSharedByUserRequest(userId, firstResult, maxResults);
    }

    @GET
    @Path("roles")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<RoleRepresentation> getRoles(
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults
    ) {
        return requestHandlerFactory
                .rolesRequestHandler(authenticate())
                .handleRolesRequest(
                        firstResult,
                        maxResults
                );
    }

    @GET
    @Path("roles/{role}/members")
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<Map<String, String>> getRoleMembers(
            @Parameter(description = "The id of the role to find the members of") @PathParam("role") String roleId,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults
    ) {

        return requestHandlerFactory
                .rolesRequestHandler(authenticate())
                .handleRoleMembersRequest(
                        roleId,
                        firstResult,
                        maxResults
                );
    }

    @GET
    @Path("profile/structure")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Get the configuration for the user profile")
    public UPConfig getProfileStructure() {
        return requestHandlerFactory.profileStructureRequestHandler(authenticate()).handleGetStructure();
    }

    @PUT
    @Path("profile/structure")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Set the configuration for the user profile")
    @APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UPConfig.class)))
    public Response putProfileStructure(UPConfig config) {
        return requestHandlerFactory.profileStructureRequestHandler(authenticate()).handleUpdateStructure(config);
    }

    @PUT
    @Path("profile/{user}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserProfile(
            @Parameter(description = "The id of the user to update the profile for") @PathParam("user") String userId,
            UserRepresentation rep
    ) {
        return requestHandlerFactory.profileDataRequestHandler(authenticate())
                .handleProfileUpdateRequest(
                        session.users().getUserById(session.getContext().getRealm(), userId),
                        rep
                );
    }

    @GET
    @Path("cache-buster")
    @Produces(MediaType.TEXT_PLAIN)
    @NoCache
    public Response getCacheBuster() {
        return requestHandlerFactory.cacheBusterRequestHandler(authenticate()).getCacheBuster();
    }

    @GET
    @Path("connection-info")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectionInfo getConnectionInfo() {
        return requestHandlerFactory.connectionInfoRequestHandler(authenticate()).handleRequest();
    }

    private List<String> commaListToCollection(String commaList) {
        return commaList == null ? List.of() : Stream.of(commaList.split(",")).map(String::trim).distinct().toList();
    }

    private HawkPermissionEvaluator authenticate() {
        AdminAuth auth = authenticateRealmAdminRequest(session.getContext().getHttpRequest().getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        return new HawkPermissionEvaluator(auth.getRealm(), auth.getToken(), auth.getUser(), auth.getClient(), session);
    }
}
