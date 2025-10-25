package com.example.hotel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;

    private int capacity;

    private long timesBooked;

    private boolean available = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private Hotel hotel;
}


