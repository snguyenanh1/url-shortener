package io.github.snguyenanh1.urlshortener.dto.user;

import io.github.snguyenanh1.urlshortener.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserDetailChange {
    @Size(min = 4, max = 20, message = "Username length must be in between 4 and 20")
    private String username;

    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 2, max = 20, message = "Name length must be in between 2 and 20")
    private String name;

    @NotNull
    private UserRole role;
}
