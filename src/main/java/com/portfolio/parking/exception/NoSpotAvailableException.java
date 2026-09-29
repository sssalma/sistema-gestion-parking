package com.portfolio.parking.exception;

import com.portfolio.parking.model.Vehicle;

public class NoSpotAvailableException extends ParkingException {

    public NoSpotAvailableException(Vehicle vehicle) {
        super("No free spot for %s".formatted(vehicle));
    }
}
