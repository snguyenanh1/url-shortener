package io.github.snguyenanh1.urlshortener.service;

import io.github.snguyenanh1.urlshortener.dto.user.*;
import io.github.snguyenanh1.urlshortener.entity.User;
import io.github.snguyenanh1.urlshortener.enums.UserRole;
import io.github.snguyenanh1.urlshortener.exception.*;
import io.github.snguyenanh1.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;
    private User targetUser;
    private User anotherAdmin;

    @BeforeEach
    public void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .name("Test User")
                .email("test@example.com")
                .role(UserRole.USER)
                .build();
        adminUser = User.builder()
                .id(10L).username("admin").password("encoded").name("Admin")
                .email("admin@example.com").role(UserRole.ADMIN).build();
        targetUser = User.builder()
                .id(2L).username("target").password("encoded").name("Target")
                .email("target@example.com").role(UserRole.USER).build();
        anotherAdmin = User.builder()
                .id(3L).username("admin2").password("encoded").name("Admin2")
                .email("admin2@example.com").role(UserRole.ADMIN).build();
    }


    @Test
    public void getUserByUsernameOrEmail_ExistingUser_ReturnUserResponse() {
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserByUsernameOrEmail("testuser");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");
    }

    @Test
    public void getUserByUsernameOrEmail_NotFound_ThrowUsernameNotFoundException() {
        when(userRepository.findByUsernameOrEmail("unknown", "unknown"))
                .thenReturn(Optional.empty());

        Exception e = assertThrows(UsernameNotFoundException.class,
                () -> userService.getUserByUsernameOrEmail("unknown"));
        assertThat(e.getMessage()).contains("unknown");
    }

    @Test
    public void signUp_ValidData_ReturnUserResponse() {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("newuser");
        signUp.setPassword("pass1234");
        signUp.setEmail("new@example.com");
        signUp.setName("New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User saved = i.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        UserResponse response = userService.signUp(signUp);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getName()).isEqualTo("New User");
    }

    @Test
    public void signUp_DuplicateEmail_ThrowInvalidEmailException() {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("newuser");
        signUp.setPassword("pass1234");
        signUp.setEmail("existing@example.com");
        signUp.setName("New User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        Exception e = assertThrows(InvalidEmailException.class, () -> userService.signUp(signUp));
        assertThat(e.getMessage()).isEqualTo("Email address already in use");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void signUp_DuplicateUsername_ThrowInvalidUsernameException() {
        UserSignUp signUp = new UserSignUp();
        signUp.setUsername("existinguser");
        signUp.setPassword("pass1234");
        signUp.setEmail("new@example.com");
        signUp.setName("New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        Exception e = assertThrows(InvalidUsernameException.class, () -> userService.signUp(signUp));
        assertThat(e.getMessage()).isEqualTo("Username already in use");
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    public void changeDetail_ValidData_ReturnUserResponse() {
        UserDetailChange detailChange = new UserDetailChange();
        detailChange.setEmail("updated@example.com");
        detailChange.setName("Updated Name");

        when(userRepository.existsByEmailAndIdNot("updated@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.changeDetail(testUser, detailChange);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("updated@example.com");
        assertThat(response.getName()).isEqualTo("Updated Name");
    }

    @Test
    public void changeDetail_DuplicateEmail_ThrowInvalidEmailException() {
        UserDetailChange detailChange = new UserDetailChange();
        detailChange.setEmail("taken@example.com");
        detailChange.setName("Updated Name");

        when(userRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        Exception e = assertThrows(InvalidEmailException.class,
                () -> userService.changeDetail(testUser, detailChange));
        assertThat(e.getMessage()).isEqualTo("Email address already in use");
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    public void changePassword_ValidData_ReturnUserResponse() {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("oldPass");
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("newPass1");

        when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass1")).thenReturn("encodedNewPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.changePassword(testUser, passwordChange);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    public void changePassword_PasswordsDoNotMatch_ThrowInvalidPasswordException() {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("oldPass");
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("differentPass");

        Exception e = assertThrows(InvalidPasswordException.class,
                () -> userService.changePassword(testUser, passwordChange));
        assertThat(e.getMessage()).isEqualTo("Passwords do not match");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void changePassword_WrongOldPassword_ThrowInvalidPasswordException() {
        UserPasswordChange passwordChange = new UserPasswordChange();
        passwordChange.setOldPassword("wrongOld");
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("newPass1");

        when(passwordEncoder.matches("wrongOld", "encodedPassword")).thenReturn(false);

        Exception e = assertThrows(InvalidPasswordException.class,
                () -> userService.changePassword(testUser, passwordChange));
        assertThat(e.getMessage()).isEqualTo("Wrong password");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void deleteUser_ValidUser_DeleteSuccessfully() {
        userService.deleteUser(testUser);

        verify(userRepository, times(1)).delete(testUser);
    }


    @Test
    public void createAccount_ValidData_ReturnUserResponse() {
        AdminUserCreate adminCreate = new AdminUserCreate();
        adminCreate.setUsername("adminCreated");
        adminCreate.setPassword("pass1234");
        adminCreate.setEmail("admin-created@example.com");
        adminCreate.setName("Admin Created");
        adminCreate.setRole(UserRole.USER);

        when(userRepository.existsByUsername("adminCreated")).thenReturn(false);
        when(userRepository.existsByEmail("admin-created@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User saved = i.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        UserResponse response = userService.createAccount(adminCreate);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("adminCreated");
        assertThat(response.getEmail()).isEqualTo("admin-created@example.com");
    }

    @Test
    public void createAccount_DuplicateUsername_ThrowInvalidUsernameException() {
        AdminUserCreate adminCreate = new AdminUserCreate();
        adminCreate.setUsername("existinguser");
        adminCreate.setPassword("pass1234");
        adminCreate.setEmail("new@example.com");
        adminCreate.setName("Name");
        adminCreate.setRole(UserRole.USER);

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        Exception e = assertThrows(InvalidUsernameException.class,
                () -> userService.createAccount(adminCreate));
        assertThat(e.getMessage()).isEqualTo("Username already in use");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void createAccount_DuplicateEmail_ThrowInvalidEmailException() {
        AdminUserCreate adminCreate = new AdminUserCreate();
        adminCreate.setUsername("newuser");
        adminCreate.setPassword("pass1234");
        adminCreate.setEmail("existing@example.com");
        adminCreate.setName("Name");
        adminCreate.setRole(UserRole.USER);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        Exception e = assertThrows(InvalidEmailException.class,
                () -> userService.createAccount(adminCreate));
        assertThat(e.getMessage()).isEqualTo("Email address already in use");
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    public void changeAccountDetail_ValidData_ReturnUserResponse() {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("updatedTarget");
        detailChange.setEmail("updatedTarget@example.com");
        detailChange.setName("Updated Target");
        detailChange.setRole(UserRole.USER);

        when(userRepository.existsByUsername("updatedTarget")).thenReturn(false);
        when(userRepository.existsByEmail("updatedTarget@example.com")).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.changeAccountDetail(adminUser, 2L, detailChange);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("updatedTarget");
        assertThat(response.getEmail()).isEqualTo("updatedTarget@example.com");
    }

    @Test
    public void changeAccountDetail_CalledByNonAdmin_StillUpdatesTarget() {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("updatedTarget");
        detailChange.setEmail("updatedTarget@example.com");
        detailChange.setName("Updated Target");
        detailChange.setRole(UserRole.USER);

        when(userRepository.existsByUsername("updatedTarget")).thenReturn(false);
        when(userRepository.existsByEmail("updatedTarget@example.com")).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.changeAccountDetail(testUser, 2L, detailChange);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("updatedTarget");
    }

    @Test
    public void changeAccountDetail_UserNotFound_ThrowUserNotFoundException() {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("newName");
        detailChange.setEmail("new@example.com");
        detailChange.setName("New");
        detailChange.setRole(UserRole.USER);

        when(userRepository.existsByUsername("newName")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(UserNotFoundException.class,
                () -> userService.changeAccountDetail(adminUser, 99L, detailChange));
        assertThat(e.getMessage()).isEqualTo("User not found");
    }

    @Test
    public void changeAccountDetail_DuplicateUsername_ThrowInvalidUsernameException() {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("existinguser");
        detailChange.setEmail("new@example.com");
        detailChange.setName("Name");
        detailChange.setRole(UserRole.USER);

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        Exception e = assertThrows(InvalidUsernameException.class,
                () -> userService.changeAccountDetail(adminUser, 2L, detailChange));
        assertThat(e.getMessage()).isEqualTo("Username already in use");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void changeAccountDetail_DuplicateEmail_ThrowInvalidEmailException() {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("newName");
        detailChange.setEmail("existing@example.com");
        detailChange.setName("Name");
        detailChange.setRole(UserRole.USER);

        when(userRepository.existsByUsername("newName")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        Exception e = assertThrows(InvalidEmailException.class,
                () -> userService.changeAccountDetail(adminUser, 2L, detailChange));
        assertThat(e.getMessage()).isEqualTo("Email address already in use");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void changeAccountDetail_ChangeAnotherAdmin_ThrowAccessDeniedException() {
        AdminUserDetailChange detailChange = new AdminUserDetailChange();
        detailChange.setUsername("changed");
        detailChange.setEmail("changed@example.com");
        detailChange.setName("Changed");
        detailChange.setRole(UserRole.ADMIN);

        when(userRepository.existsByUsername("changed")).thenReturn(false);
        when(userRepository.existsByEmail("changed@example.com")).thenReturn(false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherAdmin));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> userService.changeAccountDetail(adminUser, 3L, detailChange));
        assertThat(e.getMessage()).isEqualTo("Cannot change another admin account detail");
    }

    @Test
    public void changeAccountPassword_ValidData_ReturnUserResponse() {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("newPass1");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.encode("newPass1")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.changeAccountPassword(adminUser, 2L, passwordChange);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("target");
    }

    @Test
    public void changeAccountPassword_CalledByNonAdmin_StillUpdatesTarget() {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("newPass1");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(passwordEncoder.encode("newPass1")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.changeAccountPassword(testUser, 2L, passwordChange);

        verify(userRepository, times(1)).save(any(User.class));
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("target");
    }

    @Test
    public void changeAccountPassword_UserNotFound_ThrowUserNotFoundException() {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("newPass1");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(UserNotFoundException.class,
                () -> userService.changeAccountPassword(adminUser, 99L, passwordChange));
        assertThat(e.getMessage()).isEqualTo("User not found");
    }

    @Test
    public void changeAccountPassword_ChangeAnotherAdminPassword_ThrowAccessDeniedException() {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("newPass1");

        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherAdmin));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> userService.changeAccountPassword(adminUser, 3L, passwordChange));
        assertThat(e.getMessage()).isEqualTo("Cannot change another admin account password");
    }

    @Test
    public void changeAccountPassword_PasswordsDoNotMatch_ThrowInvalidPasswordException() {
        AdminUserPasswordChange passwordChange = new AdminUserPasswordChange();
        passwordChange.setNewPassword("newPass1");
        passwordChange.setConfirmPassword("differentPass");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        Exception e = assertThrows(InvalidPasswordException.class,
                () -> userService.changeAccountPassword(adminUser, 2L, passwordChange));
        assertThat(e.getMessage()).isEqualTo("Passwords do not match");
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    public void deleteAccount_ValidUser_DeleteSuccessfully() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        userService.deleteAccount(adminUser, 2L);

        verify(userRepository, times(1)).delete(targetUser);
    }

    @Test
    public void deleteAccount_CalledByNonAdmin_StillDeletesTarget() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        userService.deleteAccount(testUser, 2L);

        verify(userRepository, times(1)).delete(targetUser);
    }

    @Test
    public void deleteAccount_UserNotFound_ThrowUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(UserNotFoundException.class,
                () -> userService.deleteAccount(adminUser, 99L));
        assertThat(e.getMessage()).isEqualTo("User not found");
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    public void deleteAccount_DeleteAnotherAdmin_ThrowAccessDeniedException() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(anotherAdmin));

        Exception e = assertThrows(AccessDeniedException.class,
                () -> userService.deleteAccount(adminUser, 3L));
        assertThat(e.getMessage()).isEqualTo("Cannot delete another admin account");
        verify(userRepository, never()).delete(any(User.class));
    }
}

