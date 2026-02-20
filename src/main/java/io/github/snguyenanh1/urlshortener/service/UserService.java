package io.github.snguyenanh1.urlshortener.service;

import io.github.snguyenanh1.urlshortener.dto.user.*;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.enums.UserRole;
import io.github.snguyenanh1.urlshortener.exception.*;
import io.github.snguyenanh1.urlshortener.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .map(UserService::mapToUserDetailResponse)
                .orElseThrow(() -> new UsernameNotFoundException("User with " + usernameOrEmail + " not found"));
    }

    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserService::mapToUserDetailResponse)
                .orElseThrow(() -> new UsernameNotFoundException("User with " + username + " not found"));
    }
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserService::mapToUserDetailResponse)
                .orElseThrow(() -> new UsernameNotFoundException("User with " + id + " not found"));
    }

    @Transactional
    public UserResponse signUp(UserSignUp userSignUp) {
        if (userRepository.existsByEmail(userSignUp.getEmail())) {
            throw new InvalidEmailException("Email address already in use");
        }
        if (userRepository.existsByUsername(userSignUp.getUsername())) {
            throw new InvalidUsernameException("Username already in use");
        }
        User user = User.builder()
                .username(userSignUp.getUsername())
                .password(passwordEncoder.encode(userSignUp.getPassword()))
                .email(userSignUp.getEmail())
                .name(userSignUp.getName())
                .role(UserRole.USER)
                .build();
        user = userRepository.save(user);
        return mapToUserDetailResponse(user);
    }

    @Transactional
    public UserResponse changeDetail(@AuthenticationPrincipal User user, UserDetailChange userDetailChange) {
        if (userRepository.existsByEmailAndIdNot(userDetailChange.getEmail(), user.getId())) {
            throw new InvalidEmailException("Email address already in use");
        }
        user.setName(userDetailChange.getName());
        user.setEmail(userDetailChange.getEmail());
        user = userRepository.save(user);
        return mapToUserDetailResponse(user);

    }

    @Transactional
    public void changePassword(@AuthenticationPrincipal User user, UserPasswordChange userPasswordChange) {
        if (!userPasswordChange.getNewPassword().equals(userPasswordChange.getConfirmPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        }
        if (!passwordEncoder.matches(userPasswordChange.getOldPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Wrong password");
        }
        user.setPassword(passwordEncoder.encode(userPasswordChange.getNewPassword()));
        user = userRepository.save(user);
    }

    @Transactional
    public void deleteUser(@AuthenticationPrincipal User user) {
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse createAccount(AdminUserCreate adminUserCreate) {
        if (userRepository.existsByUsername(adminUserCreate.getUsername())) {
            throw new InvalidUsernameException("Username already in use");
        }
        if (userRepository.existsByEmail(adminUserCreate.getEmail())) {
            throw new InvalidEmailException("Email address already in use");
        }
        User user = User.builder()
                .username(adminUserCreate.getUsername())
                .password(passwordEncoder.encode(adminUserCreate.getPassword()))
                .email(adminUserCreate.getEmail())
                .name(adminUserCreate.getName())
                .role(adminUserCreate.getRole())
                .build();
        user = userRepository.save(user);
        return mapToUserDetailResponse(user);
    }

    @Transactional
    public UserResponse changeAccountDetail(@AuthenticationPrincipal User user,
                                            Long userId, AdminUserDetailChange adminUserDetailChange) {
        if (userRepository.existsByUsername(adminUserDetailChange.getUsername())) {
            throw new InvalidUsernameException("Username already in use");
        }
        if (userRepository.existsByEmail(adminUserDetailChange.getEmail())) {
            throw new InvalidEmailException("Email address already in use");
        }
        User toBeChanged = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (user.getId().equals(userId) ) {
            if(!user.getRole().equals(toBeChanged.getRole())) {
                throw new AccessDeniedException("Cannot change your own role");
            }
        } else {
            if (toBeChanged.getRole().equals(UserRole.ADMIN)) {
                throw new AccessDeniedException("Cannot change another admin account detail");
            }
        }
        toBeChanged.setUsername(adminUserDetailChange.getUsername());
        toBeChanged.setEmail(adminUserDetailChange.getEmail());
        toBeChanged.setName(adminUserDetailChange.getName());
        toBeChanged.setRole(adminUserDetailChange.getRole());
        toBeChanged = userRepository.save(toBeChanged);
        return mapToUserDetailResponse(toBeChanged);
    }

    @Transactional
    public void changeAccountPassword(@AuthenticationPrincipal User user, Long userId,
                                              AdminUserPasswordChange adminUserPasswordChange) {
        User toBeChanged = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if(!user.getId().equals(userId) && toBeChanged.getRole().equals(UserRole.ADMIN)) {
            throw new AccessDeniedException("Cannot change another admin account password");
        }
        if (!adminUserPasswordChange.getNewPassword().equals(adminUserPasswordChange.getConfirmPassword())) {
            throw new InvalidPasswordException("Passwords do not match");
        }
        toBeChanged.setPassword(passwordEncoder.encode(adminUserPasswordChange.getNewPassword()));
        toBeChanged = userRepository.save(toBeChanged);
    }

    @Transactional
    public void deleteAccount(@AuthenticationPrincipal User user, Long userId) {
        User toBeDeleted = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!user.getId().equals(userId) && toBeDeleted.getRole().equals(UserRole.ADMIN)) {
            throw new AccessDeniedException("Cannot delete another admin account");
        }
        userRepository.delete(toBeDeleted);
    }

    public Page<UserResponse> getAll(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(UserService::mapToUserDetailResponse);
    }

    private static UserResponse mapToUserDetailResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

}
