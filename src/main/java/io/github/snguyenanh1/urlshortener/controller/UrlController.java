package io.github.snguyenanh1.urlshortener.controller;

import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlResponse;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {
    private final UrlService urlService;

    @Autowired
    public UrlController(UrlService urlService) {
        this.urlService = urlService;
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
