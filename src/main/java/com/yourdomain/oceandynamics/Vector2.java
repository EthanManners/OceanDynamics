package com.yourdomain.oceandynamics;

public record Vector2(double x, double z) {
    public static final Vector2 ZERO = new Vector2(0.0, 0.0);

    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.z + other.z);
    }

    public Vector2 multiply(double scalar) {
        return new Vector2(this.x * scalar, this.z * scalar);
    }

    public double length() {
        return Math.sqrt(x * x + z * z);
    }

    public Vector2 normalize() {
        double len = length();
        if (len < 1.0E-8) {
            return ZERO;
        }
        return new Vector2(x / len, z / len);
    }

    public double dot(Vector2 other) {
        return this.x * other.x + this.z * other.z;
    }

    public String toCardinal() {
        double degrees = toDegrees();
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int idx = (int) Math.round(((degrees % 360.0) / 45.0)) % 8;
        return dirs[idx];
    }

    public double toDegrees() {
        double degrees = Math.toDegrees(Math.atan2(x, -z));
        if (degrees < 0) {
            degrees += 360;
        }
        return degrees;
    }
}
