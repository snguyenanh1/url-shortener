package io.github.snguyenanh1.urlshortener.service;

import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlResponse;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.entity.Url;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.enums.UserRole;
import io.github.snguyenanh1.urlshortener.exception.*;
import io.github.snguyenanh1.urlshortener.repository.UrlRepository;
import io.github.snguyenanh1.urlshortener.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * The type Url service.
 */
@Service
public class UrlService {

    private static final String appendToUrl = "collision";
    private final UrlRepository urlRepository;
    private final UserRepository userRepository;

    /**
     * Instantiates a new Url service.
     *
     * @param urlRepository the url repository
     */
    @Autowired
    public UrlService(UrlRepository urlRepository, UserRepository userRepository) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create short url response.
     *
     * @param urlRequest the url request
     * @return the url response
     */
    @Transactional
    public UrlResponse createShortUrl(@AuthenticationPrincipal User user, UrlRequest urlRequest) {
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
        url.setUser(user);
        url.setClicks(0L);
        url = urlRepository.save(url);
        return mapToUrlResponse(url, user);
    }

    /**
     * Create short url response for guest.
     * @param urlRequest the url request
     * @return the url response
     */
    public UrlResponse createShortUrlForGuest(UrlRequest urlRequest) {
        Url url = mapToUrl(urlRequest);
        String originalUrl = urlRequest.getUrl();
        String shortCode;
        do {
            shortCode = generateShortCode(originalUrl);
            originalUrl += appendToUrl;
        } while (urlRepository.existsByShortCode(shortCode));
        url.setShortCode(shortCode);
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(url.getCreatedAt().plusDays(7));
        url.setClicks(0L);
        url = urlRepository.save(url);
        return mapToUrlResponse(url, null);
    }

    /**
     * Gets short url.
     *
     * @param shortCode the short code
     * @return the short url
     */
    @Transactional
    public UrlResponse getShortUrl(@AuthenticationPrincipal User user, String shortCode) {
        Optional<Url> optionalUrl = urlRepository.findByShortCode(shortCode);
        if (optionalUrl.isPresent()) {
            Url url = optionalUrl.get();
            if (OffsetDateTime.now().isAfter(url.getExpiresAt())) {
                throw new ExpiredUrlException("Expired url");
            }
            url.setClicks((url.getClicks() == null ? 0 : url.getClicks()) + 1);
            urlRepository.save(url);
            return mapToUrlResponse(url, user);
        } else {
            throw new ShortCodeNotFoundException("No such short code");
        }
    }

    /**
     * Update short url response.
     *
     * @param urlUpdate the url update
     * @return the url response
     */
    @Transactional
    public UrlResponse updateShortUrl(@AuthenticationPrincipal User user, UrlUpdate urlUpdate) {
        Url url = urlRepository.findById(urlUpdate.getId())
                .orElseThrow(() -> new ShortCodeNotFoundException("No such short code"));
        if (user == null) {
            throw new AccessDeniedException("Access denied");
        }
        Long ownerId = (url.getUser() != null) ? url.getUser().getId() : null;
        if (!Objects.equals(ownerId, user.getId())) {
            throw new AccessDeniedException("Cannot update short code of an another account");
        }
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
        return mapToUrlResponse(url, user);
    }

    /**
     * Delete short url.
     *
     * @param shortCode the short code
     */
    @Transactional
    public void deleteShortUrl(@AuthenticationPrincipal User user, String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException("No such short code"));
        if (user == null) {
            throw new AccessDeniedException("Access denied");
        }
        Long ownerId = (url.getUser() != null) ? url.getUser().getId() : null;
        boolean isOwner = Objects.equals(user.getId(), ownerId);
        boolean isAdmin = user.getRole().equals(UserRole.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Cannot delete short code of an another account");
        }
        urlRepository.delete(url);
    }

    public Page<UrlResponse> getAllUrlsByUser(@AuthenticationPrincipal User user, Pageable pageable) {
        Page<Url> urls = urlRepository.getAllByUser_Id(pageable, user.getId());
        return urls.map(url -> mapToUrlResponse(url, user));
    }

    public Page<UrlResponse> getAllUrlsByUserId(@AuthenticationPrincipal User user, Pageable pageable, Long userId) {
        if (!user.getRole().equals(UserRole.ADMIN)) {
            throw new AccessDeniedException("Access denied");
        }
        User toFind = userRepository.findById(userId).orElseThrow(() -> new AccessDeniedException("No such user"));
        Page<Url> urls = urlRepository.getAllByUser_Id(pageable, toFind.getId());
        return urls.map(url -> mapToUrlResponse(url, user));
    }

    public Page<UrlResponse> getAllUrls(@AuthenticationPrincipal User user, Pageable pageable) {
        if (!user.getRole().equals(UserRole.ADMIN)) {
            throw new AccessDeniedException("Access denied");
        }
        Page<Url> urls = urlRepository.findAll(pageable);
        return urls.map(url -> mapToUrlResponse(url, user));
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

    private UrlResponse mapToUrlResponse(Url url, User user) {
        Long ownerId = (url.getUser() != null) ? url.getUser().getId() : null;
        boolean isAdmin = user != null && user.getRole().equals(UserRole.ADMIN);
        boolean isOwner = user != null && Objects.equals(user.getId(), ownerId);
        return UrlResponse.builder()
                .originalUrl(url.getUrl())
                .shortCode(url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .createdBy(url.getUser() == null ? null : url.getUser().getId())
                .clicks( (isOwner || isAdmin) ? url.getClicks() : null)
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
