package io.github.snguyenanh1.urlshortener.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserPasswordChange {
    @Size(min = 4, message = "Password length must be larger than 4")
    private String newPassword;

    @Size(min = 4, message = "Password length must be larger than 4")
    private String confirmPassword;
}
