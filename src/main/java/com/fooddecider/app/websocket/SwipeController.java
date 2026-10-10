package com.fooddecider.app.websocket;

import com.fooddecider.app.dto.*;
import com.fooddecider.app.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;

@Controller
public class SwipeController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    public SwipeController(RoomService roomService, SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle a swipe event from a client.
     * Client sends to: /app/room/{code}/swipe
     * Broadcasts progress to: /topic/room/{code}
     * If a match is found, also broadcasts a MATCH_FOUND event.
     */
    @MessageMapping("/room/{code}/swipe")
    public void handleSwipe(@DestinationVariable String code, SwipeRequest request) {
        // Record the swipe and check for a match
        Optional<MatchResult> matchResult = roomService.recordSwipe(request);

        // Get current swipe progress for this restaurant
        Map<String, Integer> progress = roomService.getSwipeProgress(code, request.getRestaurantId());

        // Broadcast swipe progress to all room subscribers
        messagingTemplate.convertAndSend(
                "/topic/room/" + code,
                new WebSocketMessage("SWIPE_PROGRESS", Map.of(
                        "restaurantId", request.getRestaurantId(),
                        "swipedCount", progress.get("swipedCount"),
                        "totalMembers", progress.get("totalMembers")
                ))
        );

        // If all members have swiped and a match was found, broadcast it
        if (matchResult.isPresent()) {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + code,
                    new WebSocketMessage("MATCH_FOUND", matchResult.get())
            );
        }
    }

    /**
     * Handle member joined notification.
     * Client sends to: /app/room/{code}/join
     * Broadcasts to: /topic/room/{code}
     */
    @MessageMapping("/room/{code}/join")
    public void handleJoin(@DestinationVariable String code, Map<String, String> payload) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + code,
                new WebSocketMessage("MEMBER_JOINED", Map.of(
                        "nickname", payload.getOrDefault("nickname", "Anonymous"),
                        "roomCode", code
                ))
        );
    }

    /**
     * Handle swiping started notification (sent by the host).
     * Client sends to: /app/room/{code}/start
     * Broadcasts the restaurant list to: /topic/room/{code}
     */
    @MessageMapping("/room/{code}/start")
    public void handleStart(@DestinationVariable String code, Map<String, String> payload) {
        var restaurants = roomService.startSwiping(code, payload.get("memberId"));
        messagingTemplate.convertAndSend(
                "/topic/room/" + code,
                new WebSocketMessage("SWIPING_STARTED", restaurants)
        );
    }
}
