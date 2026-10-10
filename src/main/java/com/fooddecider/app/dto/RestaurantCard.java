package com.fooddecider.app.dto;

import com.fooddecider.app.entity.RoomRestaurant;

public class RestaurantCard {

    private String id;
    private String placeId;
    private String name;
    private String photoUrl;
    private Double rating;
    private Integer priceLevel;
    private String vicinity;
    private String cuisine;

    public RestaurantCard() {}

    // --- Static factory ---

    public static RestaurantCard fromEntity(RoomRestaurant entity) {
        RestaurantCard card = new RestaurantCard();
        card.setId(entity.getId());
        card.setPlaceId(entity.getPlaceId());
        card.setName(entity.getName());
        card.setPhotoUrl(entity.getPhotoUrl());
        card.setRating(entity.getRating());
        card.setPriceLevel(entity.getPriceLevel());
        card.setVicinity(entity.getVicinity());
        card.setCuisine(entity.getCuisine());
        return card;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Double getRating() {
        return rating;
    }

    public Integer getPriceLevel() {
        return priceLevel;
    }

    public String getVicinity() {
        return vicinity;
    }

    public String getCuisine() {
        return cuisine;
    }

    // --- Setters ---

    public void setId(String id) {
        this.id = id;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
    }

    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }
}
