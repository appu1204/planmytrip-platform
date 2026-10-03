package com.planmytrip.user_service.service.impl;

import com.planmytrip.user_service.dto.ApiResponse;
import com.planmytrip.user_service.exception.BadRequestException;
import com.planmytrip.user_service.exception.ResourceNotFoundException;
import com.planmytrip.user_service.dto.*;
import com.planmytrip.user_service.entity.User;
import com.planmytrip.user_service.mapper.UserMapper;
import com.planmytrip.user_service.repository.UserRepository;
import com.planmytrip.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    // =====================================================
    // GET PROFILE
    // =====================================================
    /**
     * Fetch currently logged-in user's profile
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserResponse> getProfile(String email) {

        User user = findUser(email);

        return ApiResponse.success(
                "Profile fetched successfully",
                userMapper.toUserResponse(user)
        );
    }

    // =====================================================
    // UPDATE PROFILE
    // =====================================================
    /**
     * Update user's name and phone
     */
    @Override
    @Transactional
    public ApiResponse<UserResponse> updateProfile(String email, UpdateProfileRequest request) {

        User user = findUser(email);

        // Trim inputs to avoid unnecessary spaces
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());

        User updatedUser = userRepository.save(user);

        log.info("Profile updated for user: {}", email);

        return ApiResponse.success(
                "Profile updated successfully",
                userMapper.toUserResponse(updatedUser)
        );
    }

    // =====================================================
    // CHANGE PASSWORD
    // =====================================================
    /**
     * Change user password securely
     */
    @Override
    @Transactional
    public ApiResponse<Void> changePassword(String email, ChangePasswordRequest request) {

        User user = findUser(email);

        // 1. New password & confirm password match check
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        // 2. Prevent same password reuse
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // 3. Validate old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // 4. Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);

        return ApiResponse.success("Password changed successfully");
    }

    // =====================================================
    // UPDATE TRAVEL PERSONA
    // =====================================================
    /**
    * Update user's travel persona (drives homepage theme)
    */
    @Override
    @Transactional
    public ApiResponse<UserResponse> updatePersona(String email, UpdatePersonaRequest request) {

        User user = findUser(email);

        user.setTravelPersona(request.getPersona());

        User updatedUser = userRepository.save(user);

        log.info("Travel persona updated to {} for user: {}", request.getPersona(), email);

        return ApiResponse.success(
                "Travel persona updated successfully",
                userMapper.toUserResponse(updatedUser)
        );
    }

    // =====================================================
    // HELPER METHOD
    // =====================================================
    /**
     * Fetch user by email or throw exception
     */
    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with email: " + email)
                );
    }
}