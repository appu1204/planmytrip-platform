package com.planmytrip.user_service.service.impl;

import com.planmytrip.user_service.dto.ApiResponse;
import com.planmytrip.user_service.dto.ResendOtpRequest;
import com.planmytrip.user_service.entity.LoginOtpToken;
import com.planmytrip.user_service.entity.User;
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
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 5);
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
}
