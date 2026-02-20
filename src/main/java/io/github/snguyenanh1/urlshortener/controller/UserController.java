package io.github.snguyenanh1.urlshortener.controller;

import io.github.snguyenanh1.urlshortener.dto.user.UserDetailChange;
import io.github.snguyenanh1.urlshortener.dto.user.UserPasswordChange;
import io.github.snguyenanh1.urlshortener.dto.user.UserResponse;
import io.github.snguyenanh1.urlshortener.dto.user.UserSignUp;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping()
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody UserSignUp userSignUp) {
        UserResponse userResponse = userService.signUp(userSignUp);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/me/detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getDetail(@AuthenticationPrincipal User user) {
        UserResponse userResponse = userService.getUserById(user.getId());
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/me/detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> changeDetail(@AuthenticationPrincipal User user,
                                                     @Valid @RequestBody UserDetailChange userDetailChange) {
        UserResponse userResponse = userService.changeDetail(user, userDetailChange);
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody UserPasswordChange userPasswordChange) {
        userService.changePassword(user, userPasswordChange);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteUser(user);
        return ResponseEntity.noContent().build();
    }


}
