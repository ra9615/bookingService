package com.example.booking.dto;

import java.time.LocalDate;

public record CreateBookingRequestDto(
        Long roomId,
        LocalDate startDate,
        LocalDate endDate,
        String requestId
) {
}
