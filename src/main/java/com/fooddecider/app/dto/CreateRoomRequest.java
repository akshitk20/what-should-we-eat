package com.fooddecider.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateRoomRequest {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private Integer radiusMeters = 1500;

    @NotBlank
    private String hostNickname;

    public CreateRoomRequest() {}

    // --- Getters ---

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getRadiusMeters() {
        return radiusMeters;
    }

    public String getHostNickname() {
        return hostNickname;
    }

    // --- Setters ---

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setRadiusMeters(Integer radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public void setHostNickname(String hostNickname) {
        this.hostNickname = hostNickname;
    }
}
