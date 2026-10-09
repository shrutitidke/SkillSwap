package com.skillswap.controller;

import com.skillswap.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/test")
    public ResponseEntity<MessageResponse> adminTest() {

        return ResponseEntity.ok(
                new MessageResponse("Admin access granted")
        );
    }


}
