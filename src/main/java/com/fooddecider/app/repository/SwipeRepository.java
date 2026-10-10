package com.fooddecider.app.repository;

import com.fooddecider.app.entity.Swipe;
import com.fooddecider.app.enums.SwipeDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SwipeRepository extends JpaRepository<Swipe, String> {
    List<Swipe> findByRestaurantId(String restaurantId);
    Optional<Swipe> findByMemberIdAndRestaurantId(String memberId, String restaurantId);
    long countByRestaurantIdAndDirection(String restaurantId, SwipeDirection direction);
}
