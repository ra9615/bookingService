package com.example.booking.controller;

import com.example.booking.dto.AuthRequestDto;
import com.example.booking.dto.AuthResponseDto;
import com.example.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public AuthResponseDto register(@RequestBody AuthRequestDto request) {
        return userService.register(request);
    }

    @PostMapping("/signIn")
    public AuthResponseDto login(@RequestBody AuthRequestDto request) {
        return userService.signIn(request.username(), request.password());
    }
}


