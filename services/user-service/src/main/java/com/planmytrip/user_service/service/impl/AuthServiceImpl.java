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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.token.otp-expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

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
        sendOtpEmail(user.getEmail(), user.getFullName(), otp);

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
        String token = jwtUtil.generateToken(userDetails);

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

        String link = baseUrl + "/auth/reset-password?token=" + token;
        sendEmail(user.getEmail(), "Reset Password", "Click here: " + link);
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
        String link = baseUrl + "/auth/verify-email?token=" + UUID.randomUUID();
        sendEmail(user.getEmail(), "Verify Email", "Click here: " + link);
    }

    // ─────────────────────────────────────────────────────────────────
    // WHY INLINE IMAGE (CID)?
    //   Gmail strips <svg> completely from email bodies.
    //   External <img src="https://..."> images are blocked by default.
    //   Embedding the PNG as a CID inline attachment is the only way to
    //   guarantee the header renders in Gmail, Outlook, and all clients.
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sends a beautiful HTML OTP email.
     * The header PNG is attached inline as  cid:header-image
     * so it renders in Gmail without any "show images" prompt.
     */
    private void sendOtpEmail(String email, String name, String otp) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("JavaMailSender not configured; skipping OTP email to {}", email);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();

            // multipart = true  →  required for inline images
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PlanMyTrip");   // inbox shows: PlanMyTrip <noreply@...>
            helper.setTo(email);
            helper.setSubject("Verify Your Login - PlanMyTrip");
            helper.setText(buildOtpHtml(name, otp), true);   // true = HTML

            // Inline PNG  — HTML references it as  src="cid:header-image"
            ClassPathResource headerImg =
                    new ClassPathResource("static/images/email-header.png");
            helper.addInline("header-image", headerImg);

            mailSender.send(message);
            log.info("OTP email sent to {}", email);

        } catch (MessagingException | IOException e) {
            log.error("Failed to send OTP email to {}", email, e);
            throw new RuntimeException("Failed to send OTP email");
        }
    }

    /**
     * Loads templates/otp-email.html and replaces placeholders:
     *   {{name}}       → user full name
     *   {{otp_digits}} → styled per-digit span boxes
     *   {{expiry}}     → value of app.token.otp-expiry-minutes
     */
    private String buildOtpHtml(String name, String otp) throws IOException {
        ClassPathResource res = new ClassPathResource("templates/otp-email.html");
        String html = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        StringBuilder digitBoxes = new StringBuilder();
        for (char digit : otp.toCharArray()) {
            digitBoxes.append(
                "<span style='display:inline-block;width:52px;height:64px;" +
                "line-height:64px;text-align:center;font-size:30px;font-weight:800;" +
                "color:#1a3fcc;background:#ffffff;border-radius:10px;margin:0 5px;" +
                "box-shadow:0 2px 8px rgba(26,63,204,0.12);'>"
                + digit + "</span>"
            );
        }

        return html
                .replace("{{name}}",       name)
                .replace("{{otp_digits}}", digitBoxes.toString())
                .replace("{{expiry}}",     String.valueOf(otpExpiryMinutes));
    }

    /** Plain-text email — used for verify-email and reset-password links only */
    private void sendEmail(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender not configured; skipping email to {} (subject={})", to, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}