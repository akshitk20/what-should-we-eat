package com.fooddecider.app.service;

import com.fooddecider.app.entity.RoomRestaurant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Google Places API implementation.
 * Provides real photos, ratings, and reviews.
 * Requires a valid GOOGLE_PLACES_API_KEY.
 */
@Component
public class GooglePlaceProvider implements PlaceProvider {

    private static final Logger log = LoggerFactory.getLogger(GooglePlaceProvider.class);

    @Value("${google.places.api-key}")
    private String apiKey;

    @Value("${google.places.nearby-search-url}")
    private String nearbySearchUrl;

    @Value("${google.places.photo-url}")
    private String photoBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "Google Places API";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("your-api-key-here");
    }

    @Override
    public List<RoomRestaurant> fetchNearbyRestaurants(double lat, double lng, int radiusMeters) {
        String url = UriComponentsBuilder.fromHttpUrl(nearbySearchUrl)
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", radiusMeters)
                .queryParam("type", "restaurant")
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        log.info("[Google] Searching: lat={}, lng={}, radius={}m", lat, lng, radiusMeters);
        log.debug("[Google] URL: {}", url.replaceAll("key=[^&]+", "key=***REDACTED***"));

        String responseBody = restTemplate.getForObject(url, String.class);
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google Places response", e);
        }

        String status = root.path("status").asText("UNKNOWN");
        String errorMessage = root.path("error_message").asText("");
        log.info("[Google] Response status: {}", status);

        if (!"OK".equals(status)) {
            log.error("[Google] API error: status={}, message={}", status, errorMessage);
            if ("REQUEST_DENIED".equals(status)) {
                log.error("[Google] → API key invalid or Places API not enabled in Cloud Console");
            }
            throw new RuntimeException("Google Places API returned: " + status + " — " + errorMessage);
        }

        JsonNode results = root.path("results");
        log.info("[Google] Found {} restaurants", results.size());

        return StreamSupport.stream(results.spliterator(), false)
                .map(this::mapToRestaurant)
                .collect(Collectors.toList());
    }

    @Override
    public String getMapUrl(String placeId) {
        return "https://www.google.com/maps/place/?q=place_id:" + placeId;
    }

    // --- Private mapping ---

    private RoomRestaurant mapToRestaurant(JsonNode node) {
        RoomRestaurant r = new RoomRestaurant();
        r.setPlaceId(node.path("place_id").asText(""));
        r.setName(node.path("name").asText("Unknown"));
        r.setRating(node.path("rating").asDouble(0.0));
        r.setPriceLevel(node.path("price_level").asInt(0));
        r.setVicinity(node.path("vicinity").asText(""));

        // Cuisine from types
        JsonNode types = node.path("types");
        if (types.isArray()) {
            String cuisine = StreamSupport.stream(types.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(t -> !t.equals("restaurant") && !t.equals("food")
                              && !t.equals("point_of_interest") && !t.equals("establishment"))
                    .findFirst()
                    .orElse("Restaurant");
            r.setCuisine(cuisine);
        }

        // Photo URL
        JsonNode photos = node.path("photos");
        if (photos.isArray() && !photos.isEmpty()) {
            String photoRef = photos.get(0).path("photo_reference").asText("");
            if (!photoRef.isEmpty()) {
                r.setPhotoUrl(UriComponentsBuilder.fromHttpUrl(photoBaseUrl)
                        .queryParam("maxwidth", 400)
                        .queryParam("photo_reference", photoRef)
                        .queryParam("key", apiKey)
                        .build().toUriString());
            }
        }

        return r;
    }
}
