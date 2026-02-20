package io.github.snguyenanh1.urlshortener.controller;

import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlResponse;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {
    private final UrlService urlService;

    /**
     * Instantiates a new Url controller.
     *
     * @param urlService the url service
     */
import io.github.snguyenanh1.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/short-url")
public class UrlController {
    private final UrlService urlService;

    @Autowired
    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Gets short url.
     *
     * @param user      the user
     * @param shortCode the short code
     * @return the short url
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlResponse> getShortUrl(@AuthenticationPrincipal User user, @PathVariable String shortCode) {
        UrlResponse urlResponse = urlService.getShortUrl(user, shortCode);
        return ResponseEntity.ok(urlResponse);
    }

    /**
     * Create short url response entity.
     *
     * @param user       the user
     * @param urlRequest the url request
     * @return the response entity
     */
    @PostMapping()
    public ResponseEntity<UrlResponse> createShortUrl(@AuthenticationPrincipal User user, @RequestBody @Valid UrlRequest urlRequest) {
        UrlResponse urlResponse;
        if (user == null) {
            urlResponse = urlService.createShortUrlForGuest(urlRequest);
        } else {
            urlResponse = urlService.createShortUrl(user, urlRequest);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(urlResponse);
    }

    /**
     * Update short url response entity.
     *
     * @param user      the user
     * @param urlUpdate the url update
     * @return the response entity
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping()
    public ResponseEntity<UrlResponse> updateShortUrl(@AuthenticationPrincipal User user, @RequestBody @Valid UrlUpdate urlUpdate) {
        UrlResponse urlResponse = urlService.updateShortUrl(user, urlUpdate);
        return ResponseEntity.ok(urlResponse);
    }

    /**
     * Delete short url response entity.
     *
     * @param user      the user
     * @param shortCode the short code
     * @return the response entity
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteShortUrl(@AuthenticationPrincipal User user, @PathVariable String shortCode) {
        urlService.deleteShortUrl(user, shortCode);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets all my urls.
     *
     * @param user     the user
     * @param pageable the pageable
     * @return the all my urls
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<Page<UrlResponse>> getAllMyUrls(@AuthenticationPrincipal User user, @PageableDefault Pageable pageable) {
        Page<UrlResponse> urlResponsePage = urlService.getAllUrlsByUser(user, pageable);
        return ResponseEntity.ok(urlResponsePage);
    }

    /**
     * Gets all by id.
     *
     * @param user     the user
     * @param userId   the user id
     * @param pageable the pageable
     * @return the all by id
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<UrlResponse>> getAllById(@AuthenticationPrincipal User user,
                                                  @PathVariable Long userId, @PageableDefault Pageable pageable) {
        Page<UrlResponse> urlResponsePage = urlService.getAllUrlsByUserId(user, pageable, userId);
        return ResponseEntity.ok(urlResponsePage);
    }

    /**
     * Gets all urls.
     *
     * @param user     the user
     * @param pageable the pageable
     * @return the all urls
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<Page<UrlResponse>> getAllUrls(@AuthenticationPrincipal User user,
                                                    @PageableDefault Pageable pageable) {
        Page<UrlResponse> urlResponsePage = urlService.getAllUrls(user, pageable);
        return ResponseEntity.ok(urlResponsePage);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlResponse> getShortUrl(@PathVariable String shortCode) {
        UrlResponse urlResponse = urlService.getShortUrl(shortCode);
        return ResponseEntity.ok(urlResponse);
    }

    @PostMapping()
    public ResponseEntity<UrlResponse> createShortUrl(@RequestBody @Valid UrlRequest urlRequest) {
        UrlResponse urlResponse = urlService.createShortUrl(urlRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(urlResponse);
    }

    @PatchMapping()
    public ResponseEntity<UrlResponse> updateShortUrl(@RequestBody @Valid UrlUpdate urlUpdate) {
        UrlResponse urlResponse = urlService.updateShortUrl(urlUpdate);
        return ResponseEntity.ok(urlResponse);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<UrlResponse> deleteShortUrl(@PathVariable String shortCode) {
        urlService.deleteShortUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

}
