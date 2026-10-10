package com.fooddecider.app.service;

import com.fooddecider.app.entity.RoomRestaurant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class PlaceService {

    @Value("${google.places.api-key}")
    private String apiKey;

    @Value("${google.places.nearby-search-url}")
    private String nearbySearchUrl;

    @Value("${google.places.photo-url}")
    private String photoBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetch nearby restaurants from Google Places API.
     * Returns a list of RoomRestaurant entities (not yet persisted).
     */
    public List<RoomRestaurant> fetchNearbyRestaurants(double lat, double lng, int radiusMeters) {
        String url = UriComponentsBuilder.fromHttpUrl(nearbySearchUrl)
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", radiusMeters)
                .queryParam("type", "restaurant")
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode results = root.path("results");

            AtomicInteger order = new AtomicInteger(0);
            List<RoomRestaurant> restaurants = StreamSupport.stream(results.spliterator(), false)
                    .map(node -> mapToRestaurant(node, order.getAndIncrement()))
                    .collect(Collectors.toList());

            // Shuffle for randomized swiping order
            Collections.shuffle(restaurants);
            for (int i = 0; i < restaurants.size(); i++) {
                restaurants.get(i).setSwipeOrder(i);
            }

            return restaurants;
        } catch (Exception e) {
            // In production, log and handle properly
            throw new RuntimeException("Failed to fetch nearby restaurants: " + e.getMessage(), e);
        }
    }

    private RoomRestaurant mapToRestaurant(JsonNode node, int order) {
        RoomRestaurant r = new RoomRestaurant();
        r.setPlaceId(node.path("place_id").asText(""));
        r.setName(node.path("name").asText("Unknown"));
        r.setRating(node.path("rating").asDouble(0.0));
        r.setPriceLevel(node.path("price_level").asInt(0));
        r.setVicinity(node.path("vicinity").asText(""));
        r.setSwipeOrder(order);

        // Extract first type that looks like a cuisine
        JsonNode types = node.path("types");
        if (types.isArray()) {
            String cuisine = StreamSupport.stream(types.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(t -> !t.equals("restaurant") && !t.equals("food") && !t.equals("point_of_interest") && !t.equals("establishment"))
                    .findFirst()
                    .orElse("restaurant");
            r.setCuisine(cuisine);
        }

        // Build photo URL if available
        JsonNode photos = node.path("photos");
        if (photos.isArray() && photos.size() > 0) {
            String photoRef = photos.get(0).path("photo_reference").asText("");
            if (!photoRef.isEmpty()) {
                String photoUrl = UriComponentsBuilder.fromHttpUrl(photoBaseUrl)
                        .queryParam("maxwidth", 400)
                        .queryParam("photo_reference", photoRef)
                        .queryParam("key", apiKey)
                        .build().toUriString();
                r.setPhotoUrl(photoUrl);
            }
        }

        return r;
    }

    /**
     * Generate a Google Maps navigation URL for a restaurant.
     */
    public String getGoogleMapsUrl(String placeId) {
        return "https://www.google.com/maps/place/?q=place_id:" + placeId;
    }
}
