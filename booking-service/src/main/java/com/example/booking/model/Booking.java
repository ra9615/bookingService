package com.example.booking.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_booking_request", columnNames = {"requestId"}))
@Getter
@Setter
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long userId;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String correlationId;

    private OffsetDateTime createdAt;

    public enum Status { PENDING, CONFIRMED, CANCELLED }
}


