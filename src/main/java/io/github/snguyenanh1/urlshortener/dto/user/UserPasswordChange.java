package io.github.snguyenanh1.urlshortener.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * The type User password change.
 */
@Data
public class UserPasswordChange {
    @Size(min = 4, message = "Password length must be larger than 4")
    private String oldPassword;

    @Size(min = 4, message = "Password length must be larger than 4")
    private String newPassword;

    @Size(min = 4, message = "Password length must be larger than 4")
    private String confirmPassword;

}
