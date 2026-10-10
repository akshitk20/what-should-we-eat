package com.fooddecider.app.repository;

import com.fooddecider.app.entity.Room;
import com.fooddecider.app.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByExpiresAtBeforeAndStatusNot(LocalDateTime time, RoomStatus status);
}
