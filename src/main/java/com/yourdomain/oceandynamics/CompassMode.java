package com.yourdomain.oceandynamics;

public enum CompassMode {
    WIND,
    CURRENT;

    public CompassMode toggle() {
        return this == WIND ? CURRENT : WIND;
    }
}
