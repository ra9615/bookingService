package com.example.hotel.controller;

import com.example.hotel.model.Hotel;
import com.example.hotel.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    public List<Hotel> fetchAllHotels() {
        return hotelService.listHotels();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hotel> fetchHotelById(@PathVariable Long id) {
        return hotelService.getHotel(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @PostMapping
    public Hotel addNewHotel(@RequestBody Hotel hotel) {
        return hotelService.saveHotel(hotel);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}
