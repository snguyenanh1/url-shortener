package io.github.snguyenanh1.urlshortener.dto.url;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Data
public class UrlUpdate {

    @NotNull(message = "Id cannot be null")
    private Long id;

    private String shortCode;

    @Future(message = "Expiration date must be in the future")
    private OffsetDateTime expiresAt;

}
