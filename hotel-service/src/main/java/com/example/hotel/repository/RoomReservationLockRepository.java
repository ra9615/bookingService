package com.example.hotel.repository;

import com.example.hotel.model.RoomReservationLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomReservationLockRepository extends JpaRepository<RoomReservationLock, Long> {
    Optional<RoomReservationLock> findByRequestId(String requestId);
    List<RoomReservationLock> findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long roomId,
            List<RoomReservationLock.Status> statuses,
            LocalDate endInclusive,
            LocalDate startInclusive
    );
}


