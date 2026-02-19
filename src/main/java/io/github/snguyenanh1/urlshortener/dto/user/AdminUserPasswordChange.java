package io.github.snguyenanh1.urlshortener.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserPasswordChange {
    @Size(min = 4, max = 20, message = "New password length must be in between 4 and 20")
    private String newPassword;

    @Size(min = 4, max = 20, message = "New password length must be in between 4 and 20")
    private String confirmPassword;
}
