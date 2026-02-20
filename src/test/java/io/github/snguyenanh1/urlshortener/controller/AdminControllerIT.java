package io.github.snguyenanh1.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.snguyenanh1.urlshortener.UrlShortenerApplication;
import io.github.snguyenanh1.urlshortener.dto.user.AdminUserCreate;
import io.github.snguyenanh1.urlshortener.dto.user.AdminUserDetailChange;
import io.github.snguyenanh1.urlshortener.dto.user.AdminUserPasswordChange;
import io.github.snguyenanh1.urlshortener.enums.UserRole;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = UrlShortenerApplication.class
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AdminControllerIT {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    @Autowired
    public AdminControllerIT(MockMvc mockMvc, ObjectMapper objectMapper, JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    private String adminToken() {
        return "Bearer " + jwtService.generateToken(2L);
    }

    private String userToken() {
        return "Bearer " + jwtService.generateToken(1L);
    }


    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenValidData_whenAdminCreatesUser_thenStatus200() throws Exception {
        AdminUserCreate create = new AdminUserCreate();
        create.setUsername("newadminuser");
        create.setPassword("pass1234");
        create.setEmail("newadmin@example.com");
        create.setName("New Admin User");
        create.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(create);
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("newadminuser")))
                .andExpect(jsonPath("$.email", is("newadmin@example.com")))
                .andExpect(jsonPath("$.name", is("New Admin User")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateUsername_whenAdminCreatesUser_thenStatus400() throws Exception {
        AdminUserCreate create = new AdminUserCreate();
        create.setUsername("testuser");
        create.setPassword("pass1234");
        create.setEmail("unique@example.com");
        create.setName("Dup User");
        create.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(create);
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenDuplicateEmail_whenAdminCreatesUser_thenStatus400() throws Exception {
        AdminUserCreate create = new AdminUserCreate();
        create.setUsername("brandnew");
        create.setPassword("pass1234");
        create.setEmail("test@example.com");
        create.setName("Dup Email");
        create.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(create);
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenCreatesUser_thenStatus403() throws Exception {
        AdminUserCreate create = new AdminUserCreate();
        create.setUsername("newuser");
        create.setPassword("pass1234");
        create.setEmail("new@example.com");
        create.setName("New User");
        create.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(create);
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenUnauthenticated_whenCreatesUser_thenStatus403() throws Exception {
        AdminUserCreate create = new AdminUserCreate();
        create.setUsername("newuser");
        create.setPassword("pass1234");
        create.setEmail("new@example.com");
        create.setName("New User");
        create.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(create);
        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenGetAllUsers_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenGetAllUsers_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenUnauthenticated_whenGetAllUsers_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenGetUserById_thenStatus200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/1")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenGetNonExistentUser_thenStatus500() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/99999")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenGetUserById_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/1")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeUserDetail_thenStatus200() throws Exception {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("updateduser");
        detailChange.setEmail("updated@example.com");
        detailChange.setName("Updated User");
        detailChange.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/admin/users/1/detail")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("updateduser")))
                .andExpect(jsonPath("$.email", is("updated@example.com")))
                .andExpect(jsonPath("$.name", is("Updated User")));
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeDuplicateUsername_thenStatus400() throws Exception {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("adminuser");
        detailChange.setEmail("unique@example.com");
        detailChange.setName("Updated");
        detailChange.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/admin/users/1/detail")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeDuplicateEmail_thenStatus400() throws Exception {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("uniquename");
        detailChange.setEmail("admin@example.com");
        detailChange.setName("Updated");
        detailChange.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/admin/users/1/detail")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeNonExistentUser_thenStatus404() throws Exception {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("uniquename");
        detailChange.setEmail("unique@example.com");
        detailChange.setName("Updated");
        detailChange.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/admin/users/99999/detail")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenChangeUserDetail_thenStatus403() throws Exception {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("updateduser");
        detailChange.setEmail("updated@example.com");
        detailChange.setName("Updated");
        detailChange.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/admin/users/3/detail")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeUserPassword_thenStatus204() throws Exception {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/admin/users/1/password")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangePasswordMismatch_thenStatus403() throws Exception {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("different1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/admin/users/1/password")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeNonExistentUserPassword_thenStatus404() throws Exception {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/admin/users/99999/password")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenChangeUserPassword_thenStatus403() throws Exception {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/admin/users/3/password")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenDeleteUser_thenStatus204() throws Exception {
        // Delete user 3 (otheruser) who has no urls — avoids FK issues
        mockMvc.perform(delete("/api/v1/admin/users/3")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenDeleteSelf_thenStatus204() throws Exception {
        // Admin (id=2) has no urls, safe to delete
        mockMvc.perform(delete("/api/v1/admin/users/2")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenDeleteNonExistentUser_thenStatus404() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/99999")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenNonAdmin_whenDeleteUser_thenStatus403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/3")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenUnauthenticated_whenDeleteUser_thenStatus403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenGetUserById_thenStatus403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenChangeUserDetail_thenStatus403() throws Exception {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("updateduser");
        detailChange.setEmail("updated@example.com");
        detailChange.setName("Updated");
        detailChange.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/admin/users/1/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    public void givenUnauthenticated_whenChangeUserPassword_thenStatus403() throws Exception {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/admin/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeAnotherAdminDetail_thenStatus403() throws Exception {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("uniquename");
        detailChange.setEmail("unique@example.com");
        detailChange.setName("Updated");
        detailChange.setRole(UserRole.ADMIN);

        String json = objectMapper.writeValueAsString(detailChange);
        mockMvc.perform(patch("/api/v1/admin/users/4/detail")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenChangeAnotherAdminPassword_thenStatus403() throws Exception {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newpass1234");
        passwordChange.setConfirmPassword("newpass1234");

        String json = objectMapper.writeValueAsString(passwordChange);
        mockMvc.perform(patch("/api/v1/admin/users/4/password")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenAdmin_whenDeleteAnotherAdmin_thenStatus403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/4")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenTooShortUsername_whenAdminCreatesUser_thenStatus400() throws Exception {
        AdminUserCreate create = new AdminUserCreate();
        create.setUsername("ab");
        create.setPassword("pass1234");
        create.setEmail("ok@example.com");
        create.setName("Valid Name");
        create.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(create);
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Sql(scripts = "classpath:data-test.sql")
    public void givenInvalidEmail_whenAdminCreatesUser_thenStatus400() throws Exception {
        AdminUserCreate create = new AdminUserCreate();
        create.setUsername("validuser");
        create.setPassword("pass1234");
        create.setEmail("not-an-email");
        create.setName("Valid Name");
        create.setRole(UserRole.USER);

        String json = objectMapper.writeValueAsString(create);
        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}

