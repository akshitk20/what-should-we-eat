package com.fooddecider.app.dto;

import com.fooddecider.app.enums.SwipeDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SwipeRequest {

    @NotBlank
    private String memberId;

    @NotBlank
    private String restaurantId;

    @NotNull
    private SwipeDirection direction;

    public SwipeRequest() {}

    // --- Getters ---

    public String getMemberId() {
        return memberId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public SwipeDirection getDirection() {
        return direction;
    }

    // --- Setters ---

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setDirection(SwipeDirection direction) {
        this.direction = direction;
    }
}
