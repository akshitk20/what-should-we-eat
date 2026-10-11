package com.fooddecider.app.service;

import com.fooddecider.app.entity.RoomRestaurant;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Strategy-based restaurant service.
 *
 * Picks the best available PlaceProvider at startup:
 *   1. GooglePlaceProvider — if API key is configured
 *   2. OsmPlaceProvider   — free fallback (always available)
 *
 * If the primary provider fails at runtime, automatically falls back to the next one.
 *
 * To add a new provider (e.g. Yelp, Foursquare):
 *   1. Create a class implementing PlaceProvider
 *   2. Annotate it with @Component
 *   3. Done — it gets auto-injected into the providers list
 */
@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    private final List<PlaceProvider> providers;
    private PlaceProvider activeProvider;

    /**
     * Spring injects ALL PlaceProvider beans automatically.
     */
    public PlaceService(List<PlaceProvider> providers) {
        this.providers = providers;
    }

    /**
     * Pick the best available provider at startup.
     * Priority: first available non-OSM provider, then OSM as fallback.
     */
    @PostConstruct
    void selectProvider() {
        log.info("========================================");
        log.info("Registered place providers:");
        for (PlaceProvider p : providers) {
            log.info("  -> {} [available={}]", p.getProviderName(), p.isAvailable());
        }

        // Priority: Google > OSM > Demo
        activeProvider = providers.stream()
                .filter(p -> p.isAvailable()
                        && !(p instanceof OsmPlaceProvider)
                        && !(p instanceof DemoPlaceProvider))
                .findFirst()
                .orElse(providers.stream()
                        .filter(p -> p.isAvailable() && !(p instanceof DemoPlaceProvider))
                        .findFirst()
                        .orElse(providers.stream()
                                .filter(PlaceProvider::isAvailable)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("No place providers available!"))));

        log.info("Active provider: {}", activeProvider.getProviderName());
        log.info("========================================");
    }

    /**
     * Fetch nearby restaurants using the active provider.
     * Falls back to other providers on failure.
     */
    public List<RoomRestaurant> fetchNearbyRestaurants(double lat, double lng, int radiusMeters) {
        log.info("Fetching restaurants via {}", activeProvider.getProviderName());

        try {
            List<RoomRestaurant> results = activeProvider.fetchNearbyRestaurants(lat, lng, radiusMeters);
            if (!results.isEmpty()) {
                return shuffleAndCap(results);
            }
            log.warn("{} returned 0 results — trying fallback providers...",
                    activeProvider.getProviderName());
        } catch (Exception e) {
            log.warn("{} failed: {} — trying fallback providers...",
                    activeProvider.getProviderName(), e.getMessage());
        }
        return tryFallbackProviders(lat, lng, radiusMeters);
    }

    /**
     * Generate a navigation URL using the active provider.
     */
    public String getGoogleMapsUrl(String placeId) {
        return activeProvider.getMapUrl(placeId);
    }

    /**
     * Returns the name of the currently active provider (useful for frontend/logging).
     */
    public String getActiveProviderName() {
        return activeProvider.getProviderName();
    }

    // --- Private helpers ---

    private List<RoomRestaurant> tryFallbackProviders(double lat, double lng, int radiusMeters) {
        for (PlaceProvider fallback : providers) {
            if (fallback == activeProvider || !fallback.isAvailable()) continue;

            log.info("Trying fallback: {}", fallback.getProviderName());
            try {
                List<RoomRestaurant> results = fallback.fetchNearbyRestaurants(lat, lng, radiusMeters);
                if (!results.isEmpty()) {
                    log.info("Fallback {} succeeded with {} results",
                            fallback.getProviderName(), results.size());
                    return shuffleAndCap(results);
                }
            } catch (Exception ex) {
                log.warn("Fallback {} also failed: {}", fallback.getProviderName(), ex.getMessage());
            }
        }
        log.error("All providers failed. Returning empty list.");
        return Collections.emptyList();
    }

    private List<RoomRestaurant> shuffleAndCap(List<RoomRestaurant> restaurants) {
        if (restaurants.isEmpty()) return restaurants;

        List<RoomRestaurant> mutable = new ArrayList<>(restaurants);
        Collections.shuffle(mutable);
        for (int i = 0; i < mutable.size(); i++) {
            mutable.get(i).setSwipeOrder(i);
        }
        if (mutable.size() > 20) {
            mutable = new ArrayList<>(mutable.subList(0, 20));
            log.info("Capped to 20 restaurants for swiping");
        }
        log.info("Returning {} restaurants for swiping", mutable.size());
        return mutable;
    }
}
