package com.example.hotel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_lock_request", columnNames = {"requestId"})
})
@Getter
@Setter
public class RoomReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;

    private Long roomId;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {HELD, RELEASED, CONFIRMED}
}


