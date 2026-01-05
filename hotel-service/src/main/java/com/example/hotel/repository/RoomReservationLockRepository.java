package com.example.hotel.repository;

import com.example.hotel.model.RoomReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomReservationLockRepository extends JpaRepository<RoomReservation, Long> {
    Optional<RoomReservation> findByRequestId(String requestId);
    List<RoomReservation> findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long roomId,
            List<RoomReservation.Status> statuses,
            LocalDate endInclusive,
            LocalDate startInclusive
    );
}


