package com.fooddecider.app.service;

import com.fooddecider.app.entity.RoomRestaurant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Demo provider with realistic restaurant data.
 * Always available — used as last-resort fallback when Google and OSM both fail.
 * Returns curated restaurant data so the full app flow can be tested end-to-end.
 */
@Component
@Order(99) // lowest priority — only used if Google and OSM both fail
public class DemoPlaceProvider implements PlaceProvider {

    private static final Logger log = LoggerFactory.getLogger(DemoPlaceProvider.class);

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

    // Realistic restaurant data
    private static final String[][] RESTAURANTS = {
        {"Trattoria Roma",       "Italian",    "4.5", "2", "12 MG Road"},
        {"Sushi Zen",            "Japanese",   "4.7", "3", "45 Brigade Road"},
        {"Bombay Bistro",        "Indian",     "4.3", "2", "78 Church Street"},
        {"The Burger Joint",     "American",   "4.1", "1", "23 Commercial Street"},
        {"Dragon Palace",        "Chinese",    "4.2", "2", "56 Residency Road"},
        {"Taco Loco",            "Mexican",    "4.0", "1", "34 Lavelle Road"},
        {"Le Petit Café",        "French",     "4.6", "3", "89 Cunningham Road"},
        {"Olive Garden",         "Mediterranean", "4.4", "2", "67 Indiranagar"},
        {"Spice Route",          "Thai",       "4.3", "2", "11 Koramangala"},
        {"The Pancake House",    "Cafe",       "4.2", "1", "22 HSR Layout"},
        {"Kebab Corner",         "Turkish",    "4.1", "1", "5 Jayanagar"},
        {"Seoul Kitchen",        "Korean",     "4.5", "2", "99 Whitefield"},
        {"Pizza Paradiso",       "Pizza",      "4.0", "1", "15 Marathahalli"},
        {"The Coastal Shack",    "Seafood",    "4.6", "2", "42 Bellandur"},
        {"Green Bowl",           "Vegan",      "4.3", "2", "8 Sarjapur Road"},
    };

    @Override
    public String getProviderName() {
        return "Demo (built-in restaurant data)";
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available
    }

    @Override
    public List<RoomRestaurant> fetchNearbyRestaurants(double lat, double lng, int radiusMeters) {
        log.info("[DEMO] Returning {} demo restaurants", RESTAURANTS.length);

        List<RoomRestaurant> restaurants = new ArrayList<>();
        Random random = new Random(Double.doubleToLongBits(lat + lng)); // consistent per location

        for (int i = 0; i < RESTAURANTS.length; i++) {
            String[] data = RESTAURANTS[i];
            RoomRestaurant r = new RoomRestaurant();
            r.setPlaceId("demo-" + (i + 1));
            r.setName(data[0]);
            r.setCuisine(data[1]);
            r.setRating(Double.parseDouble(data[2]));
            r.setPriceLevel(Integer.parseInt(data[3]));
            r.setVicinity(data[4]);
            r.setPhotoUrl(FOOD_IMAGES[i % FOOD_IMAGES.length]);
            r.setSwipeOrder(i);
            restaurants.add(r);
        }

        // Shuffle for variety
        Collections.shuffle(restaurants, random);
        for (int i = 0; i < restaurants.size(); i++) {
            restaurants.get(i).setSwipeOrder(i);
        }

        return restaurants;
    }

    @Override
    public String getMapUrl(String placeId) {
        return "https://www.google.com/maps/search/restaurants+near+me";
    }
}
