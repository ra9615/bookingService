package com.example.hotel.controller;

import com.example.hotel.model.Room;
import com.example.hotel.model.RoomReservation;
import com.example.hotel.service.HotelService;
import com.example.hotel.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final HotelService hotelService;

    private final RoomService roomService;

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return roomService.getRoom(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @PostMapping
    public Room createRoom(@RequestBody Room room) {
        return roomService.createRoom(room);
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        return roomService.getRoom(id)
                .map(existingRoom -> {
                    room.setId(id);
                    Room updatedRoom = roomService.createRoom(room);
                    return ResponseEntity.ok(updatedRoom);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<RoomReservation> confirmReservation(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String requestId = request.get("requestId");
        try {
            RoomReservation lock = roomService.confirmReservation(requestId);
            return ResponseEntity.ok(lock);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).build();
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<RoomReservation> releaseReservation(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String requestId = request.get("requestId");
        try {
            RoomReservation lock = roomService.release(requestId);
            return ResponseEntity.ok(lock);
        } catch (IllegalStateException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
