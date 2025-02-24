# HAWK - Auth Server - Keycloak Extension

This keycloak extension is a companion for the [HAWK Auth client](https://github.com/HAWK-Digital-Environments/hawk-auth-client),
which aims to create a simple way of working with keycloak as an authentication and authorization backend for microservices. 
While keycloak is a really powerful tool, the REST api and the fine grained access control on it's own resources
can be a bit cumbersome to work with. This extension provides multiple new endpoints to make it easier
and more efficient. It also provides a (currently rather simple) logic to invalidate client caches when
data of the realm changes.

## Installation

To install the extension download the latest release from the [releases page](https://github.com/HAWK-Digital-Environments/hawk-keycloak-auth-server/releases).

### Configure

After the installation of the extension you need to restart the keycloak server.
With the restart the extension will create a bunch of new roles you need to assign to your clients.
To make your life easier, we have created a new role `hawk-client` that combines all the roles needed to access the new endpoints.
The installer will also automatically add a new group of client scopes to your realm called `hawk-client` as well, that you can assign to your clients.

1. Create a new client, or use an existing one
2. Open the client and navigate to the "Service account roles" tab
3. Assign the `(realm-management) hawk-client` role to the client
   - If you want to do management operations (like updating the profile structure or managing resource permissions),
     you need to assign the corresponding roles to your client: `hawk-manage-profile-structure`, `hawk-manage-profile-data`, `hawk-manage-resource-permissions`.
4. Open the "Client scopes" tab
5. Add the `hawk-client` scope using the "Default" option.
6. Open the "Realm settings" in your main menu
7. Navigate to the "Events" tab and add the `hawk-cache-buster` event listener.

## What's in the box?

### New routes
#### GET Users
`/realms/{realm}/hawk/users`

This route is more or less a drop-in replacement for the built-in keycloak user search endpoint (`/admin/realms/{realm}/users`),
however it has some notable differences:

* The response data mirrors the data structure of the `/realms/{realm}/protocol/openid-connect/token/introspect` endpoint (without the token related fields "exp", "iat", etc).
* You can request multiple users at once by providing a list of user ids in the query parameter `ids`. The response will be a list of users in the same order as the ids.
* You can request ONLY the user ids by providing the query parameter `idsOnly=true`. The response will be a list of user ids.
* You can request ONLY users that are considered "online" by providing the query parameter `onlineOnly=true`. The response will be a list of users that have a session with activity in the last 10 minutes.

Supported query parameters:
* **search** - A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and "foo" for exact search.
* **attributes** - A query to search for custom attributes, in the format `key1:value2 key2:value2`
* **ids** - A list of user ids to search for, in the format `id1,id2`. If provided, the search and attributes parameters CAN NOT be used.
* **onlineOnly** - If true, only users with an active session (in any client of the realm) will be returned
* **idsOnly** - If true, only the user ids will be returned
* **first** - The first result to return (0-based)
* **max** - The maximum number of results to return (default 100)

Required roles: `query-users` and `view-users` (the latter is required if "idsOnly" is not set to true)

#### GET User Count
`/realms/{realm}/hawk/users/count`

This route is a simple count of the users in the realm. Works similar to `/admin/realms/{realm}/users/count`, but allows specific filtering for online users.

Supported query parameters:
* **onlineOnly** - If true, only users with an active session (in any client of the realm) will be counted
* **search** - A String contained in username, first or last name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and "foo" for exact search.
* **attributes** - A query to search for custom attributes, in the format `key1:value2 key2:value2`

Required roles: `query-users`

#### GET Client Resources
`/realms/{realm}/hawk/resources`

Returns a list of authorization resource definitions for the client requesting the endpoint. This is a custom implementation of: `/realms/{realm}/authz/protection/resource_set`
that can be used to filter resources based on their ids.

Supported query parameters:
* **ids** - A list of resource ids to search for, in the format `id1,id2`. If provided the filters "name", "owner", "type" and "uri" can not be used. (Exception owner + sharedOnly, this is allowed)
* **name** - Allows for filtering the resources by their name, by default a partial search is done, set 'exactName' for exact matching
* **uri** - Allows filtering the resources by the uri
* **owner** - The uuid of a user will only return resources owned by the user
* **sharedWith** - Allows filtering the resources to all resources shared with the user with the given uuid
* **sharedOnly** - If true, only resources shared with the user will be returned. Enabling this flag MUST be accompanied by the "owner" filter, that is the user id which shared the resources.
* **idsOnly** - If set, only the ids of the resources are returned
* **exactName** - If set only resources that match the name-filter exactly will be returned
* **type** - Allows for filtering for types of resources
* **first** - The first result to return (0-based)
* **max** - The maximum number of results to return (default 100)

Required roles: `hawk-view-resource-permissions` or `hawk-manage-resource-permissions` or both.

#### GET Client Resource Users
`/realms/{realm}/hawk/resources/{resourceId}/users`

Returns a list of users that have been granted permissions (does not include the owner) for the specified resource.
The response contains both the user id and the allowed scopes.

Required roles: `hawk-view-resource-permissions` or `hawk-manage-resource-permissions` or both.

#### PUT Allow Resource to User
`/realms/{realm}/hawk/resources/{resourceId}/users/{userId}`

Grants the user the provided scopes for the specified resource.
If the list of scopes is empty, the user will be granted all scopes for the resource (if the user does not have any scope yet),
or remove all scopes from the user (if the user has any scope). This basically an endpoint that allows the "share" 
functionality provided by the "account" frontend but with custom roles.

Required roles: `hawk-manage-resource-permissions`

Expected body:
```json
{
  "scopes": ["scope1", "scope2"]
}
```

#### GET Roles
`/realms/{realm}/hawk/roles`

This endpoint has the same output as the `/admin/realms/{realm}/roles` endpoint, but has some differences:

* Built-In roles (e.g., `offline_access`, `uma_authorization`) are not included in the response.
* The response contains `Realm roles` and `Client roles` in the same list.

Supported query parameters:
* **first** - The first result to return (0-based)
* **max** - The maximum number of results to return (default 100)

Required roles: `hawk-view-roles`

#### GET Role Members
`/realms/{realm}/hawk/roles/{roleName}/members`

Returns a list of user ids that have the specified role.
The response is a list of user ids.

Supported query parameters:
* **first** - The first result to return (0-based)
* **max** - The maximum number of results to return (default 100)

Required roles: `hawk-view-roles` AND `view-users`

#### GET Profile structure
`/realms/{realm}/hawk/profile/structure`

Returns the structure of the user profile in the realm. This is a custom endpoint that
allows clients to get the profile structure without having read access to the whole realm.

The response is a [UPConfig](https://www.keycloak.org/docs-api/latest/rest-api/index.html#UPConfig) representation.

Required roles: `hawk-view-profile-structure`

#### PUT Profile structure
`/realms/{realm}/hawk/profile/structure`

Allows your client to update the profile structure of the realm. This is a custom endpoint that
allows clients to update the profile structure without having write access to the whole realm.

The body of the request should be a [UPConfig](https://www.keycloak.org/docs-api/latest/rest-api/index.html#UPConfig) representation.

Required roles: `hawk-manage-profile-structure`

#### GET User Profile Data
`/realms/{realm}/hawk/profile/{userId}`

Returns the profile data of a user. This is a custom endpoint that allows the client to choose if the
profile should be returned as the user itself or as an admin. The userProfileMetadata is ALWAYS returned,
otherwise the endpoint behaves exactly like: `/admin/realms/{realm}/users/{userId}`

Supported query parameters:
* **mode** - user|admin - If set to "admin" the update will be done as an admin, otherwise as the user itself

Required roles: `view-users`

#### PUT User Profile Data
`/realms/{realm}/hawk/profile/{userId}`

Allows your client to update the profile data of a user. This is a custom endpoint that
allows clients to update the profile without having global write access to the user or 
using impersonation to update the user.

The body of the request should be a [UserRepresentation](https://www.keycloak.org/docs-api/latest/rest-api/index.html#UserRepresentation)

Supported query parameters:
* **mode** - user|admin - If set to "admin" the update will be done as an admin, otherwise as the user itself

Required roles: `hawk-manage-profile-data`

#### PATCH User Profile Data
`/realms/{realm}/hawk/profile/{userId}`

This endpoint works similar to the `PUT` variant, but also accepts partial updates.
Only the fields that are provided in the request body will be validated and updated.

The body of the request should be a [UserRepresentation](https://www.keycloak.org/docs-api/latest/rest-api/index.html#UserRepresentation)

Supported query parameters:
* **mode** - user|admin - If set to "admin" the update will be done as an admin, otherwise as the user itself

Required roles: `hawk-manage-profile-data`

#### GET Cache Buster
`/realms/{realm}/hawk/cache-buster`

This endpoint returns a single timestamp value. The timestamp is updated whenever a write operation
is performed on the realm. This can be used by clients to invalidate their cache of realm data.

**Beware of dragons:** The current approach is not the most efficient one, as it invalidates the cache of the whole realm
on each write event. This is a simple approach that works for small realms, but it may not be the best
solution for large realms. We are open to suggestions and PRs to improve this extension.

Required roles: `hawk-view-cache-buster`

#### GET Connection Info
`/realms/{realm}/hawk/connection-info`

This endpoint returns information about the connection to the keycloak server required
for a smooth client experience. Note, to use this endpoint the requesting client MUST have a service account.

The response contains the following fields:
* keycloakVersion - The version of the keycloak server (Used for compatibility checks on the client side)
* extensionVersion - The version of the hawk keycloak extension
* clientId - The client id of the client that requested the endpoint
* clientUuid - The uuid of the client that requested the endpoint
* clientServiceAccountUuid - The uuid of the service account of the client that requested the endpoint

Required roles `hawk-client`

### New roles

The extension introduces new roles that can be used to control access to the new endpoints.
All roles are created on the `realm-management` client of all realms (except the master realm).
The roles are automatically installed on startup of the extension.

- `hawk-view-cache-buster` - Allows the client to request the cache buster timestamp
- `hawk-view-roles` - Allows the client to request the roles of the realm and clients
- `hawk-manage-profile-structure` - Allows the client to update the profile structure of the realm
- `hawk-view-profile-structure` - Allows the client to request the profile structure of the realm
- `hawk-manage-profile-data` - Allows the client to update the profile data of a user
- `hawk-view-resource-permissions` - Allows the client to request the resources of itself and the permissions of the resources
- `hawk-manage-resource-permissions` - Allows the client to manage the permissions of the resources

There is also a new convience role `hawk-client` that combines the following
roles to create a read-only client:

- `hawk-client`
  - (account) `view-groups`
  - (account) `view-profile`
  - (realm-management) `view-users`
  - (realm-management) `query-users`
  - (realm-management) `view-authorization`
  - (realm-management) `query-groups`
  - (realm-management) `hawk-view-roles`
  - (realm-management) `hawk-view-cache-buster`
  - (realm-management) `hawk-view-profile-structure`

## Building

To build the extension you may run the following command:
```bash
./build.sh
```

It will generate the `target/cache-buster-extension.jar` file.

##  Postcardware
You're free to use this package, but if it makes it to your production environment we highly appreciate you sending us a postcard from your hometown, mentioning which of our package(s) you are using.

```
HAWK Fakultät Gestaltung
Interaction Design Lab
Renatastraße 11
31134 Hildesheim
```

Thank you :D