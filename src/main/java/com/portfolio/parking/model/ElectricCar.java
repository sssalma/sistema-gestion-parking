package com.portfolio.parking.model;

/**
 * An electric car is a car that may also use charging spots.
 * <p>
 * It overrides {@link #canUse(ParkingSpot)} by <i>relaxing</i> the rule of its parent, never by adding
 * restrictions: it can go anywhere a regular car can, plus charging spots. That keeps it a safe
 * substitute for {@link Car} (Liskov Substitution Principle).
 */
public class ElectricCar extends Car {

    public ElectricCar(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public String type() {
        return "Electric car";
    }

    @Override
    public boolean canUse(ParkingSpot spot) {
        return size().fitsIn(spot.size());
    }
}
