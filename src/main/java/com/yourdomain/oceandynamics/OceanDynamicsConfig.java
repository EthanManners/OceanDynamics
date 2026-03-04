package com.yourdomain.oceandynamics;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class OceanDynamicsConfig {
    public int cellSize;
    public int tickInterval;
    public double currentPush;
    public double currentWithMultiplier;
    public double currentAgainstDrag;
    public double windPush;
    public double windWithMultiplier;
    public double windAgainstDrag;
    public int windChangeMinutes;
    public double windStrengthMin;
    public double windStrengthMax;
    public boolean windFixedStrength;
    public double windFixedValue;
    public double crewBonusMultiplier;
    public double maxHorizontalSpeed;
    public int activeTimeoutSeconds;
    public List<List<Vector2>> currents;

    public static OceanDynamicsConfig from(FileConfiguration cfg) {
        OceanDynamicsConfig c = new OceanDynamicsConfig();
        c.cellSize = Math.max(1, cfg.getInt("cellSize", 500));
        c.tickInterval = Math.max(1, cfg.getInt("tickInterval", 2));
        c.currentPush = Math.max(0.0, cfg.getDouble("currentPush", 0.05));
        c.currentWithMultiplier = Math.max(1.0, cfg.getDouble("currentWithMultiplier", 1.8));
        c.currentAgainstDrag = Math.max(0.0, cfg.getDouble("currentAgainstDrag", 0.08));
        c.windPush = Math.max(0.0, cfg.getDouble("windPush", 0.02));
        c.windWithMultiplier = Math.max(1.0, cfg.getDouble("windWithMultiplier", 1.3));
        c.windAgainstDrag = Math.max(0.0, cfg.getDouble("windAgainstDrag", 0.03));
        c.windChangeMinutes = Math.max(1, cfg.getInt("windChangeMinutes", 20));
        c.windStrengthMin = Math.max(0.0, cfg.getDouble("windStrengthMin", 0.2));
        c.windStrengthMax = Math.max(c.windStrengthMin, cfg.getDouble("windStrengthMax", 0.9));
        c.windFixedStrength = cfg.getBoolean("windFixedStrength.enabled", false);
        c.windFixedValue = Math.max(0.0, cfg.getDouble("windFixedStrength.value", 0.5));
        c.crewBonusMultiplier = Math.max(1.0, cfg.getDouble("crewBonusMultiplier", 1.15));
        c.maxHorizontalSpeed = Math.max(0.1, cfg.getDouble("maxHorizontalSpeed", 1.3));
        c.activeTimeoutSeconds = Math.max(1, cfg.getInt("activeTimeoutSeconds", 6));
        c.currents = parseCurrents(cfg.getList("currents"));
        return c;
    }

    @SuppressWarnings("unchecked")
    private static List<List<Vector2>> parseCurrents(List<?> rowsRaw) {
        List<List<Vector2>> parsed = new ArrayList<>();
        if (rowsRaw == null) {
            return parsed;
        }
        for (Object rowObj : rowsRaw) {
            List<Vector2> row = new ArrayList<>();
            if (rowObj instanceof List<?> colsRaw) {
                for (Object cellObj : colsRaw) {
                    Vector2 cell = readVectorCell(cellObj);
                    row.add(cell);
                }
            }
            parsed.add(row);
        }
        return parsed;
    }

    private static Vector2 readVectorCell(Object cellObj) {
        if (cellObj instanceof List<?> vecRaw && vecRaw.size() >= 2) {
            double vx = asDouble(vecRaw.get(0));
            double vz = asDouble(vecRaw.get(1));
            return new Vector2(vx, vz);
        }
        if (cellObj instanceof ConfigurationSection section) {
            return new Vector2(section.getDouble("vx", 0.0), section.getDouble("vz", 0.0));
        }
        return Vector2.ZERO;
    }

    private static double asDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    public Vector2 getCurrentAt(double x, double z) {
        int cellX = (int) Math.floor(x / cellSize);
        int cellZ = (int) Math.floor(z / cellSize);
        if (cellZ < 0 || cellZ >= currents.size()) {
            return Vector2.ZERO;
        }
        List<Vector2> row = currents.get(cellZ);
        if (cellX < 0 || cellX >= row.size()) {
            return Vector2.ZERO;
        }
        return row.get(cellX);
    }

    public CellIndex getCellIndex(double x, double z) {
        return new CellIndex((int) Math.floor(x / cellSize), (int) Math.floor(z / cellSize));
    }

    public record CellIndex(int x, int z) {
    }
}
