package com.portfolio.parking.exception;

public class TicketNotFoundException extends ParkingException {

    public TicketNotFoundException(String ticketId) {
        super("Ticket %s is not valid or was already used".formatted(ticketId));
    }
}
