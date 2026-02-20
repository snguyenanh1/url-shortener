package io.github.snguyenanh1.urlshortener.service;

import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlResponse;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.entity.Url;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.enums.UserRole;
import io.github.snguyenanh1.urlshortener.exception.AccessDeniedException;
import io.github.snguyenanh1.urlshortener.exception.ExpiredUrlException;
import io.github.snguyenanh1.urlshortener.exception.InvalidShortCodeException;
import io.github.snguyenanh1.urlshortener.exception.ShortCodeNotFoundException;
import io.github.snguyenanh1.urlshortener.repository.UrlRepository;
import io.github.snguyenanh1.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UrlService urlService;

    private User testUser;
    private User adminUser;
    private User otherUser;

    @BeforeEach
    public void setUp() {
        urlService = new UrlService(urlRepository, userRepository);
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.USER)
                .build();
        adminUser = User.builder()
                .id(10L)
                .username("admin")
                .password("encoded")
                .name("Admin")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .build();
        otherUser = User.builder()
                .id(2L)
                .username("other")
                .password("encoded")
                .name("Other")
                .email("other@example.com")
                .role(UserRole.USER)
                .build();
    }


    @Test
    public void createShortUrl_ValidUrl_ReturnValidUrlResponse() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrl(testUser, urlRequest);

        verify(urlRepository, times(1)).save(any(Url.class));
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getOriginalUrl()).isEqualTo(url);
        assertThat(urlResponse.getShortCode()).hasSize(7);
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

        UrlResponse urlResponse = urlService.createShortUrl(testUser, urlRequest);

        assertThat(urlResponse.getShortCode()).isEqualTo("abc124");
    }

    @Test
    public void createShortUrl_CustomExpirationDate_ReturnValidUrlResponse() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(14);
        urlRequest.setExpiresAt(expirationDate);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrl(testUser, urlRequest);

        assertThat(urlResponse.getExpiresAt()).isEqualTo(expirationDate);
    }

    @Test
    public void createShortUrl_ExistingShortCode_ThrowInvalidShortCodeException() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        urlRequest.setShortCode("ABC123");
        when(urlRepository.existsByShortCode("ABC123")).thenReturn(true);

        Exception e = assertThrows(InvalidShortCodeException.class,
                () -> urlService.createShortUrl(testUser, urlRequest));
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

        UrlResponse urlResponse = urlService.createShortUrl(testUser, urlRequest);

        verify(urlRepository, atLeast(2)).existsByShortCode(anyString());
        assertThat(urlResponse.getShortCode()).isNotNull();
    }

    @Test
    public void createShortUrl_OwnerCanSeeClicks() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrl(testUser, urlRequest);

        assertThat(urlResponse.getCreatedBy()).isEqualTo(testUser.getId());
    }

    // ========== createShortUrlForGuest ==========

    @Test
    public void createShortUrlForGuest_ValidUrl_ReturnValidUrlResponse() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrlForGuest(urlRequest);

        verify(urlRepository, times(1)).save(any(Url.class));
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getOriginalUrl()).isEqualTo(url);
        assertThat(urlResponse.getShortCode()).hasSize(7);
        assertThat(urlResponse.getExpiresAt()).isEqualTo(urlResponse.getCreatedAt().plusDays(7));
        assertThat(urlResponse.getCreatedBy()).isNull();
        assertThat(urlResponse.getClicks()).isNull();
    }

    @Test
    public void createShortUrlForGuest_CollisionHandled() {
        String url = "https://www.google.com";
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl(url);
        when(urlRepository.existsByShortCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.createShortUrlForGuest(urlRequest);

        verify(urlRepository, atLeast(2)).existsByShortCode(anyString());
        assertThat(urlResponse.getShortCode()).isNotNull();
    }

    // ========== getShortUrl ==========

    @Test
    public void getShortUrl_ExistingShortCode_ReturnUrlResponse() {
        Url url = new Url();
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(OffsetDateTime.now().plusDays(7));
        url.setClicks(0L);
        url.setUser(testUser);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.getShortUrl(testUser, "abc123");

        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getShortCode()).isEqualTo("abc123");
        assertThat(urlResponse.getOriginalUrl()).isEqualTo("http://www.google.com");
    }

    @Test
    public void getShortUrl_AsGuest_ReturnUrlResponseWithoutClicks() {
        Url url = new Url();
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(OffsetDateTime.now().plusDays(7));
        url.setClicks(0L);
        url.setUser(testUser);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.getShortUrl(null, "abc123");

        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getClicks()).isNull();
    }

    @Test
    public void getShortUrl_AsOwner_ReturnUrlResponseWithClicks() {
        Url url = new Url();
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(OffsetDateTime.now().plusDays(7));
        url.setClicks(5L);
        url.setUser(testUser);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.getShortUrl(testUser, "abc123");

        assertThat(urlResponse.getClicks()).isEqualTo(6L);
    }

    @Test
    public void getShortUrl_AsAdmin_ReturnUrlResponseWithClicks() {
        Url url = new Url();
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(OffsetDateTime.now().plusDays(7));
        url.setClicks(5L);
        url.setUser(testUser);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.getShortUrl(adminUser, "abc123");

        assertThat(urlResponse.getClicks()).isEqualTo(6L);
    }

    @Test
    public void getShortUrl_AsNonOwner_ReturnUrlResponseWithoutClicks() {
        Url url = new Url();
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(OffsetDateTime.now().plusDays(7));
        url.setClicks(5L);
        url.setUser(testUser);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.getShortUrl(otherUser, "abc123");

        assertThat(urlResponse.getClicks()).isNull();
    }

    @Test
    public void getShortUrl_NullClicks_InitializesToOne() {
        Url url = new Url();
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(OffsetDateTime.now().plusDays(7));
        url.setClicks(null);
        url.setUser(testUser);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.getShortUrl(testUser, "abc123");

        assertThat(urlResponse.getClicks()).isEqualTo(1L);
    }

    @Test
    public void getShortUrl_ShortCodeNotExisted_ThrowShortCodeNotFoundException() {
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());

        Exception e = assertThrows(ShortCodeNotFoundException.class,
                () -> urlService.getShortUrl(testUser, "abc123"));
        assertThat(e.getMessage()).isEqualTo("No such short code");
    }

    @Test
    public void getShortUrl_ExpiredUrl_ThrowExpiredUrlException() {
        Url url = new Url();
        url.setShortCode("abc123");
        url.setUrl("http://www.google.com");
        url.setCreatedAt(OffsetDateTime.now().minusDays(8));
        url.setExpiresAt(OffsetDateTime.now().minusDays(1));
        url.setClicks(0L);
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(url));

        Exception e = assertThrows(ExpiredUrlException.class,
                () -> urlService.getShortUrl(testUser, "abc123"));
        assertThat(e.getMessage()).isEqualTo("Expired url");
    }

    // ========== updateShortUrl ==========

    @Test
    public void updateShortUrl_ExistingUrl_ReturnValidUrlResponse() {
        Long id = 1L;
        Url existingUrl = Url.builder()
                .id(id).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("newCode");
        urlUpdate.setExpiresAt(OffsetDateTime.now().plusDays(10));

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.existsByShortCode("newCode")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(testUser, urlUpdate);

        verify(urlRepository, times(1)).save(any(Url.class));
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getShortCode()).isEqualTo("newCode");
    }

    @Test
    public void updateShortUrl_SameShortCode_ReturnValidUrlResponse() {
        Long id = 1L;
        String shortCode = "abc123";
        Url existingUrl = Url.builder()
                .id(id).shortCode(shortCode).url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode(shortCode);
        urlUpdate.setExpiresAt(OffsetDateTime.now().plusDays(10));

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(testUser, urlUpdate);

        verify(urlRepository, times(1)).save(any(Url.class));
        verify(urlRepository, never()).existsByShortCode(anyString());
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getShortCode()).isEqualTo(shortCode);
    }

    @Test
    public void updateShortUrl_UpdateExpirationDate_ReturnValidUrlResponse() {
        Long id = 1L;
        Url existingUrl = Url.builder()
                .id(id).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        OffsetDateTime newExpiresAt = OffsetDateTime.now().plusDays(15);
        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("abc123");
        urlUpdate.setExpiresAt(newExpiresAt);

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(testUser, urlUpdate);

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

        Exception e = assertThrows(ShortCodeNotFoundException.class,
                () -> urlService.updateShortUrl(testUser, urlUpdate));
        assertThat(e.getMessage()).isEqualTo("No such short code");
    }

    @Test
    public void updateShortUrl_DuplicateShortCode_ThrowInvalidShortCodeException() {
        Long id = 1L;
        Url existingUrl = Url.builder()
                .id(id).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("existingCode");

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.existsByShortCode("existingCode")).thenReturn(true);

        Exception e = assertThrows(InvalidShortCodeException.class,
                () -> urlService.updateShortUrl(testUser, urlUpdate));
        assertThat(e.getMessage()).isEqualTo("Duplicate short code");
    }

    @Test
    public void updateShortUrl_PastExpirationDate_DoNotUpdateExpirationDate() {
        Long id = 1L;
        OffsetDateTime originalExpiresAt = OffsetDateTime.now().plusDays(7);
        Url existingUrl = Url.builder()
                .id(id).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(originalExpiresAt)
                .user(testUser).clicks(0L).build();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("abc123");
        urlUpdate.setExpiresAt(OffsetDateTime.now().minusDays(1));

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(i -> i.getArgument(0));

        UrlResponse urlResponse = urlService.updateShortUrl(testUser, urlUpdate);

        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.getExpiresAt()).isEqualTo(originalExpiresAt);
    }

    @Test
    public void updateShortUrl_NullUser_ThrowAccessDeniedException() {
        Long id = 1L;
        Url existingUrl = Url.builder()
                .id(id).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("newCode");

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.updateShortUrl(null, urlUpdate));
        assertThat(e.getMessage()).isEqualTo("Access denied");
    }

    @Test
    public void updateShortUrl_NotOwner_ThrowAccessDeniedException() {
        Long id = 1L;
        Url existingUrl = Url.builder()
                .id(id).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("newCode");

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.updateShortUrl(otherUser, urlUpdate));
        assertThat(e.getMessage()).isEqualTo("Cannot update short code of an another account");
    }

    // ========== deleteShortUrl ==========

    @Test
    public void deleteShortUrl_AsOwner_DeleteSuccessfully() {
        String shortCode = "abc123";
        Url url = Url.builder()
                .shortCode(shortCode).url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        urlService.deleteShortUrl(testUser, shortCode);

        verify(urlRepository, times(1)).findByShortCode(shortCode);
        verify(urlRepository, times(1)).delete(url);
    }

    @Test
    public void deleteShortUrl_AsAdmin_DeleteSuccessfully() {
        String shortCode = "abc123";
        Url url = Url.builder()
                .shortCode(shortCode).url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        urlService.deleteShortUrl(adminUser, shortCode);

        verify(urlRepository, times(1)).delete(url);
    }

    @Test
    public void deleteShortUrl_ShortCodeNotFound_ThrowShortCodeNotFoundException() {
        String shortCode = "abc123";
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        Exception e = assertThrows(ShortCodeNotFoundException.class,
                () -> urlService.deleteShortUrl(testUser, shortCode));
        assertThat(e.getMessage()).isEqualTo("No such short code");
        verify(urlRepository, never()).delete(any(Url.class));
    }

    @Test
    public void deleteShortUrl_NullUser_ThrowAccessDeniedException() {
        String shortCode = "abc123";
        Url url = Url.builder()
                .shortCode(shortCode).url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.deleteShortUrl(null, shortCode));
        assertThat(e.getMessage()).isEqualTo("Access denied");
    }

    @Test
    public void deleteShortUrl_NotOwnerNotAdmin_ThrowAccessDeniedException() {
        String shortCode = "abc123";
        Url url = Url.builder()
                .shortCode(shortCode).url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.deleteShortUrl(otherUser, shortCode));
        assertThat(e.getMessage()).isEqualTo("Cannot delete short code of an another account");
    }

    // ========== getAllUrlsByUser ==========

    @Test
    public void getAllUrlsByUser_ReturnPageOfUrls() {
        Pageable pageable = PageRequest.of(0, 10);
        Url url = Url.builder()
                .id(1L).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(5L).build();
        Page<Url> urlPage = new PageImpl<>(List.of(url));
        when(urlRepository.getAllByUser_Id(pageable, testUser.getId())).thenReturn(urlPage);

        Page<UrlResponse> result = urlService.getAllUrlsByUser(testUser, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getClicks()).isEqualTo(5L);
    }

    // ========== getAllUrlsByUserId ==========

    @Test
    public void getAllUrlsByUserId_AsAdmin_ReturnPageOfUrls() {
        Pageable pageable = PageRequest.of(0, 10);
        Url url = Url.builder()
                .id(1L).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(5L).build();
        Page<Url> urlPage = new PageImpl<>(List.of(url));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(urlRepository.getAllByUser_Id(pageable, testUser.getId())).thenReturn(urlPage);

        Page<UrlResponse> result = urlService.getAllUrlsByUserId(adminUser, pageable, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void getAllUrlsByUserId_AsNonAdmin_ThrowAccessDeniedException() {
        Pageable pageable = PageRequest.of(0, 10);

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.getAllUrlsByUserId(testUser, pageable, 2L));
        assertThat(e.getMessage()).isEqualTo("Access denied");
    }

    @Test
    public void getAllUrlsByUserId_UserNotFound_ThrowAccessDeniedException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.getAllUrlsByUserId(adminUser, pageable, 99L));
        assertThat(e.getMessage()).isEqualTo("No such user");
    }

    // ========== getAllUrls ==========

    @Test
    public void getAllUrls_AsAdmin_ReturnPageOfUrls() {
        Pageable pageable = PageRequest.of(0, 10);
        Url url = Url.builder()
                .id(1L).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(5L).build();
        Page<Url> urlPage = new PageImpl<>(List.of(url));
        when(urlRepository.findAll(pageable)).thenReturn(urlPage);

        Page<UrlResponse> result = urlService.getAllUrls(adminUser, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void getAllUrls_AsNonAdmin_ThrowAccessDeniedException() {
        Pageable pageable = PageRequest.of(0, 10);

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.getAllUrls(testUser, pageable));
        assertThat(e.getMessage()).isEqualTo("Access denied");
    }


    @Test
    public void updateShortUrl_AsAdminNonOwner_ThrowAccessDeniedException() {
        Long id = 1L;
        Url existingUrl = Url.builder()
                .id(id).shortCode("abc123").url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(testUser).clicks(0L).build();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(id);
        urlUpdate.setShortCode("newCode");

        when(urlRepository.findById(id)).thenReturn(Optional.of(existingUrl));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.updateShortUrl(adminUser, urlUpdate));
        assertThat(e.getMessage()).isEqualTo("Cannot update short code of an another account");
    }

    @Test
    public void getAllUrlsByUser_EmptyResult_ReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Url> emptyPage = new PageImpl<>(List.of());
        when(urlRepository.getAllByUser_Id(pageable, testUser.getId())).thenReturn(emptyPage);

        Page<UrlResponse> result = urlService.getAllUrlsByUser(testUser, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    public void deleteShortUrl_GuestUrlAsAdmin_DeleteSuccessfully() {
        String shortCode = "abc123";
        Url url = Url.builder()
                .shortCode(shortCode).url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(null).clicks(0L).build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        urlService.deleteShortUrl(adminUser, shortCode);

        verify(urlRepository, times(1)).delete(url);
    }

    @Test
    public void deleteShortUrl_GuestUrlAsNonAdmin_ThrowAccessDeniedException() {
        String shortCode = "abc123";
        Url url = Url.builder()
                .shortCode(shortCode).url("http://www.google.com")
                .createdAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(7))
                .user(null).clicks(0L).build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> urlService.deleteShortUrl(testUser, shortCode));
        assertThat(e.getMessage()).isEqualTo("Cannot delete short code of an another account");
    }
}

