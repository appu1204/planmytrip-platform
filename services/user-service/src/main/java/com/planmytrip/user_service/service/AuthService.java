package com.planmytrip.user_service.service;

import com.planmytrip.user_service.dto.ApiResponse;
import com.planmytrip.user_service.dto.*;

public interface AuthService {

    ApiResponse<Void> register(RegisterRequest request);

    ApiResponse<Void> login(LoginRequest request);

    ApiResponse<AuthResponse> verifyOtp(VerifyOtpRequest request);

    ApiResponse<Void> forgotPassword(ForgotPasswordRequest request);

    ApiResponse<Void> resetPassword(ResetPasswordRequest request);

    ApiResponse<Void> verifyEmail(String token);

    ApiResponse<Void> resendVerification(ForgotPasswordRequest request);

    ApiResponse<Void> resendOtp(ResendOtpRequest request);
}