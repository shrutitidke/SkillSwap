package com.skillswap.controller;

import com.skillswap.dto.AdminUserDTO;
import com.skillswap.dto.AdminMessageCreateDTO;
import com.skillswap.dto.MessageResponse;
import com.skillswap.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/test")
    public ResponseEntity<MessageResponse> adminTest() {

        return ResponseEntity.ok(
                new MessageResponse("Admin access granted")
        );
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(
            @PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(
                new MessageResponse("User and related records deleted"));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendAdminMessage(
            @Valid @RequestBody AdminMessageCreateDTO request) {
        int recipientCount = adminService.sendAdminMessage(request);
        return ResponseEntity.ok(new MessageResponse(
                "Message delivered to " + recipientCount
                        + (recipientCount == 1 ? " user" : " users")));
    }

}
