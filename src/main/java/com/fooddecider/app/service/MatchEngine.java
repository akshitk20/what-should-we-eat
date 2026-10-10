package com.fooddecider.app.service;

import com.fooddecider.app.entity.Room;
import com.fooddecider.app.entity.RoomMember;
import com.fooddecider.app.entity.RoomRestaurant;
import com.fooddecider.app.entity.Swipe;
import com.fooddecider.app.enums.SwipeDirection;
import com.fooddecider.app.repository.SwipeRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchEngine {

    private final SwipeRepository swipeRepository;

    public MatchEngine(SwipeRepository swipeRepository) {
        this.swipeRepository = swipeRepository;
    }

    /**
     * Check if ALL members in the room have swiped RIGHT on this restaurant.
     * Returns the restaurant if matched, empty otherwise.
     */
    public Optional<RoomRestaurant> checkForMatch(Room room, RoomRestaurant restaurant) {
        int totalMembers = room.getMembers().size();
        List<Swipe> swipes = swipeRepository.findByRestaurantId(restaurant.getId());

        // Not everyone has swiped yet
        if (swipes.size() < totalMembers) {
            return Optional.empty();
        }

        boolean allRight = swipes.stream()
                .allMatch(s -> s.getDirection() == SwipeDirection.RIGHT);

        return allRight ? Optional.of(restaurant) : Optional.empty();
    }

    /**
     * Get swipe progress for a restaurant.
     * Returns: { swipedCount, totalMembers, yesCount }
     */
    public Map<String, Integer> getSwipeProgress(Room room, String restaurantId) {
        List<Swipe> swipes = swipeRepository.findByRestaurantId(restaurantId);
        long yesCount = swipes.stream()
                .filter(s -> s.getDirection() == SwipeDirection.RIGHT)
                .count();

        Map<String, Integer> progress = new HashMap<>();
        progress.put("swipedCount", swipes.size());
        progress.put("totalMembers", room.getMembers().size());
        progress.put("yesCount", (int) yesCount);
        return progress;
    }

    /**
     * Fallback: when no unanimous match is found, return the restaurant
     * with the highest approval ratio (most RIGHT swipes relative to total members).
     */
    public Optional<RoomRestaurant> getBestCompromise(Room room) {
        return room.getRestaurants().stream()
                .max(Comparator.comparingDouble(r -> {
                    long yesCount = swipeRepository.countByRestaurantIdAndDirection(
                            r.getId(), SwipeDirection.RIGHT);
                    return (double) yesCount / room.getMembers().size();
                }));
    }
}
