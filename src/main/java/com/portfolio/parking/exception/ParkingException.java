package com.portfolio.parking.exception;

/** Base type for business rule violations, so callers can catch all of them in one place. */
public abstract class ParkingException extends RuntimeException {

    protected ParkingException(String message) {
        super(message);
    }
}
