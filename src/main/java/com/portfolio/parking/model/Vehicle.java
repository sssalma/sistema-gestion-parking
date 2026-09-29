package com.portfolio.parking.model;

import com.portfolio.parking.exception.InvalidLicensePlateException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Base class for every vehicle. Subclasses say how big they are, and may change
 * which spots they are allowed to use by overriding {@link #canUse(ParkingSpot)}.
 */
public abstract class Vehicle {

    private static final Pattern PLATE = Pattern.compile("[A-Z0-9]{4,10}");

    private final String licensePlate;

    protected Vehicle(String licensePlate) {
        this.licensePlate = normalize(licensePlate);
    }

    public abstract Size size();

    /** Human readable type, used in tickets and reports. */
    public abstract String type();

    /**
     * Default rule: the vehicle must fit, and charging spots are kept free for electric vehicles.
     */
    public boolean canUse(ParkingSpot spot) {
        return size().fitsIn(spot.size()) && !spot.hasCharger();
    }

    public String licensePlate() {
        return licensePlate;
    }

    private static String normalize(String plate) {
        if (plate == null) {
            throw new InvalidLicensePlateException("License plate is required");
        }
        String normalized = plate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!PLATE.matcher(normalized).matches()) {
            throw new InvalidLicensePlateException("'%s' is not a valid license plate".formatted(plate));
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "%s %s".formatted(type(), licensePlate);
    }
}
