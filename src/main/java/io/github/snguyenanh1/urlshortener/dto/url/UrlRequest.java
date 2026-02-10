package io.github.snguyenanh1.urlshortener.dto.url;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UrlRequest {

    @NotBlank(message = "URL must not be blank")
    @URL(message = "Invalid URL")
    private String originalUrl;

}
