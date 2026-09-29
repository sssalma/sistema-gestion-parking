package com.portfolio.parking.exception;

import java.util.Currency;

public class CurrencyMismatchException extends ParkingException {

    public CurrencyMismatchException(Currency expected, Currency actual) {
        super("Currency mismatch: expected %s but got %s".formatted(expected, actual));
    }
}
