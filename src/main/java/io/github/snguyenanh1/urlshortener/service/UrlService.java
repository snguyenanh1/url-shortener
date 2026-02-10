package io.github.snguyenanh1.urlshortener.service;

import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlResponse;
import io.github.snguyenanh1.urlshortener.entity.Url;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class UrlService {
    // TODO: set appropriate base url
    private final static String baseUrl = "https://abc.xyz/";

    // TODO: other CRUD operations

    public UrlResponse createShortUrl(UrlRequest urlRequest) {
        Url url = mapToUrlFromUrlRequest(urlRequest);

        if (url.getShortCode() == null) {
            String shortCode = generateShortCode(urlRequest.getOriginalUrl());
            url.setShortCode(shortCode);
            // TODO: check for hash collision in DB, if there is -> hash Url + predefined String until resolved
        }

        // TODO: save to DB

        return mapToUrlResponseFromUrl(url);

    }

    private Url mapToUrlFromUrlRequest(UrlRequest urlRequest) {
        Url.UrlBuilder builder = Url.builder();
        builder.originalUrl(urlRequest.getOriginalUrl());
        OffsetDateTime now = OffsetDateTime.now();
        builder.createdAt(now);
        builder.expiresAt(now.plusDays(7));
        // TODO: custom short code and expiry date
        return builder.build();
    }

    private UrlResponse mapToUrlResponseFromUrl(Url url) {
        UrlResponse.UrlResponseBuilder builder = UrlResponse.builder();
        builder.originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt());
        return builder.build();
    }

    private String generateShortCode(String originalUrl) {
       try {
           MessageDigest md = MessageDigest.getInstance("MD5");
           md.update(originalUrl.getBytes());
           md.update(String.valueOf(System.nanoTime()).getBytes());
           byte[] digest = md.digest();

           return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 8);

       } catch (NoSuchAlgorithmException e) {
           throw new RuntimeException("Hash algorithm error", e);
       }
    }

}
