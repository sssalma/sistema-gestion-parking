package com.portfolio.parking.model;

public class Motorcycle extends Vehicle {

    public Motorcycle(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public Size size() {
        return Size.SMALL;
    }

    @Override
    public String type() {
        return "Motorcycle";
    }
}
