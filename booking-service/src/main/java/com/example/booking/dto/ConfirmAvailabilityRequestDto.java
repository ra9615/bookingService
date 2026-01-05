package com.example.booking.dto;

import java.time.LocalDate;

public record ConfirmAvailabilityRequestDto(
        String requestId,
        LocalDate startDate,
        LocalDate endDate
) {
}
