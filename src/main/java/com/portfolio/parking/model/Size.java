package com.portfolio.parking.model;

/** Size of a vehicle or of a parking spot. A vehicle fits in any spot of its size or bigger. */
public enum Size {
    SMALL, MEDIUM, LARGE;

    public boolean fitsIn(Size spotSize) {
        return this.compareTo(spotSize) <= 0;
    }
}
