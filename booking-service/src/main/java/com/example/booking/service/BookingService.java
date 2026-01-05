package com.example.booking.service;

import com.example.booking.model.Booking;
import com.example.booking.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class BookingService {
    private final BookingRepository repository;
    private final WebClient hotelClient;
    private final Duration requestTimeout;
    private final int maxRetries;

    public BookingService(
            BookingRepository repository,
            WebClient.Builder clientBuilder,
            @Value("${hotel.base-url}") String baseUrl,
            @Value("${hotel.timeout-ms}") long timeoutMs,
            @Value("${hotel.retries}") int retries
    ) {
        this.repository = repository;
        this.hotelClient = clientBuilder.baseUrl(baseUrl).build();
        this.requestTimeout = Duration.ofMillis(timeoutMs);
        this.maxRetries = retries;
    }

    @Transactional
    public Booking create(
            Long userId,
            Long roomId,
            LocalDate from,
            LocalDate to,
            String externalRequestId
    ) {

        Optional<Booking> cached = repository.findByRequestId(externalRequestId);
        if (cached.isPresent()) {
            return cached.get();
        }

        String traceId = UUID.randomUUID().toString();

        Booking booking = initializeBooking(
                userId, roomId, from, to, externalRequestId, traceId
        );

        repository.save(booking);
        log.info("[{}] Reservation created with PENDING status", traceId);

        try {
            holdRoom(roomId, externalRequestId, from, to, traceId);
            confirmRoom(roomId, externalRequestId, traceId);

            booking.setStatus(Booking.Status.CONFIRMED);
            repository.save(booking);
            log.info("[{}] Reservation CONFIRMED", traceId);

        } catch (Exception ex) {
            log.warn("[{}] Reservation failed: {}", traceId, ex.getMessage());

            releaseRoomQuietly(roomId, externalRequestId, traceId);

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

    private void holdRoom(
            Long roomId,
            String requestId,
            LocalDate from,
            LocalDate to,
            String traceId
    ) {
        invokeHotel(
                "/rooms/" + roomId + "/hold",
                Map.of(
                        "requestId", requestId,
                        "startDate", from.toString(),
                        "endDate", to.toString()
                ),
                traceId
        ).block(requestTimeout);
    }

    private void confirmRoom(Long roomId, String requestId, String traceId) {
        invokeHotel(
                "/rooms/" + roomId + "/confirm",
                Map.of("requestId", requestId),
                traceId
        ).block(requestTimeout);
    }

    private void releaseRoomQuietly(Long roomId, String requestId, String traceId) {
        try {
            invokeHotel(
                    "/rooms/" + roomId + "/release",
                    Map.of("requestId", requestId),
                    traceId
            ).block(requestTimeout);
        } catch (Exception ignored) {
        }
    }

    private Mono<String> invokeHotel(
            String endpoint,
            Map<String, String> body,
            String traceId
    ) {
        return hotelClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", traceId)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(requestTimeout)
                .retryWhen(
                        Retry.backoff(maxRetries, Duration.ofMillis(250))
                                .maxBackoff(Duration.ofSeconds(2))
                );
    }

    private Booking initializeBooking(
            Long userId,
            Long roomId,
            LocalDate from,
            LocalDate to,
            String requestId,
            String traceId
    ) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(from);
        booking.setEndDate(to);
        booking.setRequestId(requestId);
        booking.setCorrelationId(traceId);
        booking.setStatus(Booking.Status.PENDING);
        booking.setCreatedAt(OffsetDateTime.now());
        return booking;
    }
}


