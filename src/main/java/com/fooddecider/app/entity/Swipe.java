package com.fooddecider.app.entity;

import com.fooddecider.app.enums.SwipeDirection;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "swipes")
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private SwipeDirection direction;

    @Column(name = "swiped_at", nullable = false, updatable = false)
    private LocalDateTime swipedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private RoomMember member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RoomRestaurant restaurant;

    // -------------------------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.swipedAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // No-arg constructor
    // -------------------------------------------------------------------------

    public Swipe() {
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

    public SwipeDirection getDirection() {
        return direction;
    }

    public void setDirection(SwipeDirection direction) {
        this.direction = direction;
    }

    public LocalDateTime getSwipedAt() {
        return swipedAt;
    }

    public void setSwipedAt(LocalDateTime swipedAt) {
        this.swipedAt = swipedAt;
    }

    public RoomMember getMember() {
        return member;
    }

    public void setMember(RoomMember member) {
        this.member = member;
    }

    public RoomRestaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(RoomRestaurant restaurant) {
        this.restaurant = restaurant;
    }
}
