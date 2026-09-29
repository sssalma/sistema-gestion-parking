package com.portfolio.parking.model;

import com.portfolio.parking.exception.InvalidLicensePlateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleTest {

    private static final ParkingSpot SMALL = ParkingSpot.regular("S", 0, Size.SMALL);
    private static final ParkingSpot MEDIUM = ParkingSpot.regular("M", 0, Size.MEDIUM);
    private static final ParkingSpot LARGE = ParkingSpot.regular("L", 0, Size.LARGE);
    private static final ParkingSpot CHARGER = ParkingSpot.charging("C", 0);

    static Stream<Arguments> whoCanParkWhere() {
        return Stream.of(
                //                        vehicle                  small  medium large  charger
                Arguments.of(new Motorcycle("1111AAA"),   true,  true,  true,  false),
                Arguments.of(new Car("2222BBB"),          false, true,  true,  false),
                Arguments.of(new ElectricCar("3333CCC"),  false, true,  true,  true),
                Arguments.of(new Van("4444DDD"),          false, false, true,  false));
    }

    /** Polymorphism: the same call, {@code canUse}, gives a different answer for each vehicle type. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("whoCanParkWhere")
    void eachVehicleDecidesWhichSpotsItCanUse(Vehicle vehicle, boolean small, boolean medium,
                                               boolean large, boolean charger) {
        assertThat(List.of(vehicle.canUse(SMALL), vehicle.canUse(MEDIUM), vehicle.canUse(LARGE), vehicle.canUse(CHARGER)))
                .containsExactly(small, medium, large, charger);
    }

    /**
     * Liskov: an ElectricCar can replace a Car anywhere. Every spot a Car can use,
     * an ElectricCar can use too (it only adds the charging spots).
     */
    @Test
    void electricCarIsASafeSubstituteForCar() {
        Car car = new Car("5555EEE");
        Car electric = new ElectricCar("6666FFF");

        for (ParkingSpot spot : List.of(SMALL, MEDIUM, LARGE, CHARGER)) {
            if (car.canUse(spot)) {
                assertThat(electric.canUse(spot)).as(spot.id()).isTrue();
            }
        }
        assertThat(electric.size()).isEqualTo(car.size());
    }

    @Test
    void normalisesLicensePlates() {
        assertThat(new Car("1234-bcd").licensePlate()).isEqualTo("1234BCD");
        assertThat(new Car(" 1234 BCD ").licensePlate()).isEqualTo("1234BCD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "AB", "12*34", "ABCDEFGHIJKL"})
    void rejectsInvalidLicensePlates(String plate) {
        assertThatThrownBy(() -> new Car(plate)).isInstanceOf(InvalidLicensePlateException.class);
    }

    @Test
    void chargingSpotsMustFitACar() {
        assertThatThrownBy(() -> new ParkingSpot("X", 0, Size.SMALL, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
