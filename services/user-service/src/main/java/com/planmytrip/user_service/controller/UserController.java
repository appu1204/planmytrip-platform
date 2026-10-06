package com.planmytrip.user_service.controller;

import com.planmytrip.user_service.dto.ApiResponse;
import com.planmytrip.user_service.dto.*;
import com.planmytrip.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Handles user profile operations
 * UserController
 *
 * Handles all authenticated user operations such as:
 * - Fetch profile
 * - Update profile
 * - Change password
 *
 * Base URL: /users
 */

@RestController
@RequestMapping({"/users", ""})
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     *
     * Fetch currently logged-in user's profile.
     *
     * Flow:
     * - Extract email from JWT (Spring Security)
     * - Fetch user from DB
     * - Return mapped UserResponse
     *
     * @param userDetails Authenticated user (from Spring Security)
     * @return ApiResponse<UserResponse>
     */

    @GetMapping({"/me", "/profile", "/users/me"})
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Unauthorized"));
        }

        return ResponseEntity.ok(
                userService.getProfile(userDetails.getUsername())
        );
    }

    /**
     * PUT /api/users/me
     *
     * Update user's profile details (name + phone).
     *
     * Flow:
     * - Validate request body
     * - Fetch user by email
     * - Update fields
     * - Save and return updated data
     *
     * @param userDetails Authenticated user
     * @param request UpdateProfileRequest
     * @return ApiResponse<UserResponse>
     */

    @PutMapping({"/me", "/profile", "/users/me"})
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Unauthorized"));
        }

        return ResponseEntity.ok(
                userService.updateProfile(userDetails.getUsername(), request)
        );
    }

    /**
     * PUT /api/users/me/change-password
     *
     * Change user's password securely.
     *
     * Flow:
     * - Validate old password
     * - Validate new password & confirm password
     * - Encrypt new password
     * - Save in DB
     *
     * @param userDetails Authenticated user
     * @param request ChangePasswordRequest
     * @return ApiResponse<Void>
     */

    @PutMapping({"/me/change-password", "/change-password", "/users/me/change-password"})
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Unauthorized"));
        }

        return ResponseEntity.ok(
                userService.changePassword(userDetails.getUsername(), request)
        );
    }

    /**
     * GET /api/users/me/persona or /api/user/persona
     *
     * Get user's travel persona.
     */
    @GetMapping({"/me/persona", "/persona", "/users/me/persona"})
    public ResponseEntity<ApiResponse<UserResponse>> getPersona(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Unauthorized"));
        }

        return ResponseEntity.ok(
                userService.getProfile(userDetails.getUsername())
        );
    }

    /**
     * PUT /api/users/me/persona or /api/user/persona
     *
     * Update user's travel persona (drives homepage theme).
     *
     * Flow:
     * - Extract email from JWT (Spring Security)
     * - Validate persona value (SOLO, COUPLE, FRIENDS, FAMILY)
     * - Save to DB
     * - Frontend uses response to switch theme
     *
     * @param userDetails Authenticated user
     * @param request UpdatePersonaRequest
     * @return ApiResponse<UserResponse>
     */
    @PutMapping({"/me/persona", "/persona", "/users/me/persona"})
    public ResponseEntity<ApiResponse<UserResponse>> updatePersona(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePersonaRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("Unauthorized"));
        }

        return ResponseEntity.ok(
                userService.updatePersona(userDetails.getUsername(), request)
        );
    }
}