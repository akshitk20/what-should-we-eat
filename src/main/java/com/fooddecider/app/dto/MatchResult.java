package com.fooddecider.app.dto;

public class MatchResult {

    private String name;
    private Double rating;
    private Integer priceLevel;
    private String vicinity;
    private String cuisine;
    private String photoUrl;
    private String googleMapsUrl;

    public MatchResult() {}

    // --- Getters ---

    public String getName() {
        return name;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    // --- Setters ---

    public void setName(String name) {
        this.name = name;
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

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }
}
