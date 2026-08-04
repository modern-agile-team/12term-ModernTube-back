package com.moderntube.moderntubo_backend.model.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moderntube.moderntubo_backend.model.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private String email;
    private String username;
    private Set<Role> roles;
    private Long id;
    private boolean active;
    private String name;

    public UserResponse(String username, String email, Set<Role> roles) {
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public UserResponse(String username, String email, Set<Role> roles, Long id, boolean active, String name) {
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.id = id;
        this.active = active;
        this.name = name;
    }
}
