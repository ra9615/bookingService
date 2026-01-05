package com.example.hotel.service;

import com.example.hotel.model.Room;
import com.example.hotel.model.RoomReservation;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.RoomReservationLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomReservationLockRepository lockRepository;

    public Optional<Room> getRoom(Long id) {
        return roomRepository.findById(id);
    }

    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    @Transactional
    public RoomReservation confirmAvailability(String requestId, Long roomId, LocalDate startDate, LocalDate endDate) {
        Optional<RoomReservation> existingReservation = lockRepository.findByRequestId(requestId);
        if (existingReservation.isPresent()) {
            return existingReservation.get();
        }

        List<RoomReservation> overlappingReservations = lockRepository
                .findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        roomId,
                        List.of(RoomReservation.Status.HELD, RoomReservation.Status.CONFIRMED),
                        endDate,
                        startDate
                );

        if (!overlappingReservations.isEmpty()) {
            throw new IllegalStateException("Номер недоступен на указанные даты");
        }

        RoomReservation newReservation = new RoomReservation();
        newReservation.setRequestId(requestId);
        newReservation.setRoomId(roomId);
        newReservation.setStartDate(startDate);
        newReservation.setEndDate(endDate);
        newReservation.setStatus(RoomReservation.Status.HELD);

        return lockRepository.save(newReservation);
    }

    @Transactional
    public RoomReservation confirmReservation(String requestId) {
        RoomReservation reservation = lockRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Удержание не найдено"));

        if (reservation.getStatus() == RoomReservation.Status.CONFIRMED) {
            return reservation;
        }

        if (reservation.getStatus() == RoomReservation.Status.RELEASED) {
            throw new IllegalStateException("Удержание уже снято");
        }

        reservation.setStatus(RoomReservation.Status.CONFIRMED);

        roomRepository.findById(reservation.getRoomId()).ifPresent(room -> {
            room.setTimesBooked(room.getTimesBooked() + 1);
            roomRepository.save(room);
        });

        return lockRepository.save(reservation);
    }

    @Transactional
    public RoomReservation release(String requestId) {
        RoomReservation reservation = lockRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Удержание не найдено"));

        if (reservation.getStatus() == RoomReservation.Status.RELEASED || reservation.getStatus() == RoomReservation.Status.CONFIRMED) {
            return reservation;
        }

        reservation.setStatus(RoomReservation.Status.RELEASED);
        return lockRepository.save(reservation);
    }
}
