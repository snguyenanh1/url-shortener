package io.github.snguyenanh1.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.snguyenanh1.urlshortener.UrlShortenerApplication;
import io.github.snguyenanh1.urlshortener.dto.user.UserSignIn;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = UrlShortenerApplication.class
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AuthControllerIT {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidCredentials_whenLogin_thenStatus200WithToken() throws Exception {
        UserSignIn signIn = new UserSignIn();
        signIn.setUsernameOrEmail("testuser");
        signIn.setPassword("password");

        String json = objectMapper.writeValueAsString(signIn);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(notNullValue())))
                .andExpect(jsonPath("$.type", is("Bearer")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidEmail_whenLogin_thenStatus200WithToken() throws Exception {
        UserSignIn signIn = new UserSignIn();
        signIn.setUsernameOrEmail("test@example.com");
        signIn.setPassword("password");

        String json = objectMapper.writeValueAsString(signIn);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(notNullValue())));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdminCredentials_whenLogin_thenStatus200WithToken() throws Exception {
        UserSignIn signIn = new UserSignIn();
        signIn.setUsernameOrEmail("adminuser");
        signIn.setPassword("password");

        String json = objectMapper.writeValueAsString(signIn);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(notNullValue())));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenWrongPassword_whenLogin_thenStatus403() throws Exception {
        UserSignIn signIn = new UserSignIn();
        signIn.setUsernameOrEmail("testuser");
        signIn.setPassword("wrongpassword");

        String json = objectMapper.writeValueAsString(signIn);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonExistentUser_whenLogin_thenStatusError() throws Exception {
        UserSignIn signIn = new UserSignIn();
        signIn.setUsernameOrEmail("nouser");
        signIn.setPassword("password");

        String json = objectMapper.writeValueAsString(signIn);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void givenTooShortUsername_whenLogin_thenStatus400() throws Exception {
        UserSignIn signIn = new UserSignIn();
        signIn.setUsernameOrEmail("ab");
        signIn.setPassword("password");

        String json = objectMapper.writeValueAsString(signIn);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenTooShortPassword_whenLogin_thenStatus400() throws Exception {
        UserSignIn signIn = new UserSignIn();
        signIn.setUsernameOrEmail("testuser");
        signIn.setPassword("ab");

        String json = objectMapper.writeValueAsString(signIn);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
