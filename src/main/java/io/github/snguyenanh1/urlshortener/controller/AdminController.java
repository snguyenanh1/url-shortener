package io.github.snguyenanh1.urlshortener.controller;

import io.github.snguyenanh1.urlshortener.dto.user.AdminUserCreate;
import io.github.snguyenanh1.urlshortener.dto.user.AdminUserDetailChange;
import io.github.snguyenanh1.urlshortener.dto.user.AdminUserPasswordChange;
import io.github.snguyenanh1.urlshortener.dto.user.UserResponse;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;

    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody AdminUserCreate adminUserCreate) {
        UserResponse userResponse = userService.createAccount(adminUserCreate);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(@PageableDefault Pageable pageable) {
        Page<UserResponse> userResponsePage = userService.getAll(pageable);
        return ResponseEntity.ok(userResponsePage);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/users/{userId}/detail")
    public ResponseEntity<UserResponse> changeUserDetails(@AuthenticationPrincipal User user,
                                                          @PathVariable Long userId,
                                                          @Valid @RequestBody AdminUserDetailChange adminUserDetailChange) {
        UserResponse userResponse = userService.changeAccountDetail(user, userId, adminUserDetailChange);
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/users/{userId}/password")
    public ResponseEntity<Void> changeUserPassword(@AuthenticationPrincipal User user,
                                                   @PathVariable Long userId,
                                                   @Valid @RequestBody AdminUserPasswordChange adminUserPasswordChange) {
        userService.changeAccountPassword(user, userId, adminUserPasswordChange);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal User user, @PathVariable Long userId) {
        userService.deleteAccount(user, userId);
        return ResponseEntity.noContent().build();
    }

}
