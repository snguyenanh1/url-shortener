package io.github.snguyenanh1.urlshortener.service;

import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlResponse;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.entity.Url;
import io.github.snguyenanh1.urlshortener.exception.*;
import io.github.snguyenanh1.urlshortener.repository.UrlRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class UrlService {

    private static final String appendToUrl = "collision";
    private final UrlRepository urlRepository;

    //TODO: authentication logic

    @Autowired
    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Transactional
    public UrlResponse createShortUrl(UrlRequest urlRequest) {
        Url url = mapToUrl(urlRequest);
        if (url.getShortCode() == null) {
            String originalUrl = urlRequest.getUrl();
            String shortCode = generateShortCode(urlRequest.getUrl());
            while (urlRepository.existsByShortCode(shortCode)) {
                originalUrl += appendToUrl;
                shortCode = generateShortCode(originalUrl);
            }
            url.setShortCode(shortCode);
        } else {
            if (urlRepository.existsByShortCode(urlRequest.getShortCode())) {
                throw new InvalidShortCodeException("Existing short code");
            }
        }
        url.setCreatedAt(OffsetDateTime.now());
        if (url.getExpiresAt() == null) {
            url.setExpiresAt(url.getCreatedAt().plusDays(7));
        }
        url = urlRepository.save(url);
        return mapToUrlResponse(url);
    }

    @Transactional
    public UrlResponse getShortUrl(String shortCode) {
        Optional<Url> optionalUrl = urlRepository.findByShortCode(shortCode);
        if (optionalUrl.isPresent()) {
            Url url = optionalUrl.get();
            if (OffsetDateTime.now().isAfter(url.getExpiresAt())) {
                throw new ExpiredUrlException("Expired url");
            }
            return mapToUrlResponse(url);
        } else {
            throw new ShortCodeNotFoundException("No such short code");
        }
    }

    @Transactional
    public UrlResponse updateShortUrl(UrlUpdate urlUpdate) {
        Url url = urlRepository.findById(urlUpdate.getId())
                .orElseThrow(() -> new ShortCodeNotFoundException("No such short code"));
        String newShortCode = urlUpdate.getShortCode();
        if (!newShortCode.equals(url.getShortCode())) {
            if (urlRepository.existsByShortCode(newShortCode)) {
                throw new InvalidShortCodeException("Duplicate short code");
            } else {
                url.setShortCode(newShortCode);
            }
        }
        if (urlUpdate.getExpiresAt() != null
                && urlUpdate.getExpiresAt().isAfter(OffsetDateTime.now())) {
            url.setExpiresAt(urlUpdate.getExpiresAt());
        }
        url = urlRepository.save(url);
        return mapToUrlResponse(url);
    }

    @Transactional
    public void deleteShortUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException("No such short code"));
        urlRepository.delete(url);
    }

    private Url mapToUrl(UrlRequest urlRequest) {
        return Url.builder()
                .url(urlRequest.getUrl())
                .shortCode(urlRequest.getShortCode())
                .expiresAt(urlRequest.getExpiresAt())
                .build();
    }

    private UrlResponse mapToUrlResponse(Url url) {
        return UrlResponse.builder()
                .originalUrl(url.getUrl())
                .shortCode(url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt()).build();
    }

    private String generateShortCode(String originalUrl) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(originalUrl.getBytes(StandardCharsets.UTF_8));
            md.update(String.valueOf(System.nanoTime()).getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 7);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm error", e);
        }
    }

}
