package com.skillswap.controller;

import com.skillswap.dto.*;
import com.skillswap.entity.User;
import com.skillswap.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        userService.saveUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = userService.loginUser(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MessageResponse> getCurrentUser(
            Authentication authentication) {

        return ResponseEntity.ok(
                new MessageResponse(
                        "Authenticated as: " + authentication.getName()
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        String currentEmail = authentication.getName();

        UserResponse updatedUser =
                userService.updateProfile(currentEmail, request);

        return ResponseEntity.ok(updatedUser);
    }

}