package com.portfolio.parking.exception;

public class VehicleAlreadyParkedException extends ParkingException {

    public VehicleAlreadyParkedException(String licensePlate) {
        super("Vehicle %s is already inside".formatted(licensePlate));
    }
}
