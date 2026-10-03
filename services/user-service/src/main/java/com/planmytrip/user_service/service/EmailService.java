package com.planmytrip.user_service.service;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sends mail off the HTTP thread so login/forgot-password do not wait on Gmail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.token.otp-expiry-minutes}")
    private int otpExpiryMinutes;

    @Async
    public void sendOtpEmail(String email, String name, String otp) {
        log.info("==================================================");
        log.info(">>>>> [AUTH OTP] Target: {} | Code: [{}] <<<<<", email, otp);
        log.info("==================================================");
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                log.warn("JavaMailSender not configured; OTP for {} is [{}]", email, otp);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PlanMyTrip");
            helper.setTo(email);
            helper.setSubject("Verify Your Login - PlanMyTrip");
            helper.setText(buildOtpHtml(name, otp), true);

            ClassPathResource headerImg =
                    new ClassPathResource("static/images/email-header.png");
            helper.addInline("header-image", headerImg);

            mailSender.send(message);
            log.info("OTP email sent to {}", email);
        } catch (Exception e) {
            log.warn("SMTP email delivery failed for {} (reason: {}). Test OTP is: [{}]", email, e.getMessage(), otp);
        }
    }

    @Async
    public void sendPlainEmail(String to, String subject, String body) {
        log.info("==================================================");
        log.info(">>>>> [EMAIL] To: {} | Subject: {} <<<<<", to, subject);
        log.info(">>>>> Body: {} <<<<<", body);
        log.info("==================================================");
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender not configured; skipping email to {} (subject={})", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} (subject={})", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {} (subject={}): {}", to, subject, e.getMessage());
        }
    }

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
                .replace("{{name}}", name)
                .replace("{{otp_digits}}", digitBoxes.toString())
                .replace("{{expiry}}", String.valueOf(otpExpiryMinutes));
    }
}
