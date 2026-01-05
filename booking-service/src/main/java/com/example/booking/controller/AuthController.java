package com.example.booking.controller;

import com.example.booking.model.User;
import com.example.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody Map<String, Object> req) {
        String username = (String) req.get("username");
        String password = (String) req.get("password");
        boolean admin = req.getOrDefault("admin", false) instanceof Boolean b && b;
        return userService.register(username, password, admin);
    }

    @PostMapping("/signIn")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> req) {
        String token = userService.signIn(req.get("username"), req.get("password"));
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer"));
    }
}


