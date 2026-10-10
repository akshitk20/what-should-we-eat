package com.fooddecider.app.dto;

import com.fooddecider.app.enums.PriceLevel;
import jakarta.validation.constraints.NotBlank;

public class JoinRoomRequest {

    @NotBlank
    private String nickname;

    private Boolean vegetarian = false;

    private Boolean vegan = false;

    private Boolean halal = false;

    private PriceLevel maxPrice;

    public JoinRoomRequest() {}

    // --- Getters ---

    public String getNickname() {
        return nickname;
    }

    public Boolean getVegetarian() {
        return vegetarian;
    }

    public Boolean getVegan() {
        return vegan;
    }

    public Boolean getHalal() {
        return halal;
    }

    public PriceLevel getMaxPrice() {
        return maxPrice;
    }

    // --- Setters ---

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setVegetarian(Boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public void setVegan(Boolean vegan) {
        this.vegan = vegan;
    }

    public void setHalal(Boolean halal) {
        this.halal = halal;
    }

    public void setMaxPrice(PriceLevel maxPrice) {
        this.maxPrice = maxPrice;
    }
}
