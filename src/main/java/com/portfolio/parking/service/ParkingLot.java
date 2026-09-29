package com.portfolio.parking.service;

import com.portfolio.parking.assignment.SpotAssignmentStrategy;
import com.portfolio.parking.exception.NoSpotAvailableException;
import com.portfolio.parking.exception.ParkingException;
import com.portfolio.parking.exception.TicketNotFoundException;
import com.portfolio.parking.exception.VehicleAlreadyParkedException;
import com.portfolio.parking.model.Money;
import com.portfolio.parking.model.ParkingEvent;
import com.portfolio.parking.model.ParkingSpot;
import com.portfolio.parking.model.Receipt;
import com.portfolio.parking.model.Size;
import com.portfolio.parking.model.Ticket;
import com.portfolio.parking.model.Vehicle;
import com.portfolio.parking.notification.ParkingEventListener;
import com.portfolio.parking.pricing.PricingPolicy;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Entry and exit of vehicles. It coordinates the pieces but holds no business rules itself:
 * who fits where lives in the vehicles, which spot to give in the assignment strategy,
 * and how much to charge in the pricing policy. All three are injected (Dependency Inversion).
 * <p>
 * Thread safety: several barriers can operate at the same time. Choosing a free spot and marking it as
 * taken must happen atomically, otherwise two cars could get the same spot, so state changes are
 * guarded by a single lock. Listeners are notified after the lock is released.
 */
public class ParkingLot {

    private final Map<String, ParkingSpot> spots = new LinkedHashMap<>();
    private final SpotAssignmentStrategy assignmentStrategy;
    private final PricingPolicy pricingPolicy;
    private final List<ParkingEventListener> listeners;
    private final Clock clock;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Ticket> activeTicketsById = new HashMap<>();
    private final Map<String, Ticket> activeTicketsByPlate = new HashMap<>();
    private final Map<String, Ticket> ticketsBySpotId = new HashMap<>();
    private final AtomicLong ticketSequence = new AtomicLong();

    public ParkingLot(List<ParkingSpot> spots,
                      SpotAssignmentStrategy assignmentStrategy,
                      PricingPolicy pricingPolicy,
                      List<ParkingEventListener> listeners,
                      Clock clock) {
        if (spots.isEmpty()) {
            throw new IllegalArgumentException("A parking lot needs at least one spot");
        }
        spots.forEach(spot -> {
            if (this.spots.putIfAbsent(spot.id(), spot) != null) {
                throw new IllegalArgumentException("Duplicate spot id " + spot.id());
            }
        });
        this.assignmentStrategy = assignmentStrategy;
        this.pricingPolicy = pricingPolicy;
        this.listeners = List.copyOf(listeners);
        this.clock = clock;
    }

    /**
     * @throws VehicleAlreadyParkedException if the same plate is already inside
     * @throws NoSpotAvailableException      if no free spot suits this vehicle
     */
    public Ticket enter(Vehicle vehicle) {
        Ticket ticket;
        lock.lock();
        try {
            if (activeTicketsByPlate.containsKey(vehicle.licensePlate())) {
                throw new VehicleAlreadyParkedException(vehicle.licensePlate());
            }
            ParkingSpot spot = assignmentStrategy.choose(vehicle, freeSpots())
                    .orElseThrow(() -> new NoSpotAvailableException(vehicle));

            ticket = new Ticket(nextTicketId(), vehicle, spot, clock.instant());
            activeTicketsById.put(ticket.id(), ticket);
            activeTicketsByPlate.put(vehicle.licensePlate(), ticket);
            ticketsBySpotId.put(spot.id(), ticket);
        } catch (ParkingException e) {
            publish(new ParkingEvent.EntryRejected(vehicle, e.getMessage(), clock.instant()));
            throw e;
        } finally {
            lock.unlock();
        }
        publish(new ParkingEvent.VehicleEntered(ticket));
        return ticket;
    }

    /**
     * @throws TicketNotFoundException if the ticket is unknown or was already used
     */
    public Receipt exit(String ticketId) {
        Receipt receipt;
        lock.lock();
        try {
            Ticket ticket = activeTicketsById.remove(ticketId);
            if (ticket == null) {
                throw new TicketNotFoundException(ticketId);
            }
            activeTicketsByPlate.remove(ticket.vehicle().licensePlate());
            ticketsBySpotId.remove(ticket.spot().id());

            Instant exitTime = clock.instant();
            Money amount = pricingPolicy.price(ticket, exitTime);
            receipt = new Receipt(ticket, exitTime, amount);
        } finally {
            lock.unlock();
        }
        publish(new ParkingEvent.VehicleExited(receipt));
        return receipt;
    }

    /** Number of free spots per size, e.g. for the display at the entrance. */
    public Map<Size, Long> freeSpotsBySize() {
        lock.lock();
        try {
            Map<Size, Long> counts = new EnumMap<>(Size.class);
            for (Size size : Size.values()) {
                counts.put(size, 0L);
            }
            freeSpots().forEach(spot -> counts.merge(spot.size(), 1L, Long::sum));
            return counts;
        } finally {
            lock.unlock();
        }
    }

    public int occupiedCount() {
        lock.lock();
        try {
            return ticketsBySpotId.size();
        } finally {
            lock.unlock();
        }
    }

    public int capacity() {
        return spots.size();
    }

    /** Must be called while holding the lock. */
    private List<ParkingSpot> freeSpots() {
        return spots.values().stream()
                .filter(spot -> !ticketsBySpotId.containsKey(spot.id()))
                .toList();
    }

    private String nextTicketId() {
        return "T-%05d".formatted(ticketSequence.incrementAndGet());
    }

    private void publish(ParkingEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
