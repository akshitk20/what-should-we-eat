package com.fooddecider.app.service;

import com.fooddecider.app.dto.*;
import com.fooddecider.app.entity.*;
import com.fooddecider.app.enums.*;
import com.fooddecider.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final RoomRestaurantRepository restaurantRepository;
    private final SwipeRepository swipeRepository;
    private final PlaceService placeService;
    private final MatchEngine matchEngine;

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository memberRepository,
                       RoomRestaurantRepository restaurantRepository,
                       SwipeRepository swipeRepository,
                       PlaceService placeService,
                       MatchEngine matchEngine) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.restaurantRepository = restaurantRepository;
        this.swipeRepository = swipeRepository;
        this.placeService = placeService;
        this.matchEngine = matchEngine;
    }

    /**
     * Create a new room with a unique 4-digit code.
     * Fetches nearby restaurants from Google Places API.
     */
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        String code = generateRoomCode();

        Room room = new Room();
        room.setCode(code);
        room.setLatitude(request.getLatitude());
        room.setLongitude(request.getLongitude());
        room.setRadiusMeters(request.getRadiusMeters() != null ? request.getRadiusMeters() : 1500);
        room.setStatus(RoomStatus.WAITING);
        room.setMembers(new ArrayList<>());
        room.setRestaurants(new ArrayList<>());

        // Create host member
        RoomMember host = new RoomMember();
        host.setNickname(request.getHostNickname());
        host.setIsHost(true);
        host.setRoom(room);
        room.getMembers().add(host);

        // Fetch nearby restaurants
        List<RoomRestaurant> restaurants = placeService.fetchNearbyRestaurants(
                request.getLatitude(), request.getLongitude(), room.getRadiusMeters());
        restaurants.forEach(r -> {
            r.setRoom(room);
            room.getRestaurants().add(r);
        });

        roomRepository.save(room);
        return RoomResponse.fromRoom(room);
    }

    /**
     * Join an existing room.
     */
    @Transactional
    public RoomResponse joinRoom(String roomCode, JoinRoomRequest request) {
        Room room = roomRepository.findById(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RuntimeException("Room is no longer accepting members");
        }

        RoomMember member = new RoomMember();
        member.setNickname(request.getNickname());
        member.setIsHost(false);
        member.setVegetarian(request.getVegetarian());
        member.setVegan(request.getVegan());
        member.setHalal(request.getHalal());
        member.setMaxPrice(request.getMaxPrice());
        member.setRoom(room);
        room.getMembers().add(member);

        roomRepository.save(room);
        return RoomResponse.fromRoom(room);
    }

    /**
     * Get room details.
     */
    public RoomResponse getRoom(String roomCode) {
        Room room = roomRepository.findById(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));
        return RoomResponse.fromRoom(room);
    }

    /**
     * Start the swiping phase (host only).
     */
    @Transactional
    public List<RestaurantCard> startSwiping(String roomCode, String memberId) {
        Room room = roomRepository.findById(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Verify the member is the host
        RoomMember member = room.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!member.getIsHost()) {
            throw new RuntimeException("Only the host can start swiping");
        }

        room.setStatus(RoomStatus.SWIPING);
        roomRepository.save(room);

        // Return restaurant cards sorted by swipe order
        return room.getRestaurants().stream()
                .sorted(Comparator.comparingInt(RoomRestaurant::getSwipeOrder))
                .map(RestaurantCard::fromEntity)
                .toList();
    }

    /**
     * Record a swipe and check for a match.
     * Returns a MatchResult if all members matched, empty otherwise.
     */
    @Transactional
    public Optional<MatchResult> recordSwipe(SwipeRequest request) {
        RoomMember member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));
        RoomRestaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        Room room = member.getRoom();

        // Prevent duplicate swipes
        if (swipeRepository.findByMemberIdAndRestaurantId(member.getId(), restaurant.getId()).isPresent()) {
            throw new RuntimeException("Already swiped on this restaurant");
        }

        Swipe swipe = new Swipe();
        swipe.setMember(member);
        swipe.setRestaurant(restaurant);
        swipe.setDirection(request.getDirection());
        swipeRepository.save(swipe);

        // Check for match
        Optional<RoomRestaurant> match = matchEngine.checkForMatch(room, restaurant);
        if (match.isPresent()) {
            room.setStatus(RoomStatus.MATCHED);
            roomRepository.save(room);

            MatchResult result = new MatchResult();
            result.setRestaurantName(restaurant.getName());
            result.setRating(restaurant.getRating());
            result.setPriceLevel(restaurant.getPriceLevel());
            result.setVicinity(restaurant.getVicinity());
            result.setCuisine(restaurant.getCuisine());
            result.setPhotoUrl(restaurant.getPhotoUrl());
            result.setGoogleMapsUrl(placeService.getGoogleMapsUrl(restaurant.getPlaceId()));
            return Optional.of(result);
        }

        return Optional.empty();
    }

    /**
     * Get swipe progress for a specific restaurant in a room.
     */
    public Map<String, Integer> getSwipeProgress(String roomCode, String restaurantId) {
        Room room = roomRepository.findById(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return matchEngine.getSwipeProgress(room, restaurantId);
    }

    /**
     * Get best compromise when no unanimous match (fallback).
     */
    public Optional<MatchResult> getCompromise(String roomCode) {
        Room room = roomRepository.findById(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return matchEngine.getBestCompromise(room).map(r -> {
            MatchResult result = new MatchResult();
            result.setRestaurantName(r.getName());
            result.setRating(r.getRating());
            result.setPriceLevel(r.getPriceLevel());
            result.setVicinity(r.getVicinity());
            result.setCuisine(r.getCuisine());
            result.setPhotoUrl(r.getPhotoUrl());
            result.setGoogleMapsUrl(placeService.getGoogleMapsUrl(r.getPlaceId()));
            return result;
        });
    }

    /**
     * Leave a room.
     */
    @Transactional
    public void leaveRoom(String roomCode, String memberId) {
        Room room = roomRepository.findById(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        room.getMembers().removeIf(m -> m.getId().equals(memberId));
        if (room.getMembers().isEmpty()) {
            roomRepository.delete(room);
        } else {
            roomRepository.save(room);
        }
    }

    private String generateRoomCode() {
        SecureRandom random = new SecureRandom();
        String code;
        do {
            code = "FOOD-" + String.format("%04d", random.nextInt(10000));
        } while (roomRepository.existsById(code));
        return code;
    }
}
