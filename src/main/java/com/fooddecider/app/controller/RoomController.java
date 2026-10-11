package com.fooddecider.app.controller;

import com.fooddecider.app.dto.*;
import com.fooddecider.app.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * POST /api/rooms
     * Create a new room. Returns 201 Created with the room details.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request);
    }

    /**
     * GET /api/rooms/{code}
     * Retrieve room details by its join code.
     */
    @GetMapping("/{code}")
    public RoomResponse getRoom(@PathVariable String code) {
        return roomService.getRoom(code);
    }

    /**
     * POST /api/rooms/{code}/join
     * Join an existing room. Returns the updated room details.
     */
    @PostMapping("/{code}/join")
    public RoomResponse joinRoom(
            @PathVariable String code,
            @Valid @RequestBody JoinRoomRequest request) {
        return roomService.joinRoom(code, request);
    }

    /**
     * POST /api/rooms/{code}/start
     * Host starts the swiping phase. Returns the list of restaurant cards to swipe on.
     */
    @PostMapping("/{code}/start")
    public List<RestaurantCard> startSwiping(
            @PathVariable String code,
            @RequestParam String memberId) {
        return roomService.startSwiping(code, memberId);
    }

    /**
     * POST /api/rooms/{code}/compromise
     * Compute and return the best compromise match across all members' swipes.
     */
    @PostMapping("/{code}/compromise")
    public ResponseEntity<MatchResult> getCompromise(@PathVariable String code) {
        return roomService.getCompromise(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/rooms/{code}/members/{memberId}
     * Remove a member from the room. Returns 204 No Content.
     */
    @DeleteMapping("/{code}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String code,
            @PathVariable String memberId) {
        roomService.leaveRoom(code, memberId);
        return ResponseEntity.noContent().build();
    }
}
