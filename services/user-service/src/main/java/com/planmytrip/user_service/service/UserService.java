package com.planmytrip.user_service.service;

import com.planmytrip.user_service.dto.ApiResponse;
import com.planmytrip.user_service.dto.*;

public interface UserService {

    ApiResponse<UserResponse> getProfile(String email);

    ApiResponse<UserResponse> updateProfile(String email, UpdateProfileRequest request);

    ApiResponse<Void> changePassword(String email, ChangePasswordRequest request);

    ApiResponse<UserResponse> updatePersona(String email, UpdatePersonaRequest request);
}