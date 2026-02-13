package io.github.snguyenanh1.urlshortener.dto.url;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Data
public class UrlRequest {

    @NotBlank(message = "URL must not be blank")
    @URL(message = "Invalid URL")
    private String url;

    @Size(min = 4, max = 10, message = "Short code length must be in between 4 and 10")
    private String shortCode;

    @Future(message = "Expiration date must be in the future")
    private OffsetDateTime expiresAt;

}
