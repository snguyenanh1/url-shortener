package io.github.snguyenanh1.urlshortener.service;

import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlResponse;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.entity.Url;
import io.github.snguyenanh1.urlshortener.exception.ExpiredUrlException;
import io.github.snguyenanh1.urlshortener.exception.InvalidShortCodeException;
import io.github.snguyenanh1.urlshortener.exception.ShortCodeNotFoundException;
import io.github.snguyenanh1.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    private String baseUrl = "https://short-domain.com";

    @BeforeEach
    public void setUp() {
        urlService = new UrlService(urlRepository);
    }

    @Test
    public void createShortUrl_ValidUrl_ReturnValidUrlResponse() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrl(urlRequest);
        verify(urlRepository, times(1)).save(any(Url.class));
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getOriginalUrl()).isEqualTo(url);
        assertThat(urlResponse.getShortCode().length()).isEqualTo(7);
        assertThat(urlResponse.getExpiresAt()).isEqualTo(urlResponse.getCreatedAt().plusDays(7));
    }


    @Test
    public void createShortUrl_CustomShortCode_ReturnValidUrlResponse() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        urlRequest.setShortCode("abc124");
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrl(urlRequest);

        assertThat(urlResponse.getShortCode()).isEqualTo("abc124");
    }

    @Test
    public void createShortUrl_CustomExpirationDate_ReturnValidUrlResponse() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(7);
        urlRequest.setExpiresAt(expirationDate);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrl(urlRequest);

        assertThat(urlResponse.getExpiresAt()).isEqualTo(expirationDate);
    }

    @Test
    public void createShortUrl_DuplicateUrls_ReturnDifferentShortCode() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse first = urlService.createShortUrl(urlRequest);
        UrlResponse second = urlService.createShortUrl(urlRequest);

        assertThat(first.getShortCode()).isNotEqualTo(second.getShortCode());
    }

    @Test
    public void createShortUrl_ExistingShortCode_ThrowExistingShortCodeException() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        urlRequest.setShortCode("ABC");
        when(urlRepository.existsByShortCode(anyString())).thenReturn(true);

        Exception e = assertThrows(InvalidShortCodeException.class, () -> urlService.createShortUrl(urlRequest));
        assertThat(e.getMessage()).isEqualTo("Existing short code");
    }

    @Test
    public void createShortUrl_GenerateShortCodeWithCollision_GenerateShortCodeAgain() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        when(urlRepository.existsByShortCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrl(urlRequest);

        verify(urlRepository, atLeast(2)).existsByShortCode(anyString());
        assertThat(urlResponse.getShortCode()).isNotNull();
    }

    @Test
    public void getShortUrl_ExistingShortCode_ReturnUrlResponse() {
        Url url = new Url();
        String shortCode = "abc123";
        String originalUrl = "http://www.google.com";
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = createdAt.plusDays(7);
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(createdAt);
        url.setExpiresAt(expiresAt);
        Optional<Url> optionalUrl = Optional.of(url);
        when(urlRepository.findByShortCode(anyString())).thenReturn(optionalUrl);

        UrlResponse urlResponse = urlService.getShortUrl("abc123");

        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getShortCode()).isEqualTo("abc123");
        assertThat(urlResponse.getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(urlResponse.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(urlResponse.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    public void getShortUrl_ShortCodeNotExisted_ThrowShortCodeNotFoundException() {
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());

        Exception e = assertThrows(ShortCodeNotFoundException.class, () -> urlService.getShortUrl("abc123"));
        assertThat(e.getMessage()).isEqualTo("No such short code");
    }

    @Test
    public void getShortUrl_ExpiredUrl_ThrowExpiredUrlException() {
        Url url = new Url();
        String shortCode = "abc123";
        String originalUrl = "http://www.google.com";
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(8);
        OffsetDateTime expiresAt = createdAt.plusDays(7);
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(createdAt);
        url.setExpiresAt(expiresAt);
        Optional<Url> optionalUrl = Optional.of(url);
        when(urlRepository.findByShortCode(anyString())).thenReturn(optionalUrl);

        Exception e = assertThrows(ExpiredUrlException.class, () -> urlService.getShortUrl("abc123"));
        assertThat(e.getMessage()).isEqualTo("Expired url");
    }

    @Test
    public void updateShortUrl_ExistingUrl_ReturnValidUrlResponse() {
        Long id = 1L;
        Url existingUrl = new Url();
        existingUrl.setId(id);
        existingUrl.setShortCode("abc123");
        existingUrl.setUrl("http://www.google.com");
        existingUrl.setCreatedAt(OffsetDateTime.now());
        existingUrl.setExpiresAt(OffsetDateTime.now().plusDays(7));

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("newCode");
        urlUpdate.setExpiresAt(OffsetDateTime.now().plusDays(10));

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.existsByShortCode("newCode")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(urlUpdate);

        verify(urlRepository, times(1)).save(any(Url.class));
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getShortCode()).isEqualTo("newCode");
    }

    @Test
    public void updateShortUrl_SameShortCode_ReturnValidUrlResponse() {
        Long id = 1L;
        String shortCode = "abc123";
        Url existingUrl = new Url();
        existingUrl.setId(id);
        existingUrl.setShortCode(shortCode);
        existingUrl.setUrl("http://www.google.com");
        existingUrl.setCreatedAt(OffsetDateTime.now());
        existingUrl.setExpiresAt(OffsetDateTime.now().plusDays(7));

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode(shortCode);
        urlUpdate.setExpiresAt(OffsetDateTime.now().plusDays(10));

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(urlUpdate);

        verify(urlRepository, times(1)).save(any(Url.class));
        verify(urlRepository, never()).existsByShortCode(anyString());
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getShortCode()).isEqualTo(shortCode);
    }

    @Test
    public void updateShortUrl_UpdateExpirationDate_ReturnValidUrlResponse() {
        Long id = 1L;
        Url existingUrl = new Url();
        existingUrl.setId(id);
        existingUrl.setShortCode("abc123");
        existingUrl.setUrl("http://www.google.com");
        existingUrl.setCreatedAt(OffsetDateTime.now());
        existingUrl.setExpiresAt(OffsetDateTime.now().plusDays(7));

        OffsetDateTime newExpiresAt = OffsetDateTime.now().plusDays(15);
        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("abc123");
        urlUpdate.setExpiresAt(newExpiresAt);

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(urlUpdate);

        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getExpiresAt()).isEqualTo(newExpiresAt);
    }

    @Test
    public void updateShortUrl_IdNotFound_ThrowShortCodeNotFoundException() {
        Long id = 1L;
        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("newCode");

        when(urlRepository.findById(id)).thenReturn(Optional.empty());

        Exception e = assertThrows(ShortCodeNotFoundException.class, () -> urlService.updateShortUrl(urlUpdate));
        assertThat(e.getMessage()).isEqualTo("No such short code");
    }

    @Test
    public void updateShortUrl_DuplicateShortCode_ThrowExistingShortCodeException() {
        Long id = 1L;
        Url existingUrl = new Url();
        existingUrl.setId(id);
        existingUrl.setShortCode("abc123");
        existingUrl.setUrl("http://www.google.com");
        existingUrl.setCreatedAt(OffsetDateTime.now());
        existingUrl.setExpiresAt(OffsetDateTime.now().plusDays(7));

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("existingCode");

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.existsByShortCode("existingCode")).thenReturn(true);

        Exception e = assertThrows(InvalidShortCodeException.class, () -> urlService.updateShortUrl(urlUpdate));
        assertThat(e.getMessage()).isEqualTo("Duplicate short code");
    }

    @Test
    public void updateShortUrl_PastExpirationDate_DoNotUpdateExpirationDate() {
        Long id = 1L;
        OffsetDateTime originalExpiresAt = OffsetDateTime.now().plusDays(7);
        Url existingUrl = new Url();
        existingUrl.setId(id);
        existingUrl.setShortCode("abc123");
        existingUrl.setUrl("http://www.google.com");
        existingUrl.setCreatedAt(OffsetDateTime.now());
        existingUrl.setExpiresAt(originalExpiresAt);

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("abc123");
        urlUpdate.setExpiresAt(OffsetDateTime.now().minusDays(1));

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(urlUpdate);

        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getExpiresAt()).isEqualTo(originalExpiresAt);
    }

    @Test
    public void deleteShortUrl_ExistingShortCode_DeleteSuccessfully() {
        String shortCode = "abc123";
        Url url = new Url();
        url.setShortCode(shortCode);
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(OffsetDateTime.now().plusDays(7));

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        urlService.deleteShortUrl(shortCode);

        verify(urlRepository, times(1)).findByShortCode(shortCode);
        verify(urlRepository, times(1)).delete(url);
    }

    @Test
    public void deleteShortUrl_ShortCodeNotFound_ThrowShortCodeNotFoundException() {
        String shortCode = "abc123";
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        Exception e = assertThrows(ShortCodeNotFoundException.class, () -> urlService.deleteShortUrl(shortCode));
        assertThat(e.getMessage()).isEqualTo("No such short code");
        verify(urlRepository, never()).delete(any(Url.class));
    }
}
