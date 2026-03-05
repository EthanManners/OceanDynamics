package com.oceandynamics;

import org.bukkit.util.Vector;

public final class DirectionUtil {
    private static final String[] DIRECTIONS_16 = {
            "E", "ENE", "NE", "NNE", "N", "NNW", "NW", "WNW",
            "W", "WSW", "SW", "SSW", "S", "SSE", "SE", "ESE"
    };

    private DirectionUtil() {
    }

    public static double toDegrees(Vector vector) {
        Vector flat = vector.clone().setY(0);
        if (flat.lengthSquared() < 1e-9) {
            return 0.0;
        }
        flat.normalize();
        double degrees = Math.toDegrees(Math.atan2(flat.getZ(), flat.getX()));
        if (degrees < 0) {
            degrees += 360;
        }
        return Math.round(degrees * 10.0) / 10.0;
    }

    public static String toCompass(Vector vector) {
        Vector flat = vector.clone().setY(0);
        if (flat.lengthSquared() < 1e-9) {
            return "CALM";
        }
        double degrees = toDegrees(flat);
        int index = (int) Math.round(degrees / 22.5) % 16;
        return DIRECTIONS_16[index];
    }
}
