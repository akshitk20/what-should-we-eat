package com.fooddecider.app.repository;

import com.fooddecider.app.entity.RoomRestaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRestaurantRepository extends JpaRepository<RoomRestaurant, String> {
    List<RoomRestaurant> findByRoomCodeOrderBySwipeOrderAsc(String roomCode);
}
