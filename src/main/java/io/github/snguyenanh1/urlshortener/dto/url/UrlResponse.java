package io.github.snguyenanh1.urlshortener.dto.url;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlResponse {


    private String originalUrl;

    private String shortCode;

    private String shortUrl;

    private OffsetDateTime createdAt;

    private OffsetDateTime expiresAt;

}
