package com.skillswap.controller;

import com.skillswap.dto.RegisterRequest;
import com.skillswap.entity.User;
import com.skillswap.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User registerUser(@Valid @RequestBody RegisterRequest request){
        return userService.saveUser(request);
    }
}