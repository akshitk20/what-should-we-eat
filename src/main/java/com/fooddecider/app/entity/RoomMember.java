package com.fooddecider.app.entity;

import com.fooddecider.app.enums.PriceLevel;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room_members")
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "is_host", nullable = false)
    private Boolean isHost = false;

    @Column(name = "vegetarian", nullable = false)
    private Boolean vegetarian = false;

    @Column(name = "vegan", nullable = false)
    private Boolean vegan = false;

    @Column(name = "halal", nullable = false)
    private Boolean halal = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_price")
    private PriceLevel maxPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_code", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Swipe> swipes = new ArrayList<>();

    // -------------------------------------------------------------------------
    // No-arg constructor
    // -------------------------------------------------------------------------

    public RoomMember() {
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Boolean getIsHost() {
        return isHost;
    }

    public void setIsHost(Boolean isHost) {
        this.isHost = isHost;
    }

    public Boolean getVegetarian() {
        return vegetarian;
    }

    public void setVegetarian(Boolean vegetarian) {
        this.vegetarian = vegetarian;
    }

    public Boolean getVegan() {
        return vegan;
    }

    public void setVegan(Boolean vegan) {
        this.vegan = vegan;
    }

    public Boolean getHalal() {
        return halal;
    }

    public void setHalal(Boolean halal) {
        this.halal = halal;
    }

    public PriceLevel getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(PriceLevel maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public List<Swipe> getSwipes() {
        return swipes;
    }

    public void setSwipes(List<Swipe> swipes) {
        this.swipes = swipes;
    }
}
