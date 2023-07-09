package com.icloud.model;

public record TurbineState(String timestamp, double windSpeedMph, Power power, Type type) {

    public static TurbineState desireOff(TurbineState original) {
        return new TurbineState(
                original.timestamp(),
                original.windSpeedMph(),
                Power.OFF,
                Type.DESIRED);
    }

    public boolean isHighWind() {
        return this.windSpeedMph > 65 && this.power.equals(Power.ON);
    }


    public enum Power {ON, OFF}

    public enum Type {DESIRED, REPORTED}
}
