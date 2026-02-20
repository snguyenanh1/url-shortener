package io.github.snguyenanh1.urlshortener.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class UserSignUp {
    @Size(min = 4, max = 20, message = "Username length must be in between 4 and 20")
    private String username;

    @Size(min = 4, message = "Password length must be larger than 4")
    private String password;

    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 4, message = "Password length must be larger than 4")
    private String confirmPassword;

    @Size(min = 2, max = 20, message = "Name length must be in between 2 and 20")
    private String name;
}
