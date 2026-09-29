package com.portfolio.parking.model;

public class Car extends Vehicle {

    public Car(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public Size size() {
        return Size.MEDIUM;
    }

    @Override
    public String type() {
        return "Car";
    }
}
