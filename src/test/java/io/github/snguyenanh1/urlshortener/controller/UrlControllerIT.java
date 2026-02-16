package io.github.snguyenanh1.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.snguyenanh1.urlshortener.UrlShortenerApplication;
import io.github.snguyenanh1.urlshortener.dto.url.UrlRequest;
import io.github.snguyenanh1.urlshortener.dto.url.UrlUpdate;
import io.github.snguyenanh1.urlshortener.entity.Url;
import io.github.snguyenanh1.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;


import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = UrlShortenerApplication.class
)
@AutoConfigureMockMvc
public class UrlControllerIT {

    private final MockMvc mockMvc;

    private final UrlRepository urlRepository;

    private final ObjectMapper objectMapper;

    private final OffsetDateTime now = OffsetDateTime.now();

    @Autowired
    public UrlControllerIT(MockMvc mockMvc, UrlRepository urlRepository, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.urlRepository = urlRepository;
        this.objectMapper = objectMapper;
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenGetUrl_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/short-url/fb123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl", is("https://facebook.com")))
                .andExpect(jsonPath("$.shortCode", is("fb123")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidUrlRequest_whenCreateUrl_thenStatus201() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://google.com");
        String json = objectMapper.writeValueAsString(urlRequest);
        mockMvc.perform(post("/api/v1/short-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.originalUrl", is("https://google.com")))
                .andExpect(jsonPath("$.shortCode").value(hasLength(7)));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidUrlUpdate_whenUpdateUrl_thenStatus200() throws Exception {
        Url url = urlRepository.findByShortCode("fb123").orElseThrow();

        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setId(url.getId());
        urlUpdate.setShortCode("fb999");
        urlUpdate.setExpiresAt(now.plusDays(30));

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/short-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shortCode", is("fb999")))
                .andExpect(jsonPath("$.originalUrl", is("https://facebook.com")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenShortCode_whenDeleteUrl_thenStatus204() throws Exception {
        mockMvc.perform(delete("/api/v1/short-url/fb123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenInvalidUrl_whenCreateUrl_thenStatus400() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("abcxyz");
        String json = objectMapper.writeValueAsString(urlRequest);

        mockMvc.perform(post("/api/v1/short-url")
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

        mockMvc.perform(post("/api/v1/short-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNullId_whenUpdateUrl_thenStatus400() throws Exception {
        UrlUpdate urlUpdate = new UrlUpdate();
        urlUpdate.setShortCode("newcode");

        String json = objectMapper.writeValueAsString(urlUpdate);
        mockMvc.perform(patch("/api/v1/short-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }


    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenExpiredUrl_whenGetUrl_thenStatus410() throws Exception {
        mockMvc.perform(get("/api/v1/short-url/yt456")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonExistentShortCode_whenGetUrl_thenStatus404() throws Exception {
        mockMvc.perform(get("/api/v1/short-url/notexist")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonExistentShortCode_whenDeleteUrl_thenStatus404() throws Exception {
        mockMvc.perform(delete("/api/v1/short-url/notexist")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateShortCode_whenCreateUrl_thenStatus400() throws Exception {
        UrlRequest urlRequest = new UrlRequest();
        urlRequest.setUrl("https://twitter.com");
        urlRequest.setShortCode("fb123");

        String json = objectMapper.writeValueAsString(urlRequest);
        mockMvc.perform(post("/api/v1/short-url")
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
        mockMvc.perform(patch("/api/v1/short-url")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

}
