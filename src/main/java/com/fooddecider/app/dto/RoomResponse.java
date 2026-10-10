package com.fooddecider.app.dto;

import com.fooddecider.app.entity.Room;
import com.fooddecider.app.enums.RoomStatus;
import java.util.List;
import java.util.stream.Collectors;

public class RoomResponse {

    private String code;
    private RoomStatus status;
    private Integer restaurantCount;
    private List<MemberInfo> members;

    public RoomResponse() {}

    // --- Static factory ---

    public static RoomResponse fromRoom(Room room) {
        RoomResponse response = new RoomResponse();
        response.setCode(room.getCode());
        response.setStatus(room.getStatus());
        response.setRestaurantCount(room.getRestaurantCount());
        response.setMembers(
            room.getMembers().stream()
                .map(m -> new MemberInfo(m.getId(), m.getNickname(), m.getIsHost()))
                .collect(Collectors.toList())
        );
        return response;
    }

    // --- Getters ---

    public String getCode() {
        return code;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public Integer getRestaurantCount() {
        return restaurantCount;
    }

    public List<MemberInfo> getMembers() {
        return members;
    }

    // --- Setters ---

    public void setCode(String code) {
        this.code = code;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public void setRestaurantCount(Integer restaurantCount) {
        this.restaurantCount = restaurantCount;
    }

    public void setMembers(List<MemberInfo> members) {
        this.members = members;
    }

    // -------------------------------------------------------------------------
    // Inner static class
    // -------------------------------------------------------------------------

    public static class MemberInfo {

        private String id;
        private String nickname;
        private Boolean isHost;

        public MemberInfo() {}

        public MemberInfo(String id, String nickname, Boolean isHost) {
            this.id = id;
            this.nickname = nickname;
            this.isHost = isHost;
        }

        // --- Getters ---

        public String getId() {
            return id;
        }

        public String getNickname() {
            return nickname;
        }

        public Boolean getIsHost() {
            return isHost;
        }

        // --- Setters ---

        public void setId(String id) {
            this.id = id;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public void setIsHost(Boolean isHost) {
            this.isHost = isHost;
        }
    }
}
