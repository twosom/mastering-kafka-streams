package com.icloud.model;

public record DigitalTwin(
        TurbineState desired,
        TurbineState reported
) {

    public DigitalTwin withDesired(final TurbineState desiredTurbineState) {
        return new DigitalTwin(desiredTurbineState, this.reported);
    }

    public DigitalTwin withReported(final TurbineState reportedTurbineState) {
        return new DigitalTwin(this.desired, reportedTurbineState);
    }

    public static DigitalTwin empty() {
        return new DigitalTwin(null, null);
    }
}
