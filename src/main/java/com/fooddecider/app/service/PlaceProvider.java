package com.fooddecider.app.service;

import com.fooddecider.app.entity.RoomRestaurant;
import java.util.List;

/**
 * Strategy interface for fetching nearby restaurants.
 *
 * Implementations:
 *   - GooglePlaceProvider  — uses Google Places API (requires API key)
 *   - OsmPlaceProvider     — uses OpenStreetMap Overpass API (free, no key)
 *
 * Adding a new provider (e.g. Yelp, Foursquare) = one new class, zero edits elsewhere.
 */
public interface PlaceProvider {

    /**
     * Fetch nearby restaurants for a given location.
     *
     * @param lat          latitude
     * @param lng          longitude
     * @param radiusMeters search radius in meters
     * @return list of RoomRestaurant entities (not yet persisted)
     */
    List<RoomRestaurant> fetchNearbyRestaurants(double lat, double lng, int radiusMeters);

    /**
     * Generate a navigation/map URL for a restaurant.
     *
     * @param placeId provider-specific place identifier
     * @return URL string users can click to navigate
     */
    String getMapUrl(String placeId);

    /**
     * Human-readable name of this provider (for logging).
     */
    String getProviderName();

    /**
     * Whether this provider is properly configured and ready to use.
     */
    boolean isAvailable();
}
