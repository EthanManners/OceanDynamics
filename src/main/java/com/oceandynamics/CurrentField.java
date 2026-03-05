package com.oceandynamics;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class CurrentField {
    private final OceanDynamics plugin;
    private int cellSize;
    private double[][][] grid;

    public CurrentField(OceanDynamics plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.cellSize = Math.max(1, config.getInt("cellSize", 500));

        List<?> rowsRaw = config.getList("currents", new ArrayList<>());
        int rows = rowsRaw.size();
        int cols = 0;

        List<List<double[]>> parsed = new ArrayList<>();
        for (Object rowObj : rowsRaw) {
            if (!(rowObj instanceof List<?> rowList)) {
                continue;
            }
            List<double[]> row = new ArrayList<>();
            for (Object cellObj : rowList) {
                if (cellObj instanceof List<?> pair && pair.size() >= 2) {
                    double vx = toDouble(pair.get(0));
                    double vz = toDouble(pair.get(1));
                    row.add(new double[]{vx, vz});
                } else {
                    row.add(new double[]{0.0, 0.0});
                }
            }
            cols = Math.max(cols, row.size());
            parsed.add(row);
        }

        this.grid = new double[Math.max(rows, 0)][Math.max(cols, 0)][2];
        for (int z = 0; z < parsed.size(); z++) {
            List<double[]> row = parsed.get(z);
            for (int x = 0; x < row.size(); x++) {
                double[] vec = row.get(x);
                grid[z][x][0] = vec[0];
                grid[z][x][1] = vec[1];
            }
        }

        plugin.getLogger().info("Loaded current field: " + rows + " rows, max " + cols + " cols, cellSize=" + cellSize);
    }

    public int getCellSize() {
        return cellSize;
    }

    public Vector getCurrentVector(double x, double z) {
        if (grid.length == 0) {
            return new Vector(0, 0, 0);
        }

        int cellX = (int) Math.floor(x / cellSize);
        int cellZ = (int) Math.floor(z / cellSize);

        if (cellZ < 0 || cellZ >= grid.length) {
            return new Vector(0, 0, 0);
        }
        if (grid[cellZ].length == 0 || cellX < 0 || cellX >= grid[cellZ].length) {
            return new Vector(0, 0, 0);
        }

        double vx = grid[cellZ][cellX][0];
        double vz = grid[cellZ][cellX][1];
        return new Vector(vx, 0, vz);
    }

    private double toDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }
}
