package com.portfolio.parking.model;

public class Van extends Vehicle {

    public Van(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public Size size() {
        return Size.LARGE;
    }

    @Override
    public String type() {
        return "Van";
    }
}
