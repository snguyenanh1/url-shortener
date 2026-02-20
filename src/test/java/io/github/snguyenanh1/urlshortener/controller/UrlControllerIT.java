package io.github.snguyenanh1.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.snguyenanh1.urlshortener.UrlShortenerApplication;
import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.entity.Url;
import io.github.snguyenanh1.urlshortener.repository.UrlRepository;
import io.github.snguyenanh1.urlshortener.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = UrlShortenerApplication.class
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UrlControllerIT {

    private final MockMvc mockMvc;
    private final UrlRepository urlRepository;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    @Autowired
    public UrlControllerIT(MockMvc mockMvc, UrlRepository urlRepository,
                           ObjectMapper objectMapper, JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.urlRepository = urlRepository;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    private String tokenForUser(Long userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenGetUrl_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/urls/fb123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl", is("https://facebook.com")))
                .andExpect(jsonPath("$.shortCode", is("fb123")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenGetUrlAsGuest_thenClicksIsNull() throws Exception {
        mockMvc.perform(get("/api/v1/urls/fb123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").doesNotExist());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenGetUrlAsOwner_thenClicksIsVisible() throws Exception {
        mockMvc.perform(get("/api/v1/urls/fb123")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").isNumber());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenGetUrlAsAdmin_thenClicksIsVisible() throws Exception {
        mockMvc.perform(get("/api/v1/urls/fb123")
                        .header("Authorization", tokenForUser(2L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").isNumber());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenExpiredUrl_whenGetUrl_thenStatus410() throws Exception {
        mockMvc.perform(get("/api/v1/urls/yt456")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonExistentShortCode_whenGetUrl_thenStatus404() throws Exception {
        mockMvc.perform(get("/api/v1/urls/notexist")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidUrlRequest_whenCreateUrlAsGuest_thenStatus201() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://google.com");
        String json = objectMapper.writeValueAsString(urlRequest);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl", is("https://google.com")))
                .andExpect(jsonPath("$.shortCode").value(hasLength(7)))
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidUrlRequest_whenCreateUrlAsAuthenticated_thenStatus201WithCreatedBy() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://google.com");
        String json = objectMapper.writeValueAsString(urlRequest);

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl", is("https://google.com")))
                .andExpect(jsonPath("$.shortCode").value(hasLength(7)))
                .andExpect(jsonPath("$.createdBy", is(1)));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenInvalidUrl_whenCreateUrl_thenStatus400() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("abcxyz");
        String json = objectMapper.writeValueAsString(urlRequest);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenBlankUrl_whenCreateUrl_thenStatus400() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("");
        String json = objectMapper.writeValueAsString(urlRequest);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateShortCode_whenCreateUrl_thenStatus400() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://twitter.com");
        urlRequest.setShortCode("fb123");

        String json = objectMapper.writeValueAsString(urlRequest);
        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }


    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidUrlUpdate_whenUpdateUrlAsOwner_thenStatus200() throws Exception {
        Url url = urlRepository.findByShortCode("fb123").orElseThrow();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(url.getId());
        urlUpdate.setShortCode("fb999");
        urlUpdate.setExpiresAt(OffsetDateTime.now().plusDays(30));

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is("fb999")))
                .andExpect(jsonPath("$.originalUrl", is("https://facebook.com")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidUrlUpdate_whenUpdateUrlWithoutAuth_thenStatus401Or403() throws Exception {
        Url url = urlRepository.findByShortCode("fb123").orElseThrow();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(url.getId());
        urlUpdate.setShortCode("fb999");

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidUrlUpdate_whenUpdateUrlAsNonOwner_thenStatus403() throws Exception {
        Url url = urlRepository.findByShortCode("fb123").orElseThrow();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(url.getId());
        urlUpdate.setShortCode("fb999");

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/urls")
                        .header("Authorization", tokenForUser(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNullId_whenUpdateUrl_thenStatus400() throws Exception {
        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setShortCode("newcode");

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonExistentId_whenUpdateUrl_thenStatus404() throws Exception {
        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(99999L);
        urlUpdate.setShortCode("newcode");

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateShortCode_whenUpdateUrl_thenStatus400() throws Exception {
        Url url = urlRepository.findByShortCode("fb123").orElseThrow();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(url.getId());
        urlUpdate.setShortCode("gh789"); // another existing short code

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }


    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenDeleteUrlAsOwner_thenStatus204() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/fb123")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenDeleteUrlAsAdmin_thenStatus204() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/fb123")
                        .header("Authorization", tokenForUser(2L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenDeleteUrlWithoutAuth_thenStatus403() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/fb123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonExistentShortCode_whenDeleteUrl_thenStatus404() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/notexist")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAuthenticatedUser_whenGetMyUrls_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/urls/me")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenUnauthenticated_whenGetMyUrls_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/urls/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }


    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenGetUrlsByUserId_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/urls/users/1")
                        .header("Authorization", tokenForUser(2L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenGetUrlsByUserId_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/urls/users/1")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenUnauthenticated_whenGetUrlsByUserId_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/urls/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenGetAllUrls_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/urls/users")
                        .header("Authorization", tokenForUser(2L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenGetAllUrls_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/urls/users")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenUnauthenticated_whenGetAllUrls_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/urls/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenGetUrlAsNonOwner_thenClicksIsNull() throws Exception {
        mockMvc.perform(get("/api/v1/urls/fb123")
                        .header("Authorization", tokenForUser(3L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").doesNotExist());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidCustomShortCode_whenCreateUrlAsAuthenticated_thenStatus201() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://google.com");
        urlRequest.setShortCode("mycode1");

        String json = objectMapper.writeValueAsString(urlRequest);
        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", is("mycode1")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenTooShortShortCode_whenCreateUrl_thenStatus400() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://google.com");
        urlRequest.setShortCode("ab");

        String json = objectMapper.writeValueAsString(urlRequest);
        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenCustomExpirationDate_whenCreateUrlAsAuthenticated_thenStatus201() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://google.com");
        urlRequest.setExpiresAt(OffsetDateTime.now().plusDays(30));

        String json = objectMapper.writeValueAsString(urlRequest);
        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl", is("https://google.com")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenSameShortCode_whenUpdateUrlAsOwner_thenStatus200() throws Exception {
        Url url = urlRepository.findByShortCode("fb123").orElseThrow();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(url.getId());
        urlUpdate.setShortCode("fb123");
        urlUpdate.setExpiresAt(OffsetDateTime.now().plusDays(30));

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/urls")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is("fb123")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenDeleteUrlAsNonOwnerNonAdmin_thenStatus403() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/fb123")
                        .header("Authorization", tokenForUser(3L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenGetUrlsByNonExistentUserId_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/urls/users/99999")
                        .header("Authorization", tokenForUser(2L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}

