package com.icloud.model;

public record TurbineState(
        String timestamp,
        Double windSpeedMph,
        Power power,
        Type type
) {
    public static TurbineState desireOff(TurbineState original) {
        return new TurbineState(
                original.timestamp(),
                original.windSpeedMph(),
                Power.OFF,
                Type.DESIRED);
    }

    public boolean isHighWindAndPowerOn() {
        return this.windSpeedMph() > 65 && this.power == Power.ON;
    }

    public Double windSpeedMph() {
        return this.windSpeedMph == null ? 0 : this.windSpeedMph;
    }

    public enum Power {ON, OFF}

    public enum Type {DESIRED, REPORTED}

}

