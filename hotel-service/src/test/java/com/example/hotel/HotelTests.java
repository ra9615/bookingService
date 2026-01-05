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
public class HotelTests {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomService roomService;

    @Test
    @Transactional
    void shouldThrowExceptionOnDateConflict() {
        Hotel hotel = new Hotel();
        hotel.setName("H");
        hotel.setCity("C");
        hotel = hotelRepository.save(hotel);

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber("101");
        room.setCapacity(2);
        room = roomService.createRoom(room);

        LocalDate startDate1 = LocalDate.now();
        LocalDate endDate1 = startDate1.plusDays(2);
        roomService.confirmAvailability("req-a", room.getId(), startDate1, endDate1);

        Room finalRoom = room;
        Assertions.assertThrows(IllegalStateException.class, () -> {
            roomService.confirmAvailability("req-b", finalRoom.getId(), startDate1.plusDays(1), endDate1.plusDays(1));
        });
    }

    @Test
    @Transactional
    void availabilityFlagDoesNotImpactDateReservation() {
        Hotel hotel = new Hotel();
        hotel.setName("H");
        hotel.setCity("C");
        hotel = hotelRepository.save(hotel);

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber("102");
        room.setCapacity(2);
        room.setAvailable(false);
        room = roomService.createRoom(room);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(1);
        RoomReservation reservationLock = roomService.confirmAvailability("req-c", room.getId(), startDate, endDate);

        Assertions.assertEquals(RoomReservation.Status.HELD, reservationLock.getStatus());
    }
}
