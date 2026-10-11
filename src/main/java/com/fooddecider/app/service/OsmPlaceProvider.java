package com.fooddecider.app.service;

import com.fooddecider.app.entity.RoomRestaurant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * OpenStreetMap Overpass API implementation.
 * Completely FREE — no API key required.
 * Falls behind Google in data richness (no photos, no ratings) but works everywhere.
 *
 * Docs: https://wiki.openstreetmap.org/wiki/Overpass_API
 */
@Component
public class OsmPlaceProvider implements PlaceProvider {

    private static final Logger log = LoggerFactory.getLogger(OsmPlaceProvider.class);

    // Multiple Overpass servers — if one is overloaded, try the next
    private static final String[] OVERPASS_SERVERS = {
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    };

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Curated Unsplash restaurant photos (OSM has no photos)
    private static final String[] FOOD_IMAGES = {
        "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1537047902294-62a40c20a6ae?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1578474846511-04ba529f0b88?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1590846406792-0adc7f938f1d?w=400&h=300&fit=crop",
        "https://images.unsplash.com/photo-1544025162-d76694265947?w=400&h=300&fit=crop"
    };

    @Override
    public String getProviderName() {
        return "OpenStreetMap (Overpass API)";
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available — no key needed
    }

    @Override
    public List<RoomRestaurant> fetchNearbyRestaurants(double lat, double lng, int radiusMeters) {
        log.info("[OSM] Searching: lat={}, lng={}, radius={}m", lat, lng, radiusMeters);

        // Simpler, faster query — only nodes (covers 95% of restaurants in OSM)
        String query = String.format(
            "[out:json][timeout:15];" +
            "node[\"amenity\"~\"restaurant|cafe|fast_food\"](around:%d,%f,%f);" +
            "out body;",
            radiusMeters, lat, lng
        );

        // Try each Overpass server until one works
        for (String server : OVERPASS_SERVERS) {
            try {
                log.info("[OSM] Trying server: {}", server);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                HttpEntity<String> request = new HttpEntity<>("data=" + query, headers);

                String responseBody = restTemplate.postForObject(server, request, String.class);
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode elements = root.path("elements");

                if (!elements.isArray() || elements.isEmpty()) {
                    log.warn("[OSM] No restaurants found on {} — trying next server", server);
                    continue;
                }

                log.info("[OSM] {} returned {} results", server, elements.size());

                List<RoomRestaurant> restaurants = StreamSupport.stream(elements.spliterator(), false)
                        .filter(node -> node.path("tags").has("name"))
                        .map(this::mapToRestaurant)
                        .collect(Collectors.toList());

                log.info("[OSM] Mapped {} named restaurants", restaurants.size());
                return restaurants;

            } catch (Exception e) {
                log.warn("[OSM] Server {} failed: {} — trying next", server, e.getMessage());
            }
        }

        log.error("[OSM] All Overpass servers failed");
        return Collections.emptyList();
    }

    @Override
    public String getMapUrl(String placeId) {
        return "https://www.openstreetmap.org/node/" + placeId;
    }

    // --- Private mapping ---

    private RoomRestaurant mapToRestaurant(JsonNode element) {
        JsonNode tags = element.path("tags");
        Random random = new Random(element.path("id").asLong());

        RoomRestaurant r = new RoomRestaurant();
        r.setPlaceId(String.valueOf(element.path("id").asLong()));
        r.setName(tags.path("name").asText("Unknown Restaurant"));

        // Pseudo-rating 3.5–5.0 (seeded by OSM id for consistency)
        r.setRating(Math.round((3.5 + random.nextDouble() * 1.5) * 10.0) / 10.0);

        // Cuisine
        String cuisine = tags.path("cuisine").asText("");
        if (cuisine.isEmpty()) {
            String amenity = tags.path("amenity").asText("restaurant");
            cuisine = switch (amenity) {
                case "cafe" -> "Cafe";
                case "fast_food" -> "Fast Food";
                default -> "Restaurant";
            };
        } else {
            cuisine = cuisine.split(";")[0].trim();
            cuisine = cuisine.substring(0, 1).toUpperCase() + cuisine.substring(1);
        }
        r.setCuisine(cuisine);

        // Address
        String street = tags.path("addr:street").asText("");
        String houseNumber = tags.path("addr:housenumber").asText("");
        String city = tags.path("addr:city").asText("");
        String vicinity = "";
        if (!street.isEmpty()) {
            vicinity = houseNumber.isEmpty() ? street : houseNumber + " " + street;
            if (!city.isEmpty()) vicinity += ", " + city;
        } else if (!city.isEmpty()) {
            vicinity = city;
        }
        r.setVicinity(vicinity);

        // Price level 1-3 (seeded)
        r.setPriceLevel(1 + random.nextInt(3));

        // Random Unsplash food photo
        r.setPhotoUrl(FOOD_IMAGES[random.nextInt(FOOD_IMAGES.length)]);

        return r;
    }
}
