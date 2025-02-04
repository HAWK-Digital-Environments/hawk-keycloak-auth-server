package com.hawk.keycloak.resources.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserResourcePermissionsRequest {
    private List<String> scopes;
}
