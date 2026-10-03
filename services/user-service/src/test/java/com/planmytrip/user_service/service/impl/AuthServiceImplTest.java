package com.planmytrip.user_service.service.impl;

import com.planmytrip.user_service.dto.ApiResponse;
import com.planmytrip.user_service.dto.LoginRequest;
import com.planmytrip.user_service.dto.ResendOtpRequest;
import com.planmytrip.user_service.entity.LoginOtpToken;
import com.planmytrip.user_service.entity.User;
import com.planmytrip.user_service.exception.AccountLockedException;
import com.planmytrip.user_service.exception.BadRequestException;
import com.planmytrip.user_service.exception.ResourceNotFoundException;
import com.planmytrip.user_service.repository.LoginOtpTokenRepository;
import com.planmytrip.user_service.repository.UserRepository;
import com.planmytrip.user_service.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginOtpTokenRepository loginOtpTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockDurationMinutes", 30);
    }

    @Test
    void resendOtp_ShouldSucceed_WhenUserExists() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("TEST@EXAMPLE.COM ");

        ApiResponse<Void> response = authService.resendOtp(request);

        assertTrue(response.isSuccess());
        assertEquals("OTP sent successfully", response.getMessage());

        verify(loginOtpTokenRepository).deleteAllByUserId(1L);
        verify(loginOtpTokenRepository).save(any(LoginOtpToken.class));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("test@example.com"), eq("Test User"), otpCaptor.capture());
        assertNotNull(otpCaptor.getValue());
        assertEquals(6, otpCaptor.getValue().length());
    }

    @Test
    void resendOtp_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("unknown@example.com");

        assertThrows(ResourceNotFoundException.class, () -> authService.resendOtp(request));
        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    void login_ShouldSucceed_WhenCredentialsAreValid() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encodedPassword")
                .fullName("John Doe")
                .isActive(true)
                .isVerified(true)
                .failedLoginAttempts(0)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("USER@EXAMPLE.COM ");
        request.setPassword("rawPassword");

        ApiResponse<Void> response = authService.login(request);

        assertTrue(response.isSuccess());
        assertEquals("OTP sent successfully", response.getMessage());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockUntil());
        assertNotNull(user.getLastLogin());

        verify(userRepository, atLeastOnce()).save(user);
        verify(loginOtpTokenRepository).deleteAllByUserId(1L);
        verify(loginOtpTokenRepository).save(any(LoginOtpToken.class));
        verify(emailService).sendOtpEmail(eq("user@example.com"), eq("John Doe"), any());
    }

    @Test
    void login_ShouldIncrementFailedAttempts_WhenPasswordIsIncorrect() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encodedPassword")
                .isActive(true)
                .isVerified(true)
                .failedLoginAttempts(2)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrongPassword");

        assertThrows(BadRequestException.class, () -> authService.login(request));
        assertEquals(3, user.getFailedLoginAttempts());
        assertNull(user.getLockUntil());
        verify(userRepository).save(user);
    }

    @Test
    void login_ShouldLockAccount_WhenFailedAttemptsExceedMax() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encodedPassword")
                .isActive(true)
                .isVerified(true)
                .failedLoginAttempts(4)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrongPassword");

        assertThrows(AccountLockedException.class, () -> authService.login(request));
        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockUntil());
        assertTrue(user.getLockUntil().isAfter(LocalDateTime.now()));
        verify(userRepository).save(user);
    }

    @Test
    void login_ShouldThrowAccountLockedException_WhenAccountAlreadyLocked() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .lockUntil(LocalDateTime.now().plusMinutes(20))
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");

        assertThrows(AccountLockedException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_ShouldThrowBadRequest_WhenAccountDeactivated() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .isActive(false)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    void login_ShouldThrowBadRequest_WhenEmailNotVerified() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .isActive(true)
                .isVerified(false)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }
}
