package com.planmytrip.user_service.service.impl;

import com.planmytrip.user_service.dto.ApiResponse;
import com.planmytrip.user_service.exception.*;
import com.planmytrip.user_service.security.*;
import com.planmytrip.user_service.dto.*;
import com.planmytrip.user_service.entity.*;
import com.planmytrip.user_service.enums.Provider;
import com.planmytrip.user_service.enums.Role;
import com.planmytrip.user_service.mapper.UserMapper;
import com.planmytrip.user_service.repository.*;
import com.planmytrip.user_service.service.AuthService;
import com.planmytrip.user_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final LoginOtpTokenRepository loginOtpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.token.otp-expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.token.email-verification-expiry-hours}")
    private int emailVerificationExpiryHours;

    @Override
    @Transactional
    public ApiResponse<Void> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new BadRequestException("Email already registered");
        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw new BadRequestException("Passwords do not match");

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER).provider(Provider.LOCAL)
                .isVerified(false).isActive(true).failedLoginAttempts(0)
                .build();

        userRepository.save(user);
        sendVerificationEmail(user);
        log.info("User registered: {}", user.getEmail());
        return ApiResponse.success("Registration successful. Please verify your email.");
    }

    @Override
    @Transactional
    public ApiResponse<Void> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new BadRequestException("Invalid credentials");

        loginOtpTokenRepository.deleteAllByUserId(user.getId());

        String otp = generateOtp();
        saveOtp(user, otp);
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        return ApiResponse.success("OTP sent successfully");
    }

    @Override
    @Transactional
    public ApiResponse<AuthResponse> verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LoginOtpToken otpToken = loginOtpTokenRepository
                .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new BadRequestException("OTP not found"));

        if (otpToken.isExpired())  throw new TokenExpiredException("OTP expired");
        if (!otpToken.getOtp().equals(request.getOtp())) throw new BadRequestException("Invalid OTP");

        otpToken.setUsed(true);
        loginOtpTokenRepository.save(otpToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());

        String token = jwtUtil.generateToken(claims, userDetails);

        return ApiResponse.success("Login successful",
                AuthResponse.of(token, userMapper.toUserResponse(user)));
    }

    @Override
    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user).token(token)
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .used(false).build();
        passwordResetTokenRepository.save(resetToken);

        String link = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPlainEmail(user.getEmail(), "Reset Password", "Click here: " + link);
        return ApiResponse.success("Reset link sent");
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new BadRequestException("Passwords do not match");

        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid token"));

        if (token.getUsed())    throw new BadRequestException("Token already used");
        if (token.isExpired())  throw new TokenExpiredException("Token expired");

        User user = token.getUser();
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword()))
            throw new BadRequestException("New password must be different");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        return ApiResponse.success("Password reset successful");
    }

    @Override
    @Transactional
    public ApiResponse<Void> verifyEmail(String token) {
        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByToken(token)
                        .orElseThrow(() -> new BadRequestException("Invalid token"));

        if (verificationToken.getUsed())    throw new BadRequestException("Token already used");
        if (verificationToken.isExpired())  throw new TokenExpiredException("Token expired");

        User user = verificationToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
        return ApiResponse.success("Email verified successfully");
    }

    @Override
    @Transactional
    public ApiResponse<Void> resendVerification(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("Email already verified");
        }

        sendVerificationEmail(user);
        return ApiResponse.success("Verification link sent");
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private void saveOtp(User user, String otp) {
        LoginOtpToken token = LoginOtpToken.builder()
                .user(user).otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false).build();
        loginOtpTokenRepository.save(token);
    }

    private void sendVerificationEmail(User user) {
        emailVerificationTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiryTime(LocalDateTime.now().plusHours(emailVerificationExpiryHours))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        String link = baseUrl + "/auth/verify-email?token=" + token;
        emailService.sendPlainEmail(user.getEmail(), "Verify Email", "Click here: " + link);
    }
}