package com.fooddecider.app.service;

import com.fooddecider.app.entity.RoomRestaurant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Fetches nearby restaurants using DUAL mode:
 *   1. Google Places API — if GOOGLE_PLACES_API_KEY is set (richer data, photos, real ratings)
 *   2. OpenStreetMap Overpass API — free fallback, no key needed
 *
 * The mode is auto-detected at room creation time based on whether the API key is configured.
 */
@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    // --- Overpass (OSM) ---
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    // --- Google Places ---
    @Value("${google.places.api-key}")
    private String googleApiKey;

    @Value("${google.places.nearby-search-url}")
    private String googleNearbyUrl;

    @Value("${google.places.photo-url}")
    private String googlePhotoUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Unsplash food images for OSM cards (OSM has no photos)
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

    // =====================================================================
    //  PUBLIC API — auto-selects Google or OSM
    // =====================================================================

    private boolean isGoogleConfigured() {
        return googleApiKey != null && !googleApiKey.isBlank() && !googleApiKey.equals("your-api-key-here");
    }

    /**
     * Fetch nearby restaurants. Auto-selects data source:
     *   - Google Places API if GOOGLE_PLACES_API_KEY is set
     *   - OpenStreetMap Overpass API otherwise (free, no key)
     */
    public List<RoomRestaurant> fetchNearbyRestaurants(double lat, double lng, int radiusMeters) {
        if (isGoogleConfigured()) {
            log.info("Using GOOGLE PLACES API (key is configured)");
            return fetchFromGoogle(lat, lng, radiusMeters);
        } else {
            log.info("Using OPENSTREETMAP OVERPASS API (no Google key set — free mode)");
            return fetchFromOverpass(lat, lng, radiusMeters);
        }
    }

    /**
     * Generate a navigation URL for a restaurant.
     */
    public String getGoogleMapsUrl(String placeId) {
        if (isGoogleConfigured()) {
            return "https://www.google.com/maps/place/?q=place_id:" + placeId;
        }
        return "https://www.openstreetmap.org/node/" + placeId;
    }

    // =====================================================================
    //  GOOGLE PLACES API
    // =====================================================================

    private List<RoomRestaurant> fetchFromGoogle(double lat, double lng, int radiusMeters) {
        String url = UriComponentsBuilder.fromHttpUrl(googleNearbyUrl)
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", radiusMeters)
                .queryParam("type", "restaurant")
                .queryParam("key", googleApiKey)
                .build()
                .toUriString();

        log.info("Google Places: lat={}, lng={}, radius={}m", lat, lng, radiusMeters);
        log.debug("URL: {}", url.replaceAll("key=[^&]+", "key=***REDACTED***"));

        try {
            String responseBody = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(responseBody);

            String status = root.path("status").asText("UNKNOWN");
            String errorMessage = root.path("error_message").asText("");
            log.info("Google Places response status: {}", status);

            if (!"OK".equals(status)) {
                log.error("Google Places API error: status={}, message={}", status, errorMessage);
                if ("REQUEST_DENIED".equals(status)) {
                    log.error("→ API key invalid or Places API not enabled in Cloud Console");
                }
                log.warn("Falling back to OpenStreetMap Overpass API...");
                return fetchFromOverpass(lat, lng, radiusMeters);
            }

            JsonNode results = root.path("results");
            log.info("Google Places returned {} restaurants", results.size());

            List<RoomRestaurant> restaurants = StreamSupport.stream(results.spliterator(), false)
                    .map(this::mapGooglePlace)
                    .collect(Collectors.toList());

            return shuffleAndCap(restaurants);

        } catch (Exception e) {
            log.error("Google Places API call failed: {} — falling back to OSM", e.getMessage());
            return fetchFromOverpass(lat, lng, radiusMeters);
        }
    }

    private RoomRestaurant mapGooglePlace(JsonNode node) {
        RoomRestaurant r = new RoomRestaurant();
        r.setPlaceId(node.path("place_id").asText(""));
        r.setName(node.path("name").asText("Unknown"));
        r.setRating(node.path("rating").asDouble(0.0));
        r.setPriceLevel(node.path("price_level").asInt(0));
        r.setVicinity(node.path("vicinity").asText(""));

        // Extract cuisine from types
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
                r.setPhotoUrl(UriComponentsBuilder.fromHttpUrl(googlePhotoUrl)
                        .queryParam("maxwidth", 400)
                        .queryParam("photo_reference", photoRef)
                        .queryParam("key", googleApiKey)
                        .build().toUriString());
            }
        }

        return r;
    }

    // =====================================================================
    //  OPENSTREETMAP OVERPASS API (free, no key)
    // =====================================================================

    private List<RoomRestaurant> fetchFromOverpass(double lat, double lng, int radiusMeters) {
        log.info("Overpass API: lat={}, lng={}, radius={}m", lat, lng, radiusMeters);

        String query = String.format(
            "[out:json][timeout:25];" +
            "(" +
            "  node[\"amenity\"=\"restaurant\"](around:%d,%f,%f);" +
            "  way[\"amenity\"=\"restaurant\"](around:%d,%f,%f);" +
            "  node[\"amenity\"=\"cafe\"](around:%d,%f,%f);" +
            "  node[\"amenity\"=\"fast_food\"](around:%d,%f,%f);" +
            ");" +
            "out center body;",
            radiusMeters, lat, lng,
            radiusMeters, lat, lng,
            radiusMeters, lat, lng,
            radiusMeters, lat, lng
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<String> request = new HttpEntity<>("data=" + query, headers);

            String responseBody = restTemplate.postForObject(OVERPASS_URL, request, String.class);
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode elements = root.path("elements");

            if (!elements.isArray() || elements.isEmpty()) {
                log.warn("Overpass: no restaurants found near ({}, {}) within {}m", lat, lng, radiusMeters);
                return Collections.emptyList();
            }

            log.info("Overpass returned {} results", elements.size());

            List<RoomRestaurant> restaurants = StreamSupport.stream(elements.spliterator(), false)
                    .filter(node -> node.path("tags").has("name"))
                    .map(this::mapOsmElement)
                    .collect(Collectors.toList());

            log.info("Mapped {} named restaurants from OSM", restaurants.size());
            return shuffleAndCap(restaurants);

        } catch (Exception e) {
            log.error("Overpass API failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private RoomRestaurant mapOsmElement(JsonNode element) {
        JsonNode tags = element.path("tags");
        Random random = new Random(element.path("id").asLong());

        RoomRestaurant r = new RoomRestaurant();
        r.setPlaceId(String.valueOf(element.path("id").asLong()));
        r.setName(tags.path("name").asText("Unknown Restaurant"));

        // OSM has no star ratings — generate consistent pseudo-rating 3.5-5.0
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

        // Price level (1-3, seeded by OSM id for consistency)
        r.setPriceLevel(1 + random.nextInt(3));

        // Random Unsplash food photo
        r.setPhotoUrl(FOOD_IMAGES[random.nextInt(FOOD_IMAGES.length)]);

        return r;
    }

    // =====================================================================
    //  SHARED HELPERS
    // =====================================================================

    private List<RoomRestaurant> shuffleAndCap(List<RoomRestaurant> restaurants) {
        Collections.shuffle(restaurants);
        for (int i = 0; i < restaurants.size(); i++) {
            restaurants.get(i).setSwipeOrder(i);
        }
        if (restaurants.size() > 20) {
            restaurants = new ArrayList<>(restaurants.subList(0, 20));
            log.info("Capped to 20 restaurants for swiping");
        }
        log.info("Returning {} restaurants for swiping", restaurants.size());
        return restaurants;
    }
}
