package io.github.snguyenanh1.urlshortener.dto.user;

import io.github.snguyenanh1.urlshortener.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserCreate {

    @Size(min = 4, max = 30, message = "Username length must be in between 4 and 30")
    private String username;

    @Size(min = 4, message = "Password length must be larger than 4")
    private String password;

    @Size(min = 2, max = 30, message = "Name length must be in between 2 and 30")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull
    private UserRole role;
}
