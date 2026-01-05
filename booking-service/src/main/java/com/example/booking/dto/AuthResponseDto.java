package com.example.booking.dto;

public record AuthResponseDto(
        String token,
        String username,
        String role
) {
}
