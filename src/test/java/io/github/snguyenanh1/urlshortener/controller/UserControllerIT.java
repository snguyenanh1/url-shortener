package io.github.snguyenanh1.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.snguyenanh1.urlshortener.UrlShortenerApplication;
import io.github.snguyenanh1.urlshortener.dto.user.UserDetailChange;
import io.github.snguyenanh1.urlshortener.dto.user.UserPasswordChange;
import io.github.snguyenanh1.urlshortener.dto.user.UserSignUp;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = UrlShortenerApplication.class
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerIT {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    @Autowired
    public UserControllerIT(MockMvc mockMvc, ObjectMapper objectMapper, JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    private String tokenForUser(Long userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidData_whenSignUp_thenStatus200() throws Exception {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("newuser1");
        signUp.setPassword("pass1234");
        signUp.setConfirmPassword("pass1234");
        signUp.setEmail("newuser1@example.com");
        signUp.setName("New User");

        String json = objectMapper.writeValueAsString(signUp);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("newuser1")))
                .andExpect(jsonPath("$.email", is("newuser1@example.com")))
                .andExpect(jsonPath("$.name", is("New User")))
                .andExpect(jsonPath("$.id", is(notNullValue())));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateUsername_whenSignUp_thenStatus400() throws Exception {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("testuser");
        signUp.setPassword("pass1234");
        signUp.setConfirmPassword("pass1234");
        signUp.setEmail("unique@example.com");
        signUp.setName("Dup User");

        String json = objectMapper.writeValueAsString(signUp);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateEmail_whenSignUp_thenStatus400() throws Exception {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("brandnew");
        signUp.setPassword("pass1234");
        signUp.setConfirmPassword("pass1234");
        signUp.setEmail("test@example.com");
        signUp.setName("Dup Email");

        String json = objectMapper.writeValueAsString(signUp);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenTooShortUsername_whenSignUp_thenStatus400() throws Exception {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("ab");
        signUp.setPassword("pass1234");
        signUp.setConfirmPassword("pass1234");
        signUp.setEmail("ok@example.com");
        signUp.setName("Name");

        String json = objectMapper.writeValueAsString(signUp);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenInvalidEmail_whenSignUp_thenStatus400() throws Exception {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("validuser");
        signUp.setPassword("pass1234");
        signUp.setConfirmPassword("pass1234");
        signUp.setEmail("not-an-email");
        signUp.setName("Name");

        String json = objectMapper.writeValueAsString(signUp);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAuthenticated_whenGetDetail_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/detail")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.name", is("Test User")));
    }

    @Test
    public void givenUnauthenticated_whenGetDetail_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/detail")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidData_whenChangeDetail_thenStatus200() throws Exception {
        UserDetailChange detailChange = new UserDetailChange();
        detailChange.setEmail("newemail@example.com");
        detailChange.setName("New Name");

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/users/me/detail")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("newemail@example.com")))
                .andExpect(jsonPath("$.name", is("New Name")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateEmail_whenChangeDetail_thenStatus400() throws Exception {
        UserDetailChange detailChange = new UserDetailChange();
        detailChange.setEmail("admin@example.com");
        detailChange.setName("New Name");

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/users/me/detail")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenUnauthenticated_whenChangeDetail_thenStatus403() throws Exception {
        UserDetailChange detailChange = new UserDetailChange();
        detailChange.setEmail("x@example.com");
        detailChange.setName("Name");

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/users/me/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenInvalidEmail_whenChangeDetail_thenStatus400() throws Exception {
        UserDetailChange detailChange = new UserDetailChange();
        detailChange.setEmail("not-an-email");
        detailChange.setName("New Name");

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/users/me/detail")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidData_whenChangePassword_thenStatus204() throws Exception {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("password");
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenWrongOldPassword_whenChangePassword_thenStatus403() throws Exception {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("wrongpassword");
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenMismatchPasswords_whenChangePassword_thenStatus403() throws Exception {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("password");
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("different1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenChangePassword_thenStatus403() throws Exception {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("password");
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAuthenticated_whenDeleteSelf_thenStatus204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", tokenForUser(3L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void givenUnauthenticated_whenDeleteSelf_thenStatus403() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenTooShortPassword_whenSignUp_thenStatus400() throws Exception {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("validuser");
        signUp.setPassword("ab");
        signUp.setConfirmPassword("ab");
        signUp.setEmail("ok@example.com");
        signUp.setName("Valid Name");

        String json = objectMapper.writeValueAsString(signUp);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenTooShortName_whenSignUp_thenStatus400() throws Exception {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("validuser");
        signUp.setPassword("pass1234");
        signUp.setConfirmPassword("pass1234");
        signUp.setEmail("ok@example.com");
        signUp.setName("N");

        String json = objectMapper.writeValueAsString(signUp);
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenTooShortName_whenChangeDetail_thenStatus400() throws Exception {
        UserDetailChange detailChange = new UserDetailChange();
        detailChange.setEmail("ok@example.com");
        detailChange.setName("N");

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/users/me/detail")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenTooShortNewPassword_whenChangePassword_thenStatus400() throws Exception {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("password");
        passwordChange.setNewPassword("ab");
        passwordChange.setConfirmPassword("ab");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", tokenForUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}

