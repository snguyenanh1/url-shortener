package io.github.snguyenanh1.urlshortener.dto.user;

import io.github.snguyenanh1.urlshortener.enums.UserRole;
import lombok.Builder;
import lombok.Data;

/**
 * The type User response.
 */
@Data
@Builder
public class UserResponse {

    private Long id;

    private String username;

    private String name;

    private String email;

    private UserRole role;

}
