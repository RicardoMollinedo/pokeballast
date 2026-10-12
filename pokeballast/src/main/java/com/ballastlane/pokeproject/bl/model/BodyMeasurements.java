package com.ballastlane.pokeproject.bl.model;

/**
 * Kept in PokeAPI's native units (decimetres / hectograms) so nothing is lost;
 * converted to SI units only when presented.
 */
public record BodyMeasurements(int heightDecimetres, int weightHectograms) {

    public BodyMeasurements {
        if (heightDecimetres < 0 || weightHectograms < 0) {
            throw new IllegalArgumentException("Measurements cannot be negative");
        }
    }

    public double heightMeters() {
        return heightDecimetres / 10.0;
    }

    public double weightKilograms() {
        return weightHectograms / 10.0;
    }
}
