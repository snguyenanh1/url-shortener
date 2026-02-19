package io.github.snguyenanh1.urlshortener.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * The type User detail change.
 */
@Data
public class UserDetailChange {
    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 2, max = 20, message = "Name length must be in between 2 and 20")
    private String name;
}
