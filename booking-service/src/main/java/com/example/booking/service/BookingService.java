package com.example.booking.service;

import com.example.booking.client.HotelClient;
import com.example.booking.dto.ConfirmAvailabilityRequestDto;
import com.example.booking.dto.CreateBookingRequestDto;
import com.example.booking.model.Booking;
import com.example.booking.repository.BookingRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final BookingRepository repository;
    private final HotelClient hotelClient;

    @Transactional
    public Booking create(
            Long userId,
            CreateBookingRequestDto request
    ) {

        Optional<Booking> cached = repository.findByRequestId(request.requestId());
        if (cached.isPresent()) {
            return cached.get();
        }

        String traceId = UUID.randomUUID().toString();
        Long roomId = request.roomId();
        LocalDate from = request.startDate();
        LocalDate to = request.endDate();
        String externalRequestId = request.requestId();

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(from);
        booking.setEndDate(to);
        booking.setRequestId(externalRequestId);
        booking.setCorrelationId(traceId);
        booking.setStatus(Booking.Status.PENDING);
        booking.setCreatedAt(OffsetDateTime.now());

        Booking saved = repository.save(booking);
        log.info("[{}] Reservation created with PENDING status", traceId);

        ConfirmAvailabilityRequestDto confirmAvailabilityRequestDto = new ConfirmAvailabilityRequestDto(externalRequestId, from, to);

        try {
            confirmRoom(saved);

            booking.setStatus(Booking.Status.CONFIRMED);
            repository.save(booking);
            log.info("[{}] Reservation CONFIRMED", traceId);

        } catch (Exception ex) {
            log.warn("[{}] Reservation failed: {}", traceId, ex.getMessage());

            releaseRoom(roomId, confirmAvailabilityRequestDto.requestId());

            booking.setStatus(Booking.Status.CANCELLED);
            repository.save(booking);
            log.info("[{}] Reservation CANCELLED", traceId);
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long id, String username) {
        Booking booking = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Access denied");
        }

        return booking;
    }

    @Retryable(
            retryFor = {FeignException.class, Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    private void confirmRoom(
            Booking booking
    ) {
        log.info("Attempting to confirm availability with Hotel Service: bookingId={}, attempt", booking.getId());

        ConfirmAvailabilityRequestDto request = new ConfirmAvailabilityRequestDto(
                booking.getRequestId(),
                booking.getStartDate(),
                booking.getEndDate()
        );

        hotelClient.confirmAvailability(booking.getRoomId(), request);
    }

    private void releaseRoom(Long roomId, String requestId) {
        hotelClient.releaseRoom(roomId, requestId);
    }
}


