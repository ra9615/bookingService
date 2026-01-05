package com.example.hotel;

import com.example.hotel.model.Hotel;
import com.example.hotel.model.Room;
import com.example.hotel.model.RoomReservation;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.service.HotelService;
import com.example.hotel.service.RoomService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@SpringBootTest
public class RoomTests {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomService roomService;

    @Test
    @Transactional
    void testHoldConfirmReleaseIdempotency() {
        Hotel hotel = new Hotel();
        hotel.setName("H");
        hotel.setCity("C");
        hotel = hotelRepository.save(hotel);

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber("101");
        room.setCapacity(2);
        room = roomService.createRoom(room);

        String requestId = "req-1";
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(2);

        RoomReservation initialHold = roomService.confirmAvailability(requestId, room.getId(), startDate, endDate);
        RoomReservation repeatedHold = roomService.confirmAvailability(requestId, room.getId(), startDate, endDate);
        Assertions.assertEquals(initialHold.getId(), repeatedHold.getId());

        roomService.confirmReservation(requestId);
        roomService.confirmReservation(requestId);

        RoomReservation confirmedReservation = roomService.confirmReservation(requestId);
        Assertions.assertEquals(RoomReservation.Status.CONFIRMED, confirmedReservation.getStatus());

        RoomReservation reservationAfterReleaseAttempt = roomService.release(requestId);
        Assertions.assertEquals(RoomReservation.Status.CONFIRMED, reservationAfterReleaseAttempt.getStatus());
    }
}
