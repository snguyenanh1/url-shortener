package io.github.snguyenanh1.urlshortener.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * The type User sign in.
 */
@Data
public class UserSignIn {
    @Size(min = 4, max = 30, message = "Username or email length must be in between 4 and 30")
    private String usernameOrEmail;

    @Size(min = 4, message = "Password length must be larger than 4")
    private String password;
}
