package com.example.booking.controller;

import com.example.booking.model.Booking;
import com.example.booking.repository.BookingRepository;
import com.example.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-jwt")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    @PostMapping
    public Booking createBooking(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> request) {
        Long userId = Long.valueOf(jwt.getSubject());
        Long roomId = Long.valueOf(request.get("roomId"));
        LocalDate startDate = LocalDate.parse(request.get("startDate"));
        LocalDate endDate = LocalDate.parse(request.get("endDate"));
        String requestId = request.get("requestId");

        return bookingService.create(userId, roomId, startDate, endDate, requestId);
    }

    @GetMapping
    public List<Booking> getUserBookings(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return bookingRepository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Booking getBookingById(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        return bookingService.getBooking(id, username);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Booking>> getAllBookings(@AuthenticationPrincipal Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        if ("ADMIN".equals(scope)) {
            List<Booking> bookings = bookingRepository.findAll();
            return ResponseEntity.ok(bookings);
        } else {
            return ResponseEntity.status(403).build();
        }
    }
}
