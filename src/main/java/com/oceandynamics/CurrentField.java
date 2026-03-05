package com.oceandynamics;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.logging.Logger;

public final class CurrentField {

    private final int cellSize;
    private final Vector[][] grid;
    private final int width;
    private final int height;

    private CurrentField(int cellSize, Vector[][] grid, int width, int height) {
        this.cellSize = cellSize;
        this.grid = grid;
        this.width = width;
        this.height = height;
    }

    public static CurrentField fromConfig(FileConfiguration config, Logger logger) {
        int cellSize = Math.max(1, config.getInt("cellSize", 500));
        List<?> rows = config.getList("currents");
        if (rows == null || rows.isEmpty()) {
            logger.warning("No currents grid found in config; defaulting to zero current.");
            return new CurrentField(cellSize, new Vector[0][0], 0, 0);
        }

        int height = rows.size();
        int width = 0;
        for (Object rowObj : rows) {
            if (rowObj instanceof List<?> rowList) {
                width = Math.max(width, rowList.size());
            }
        }

        Vector[][] grid = new Vector[height][Math.max(0, width)];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                grid[z][x] = new Vector(0, 0, 0);
            }
        }

        for (int z = 0; z < height; z++) {
            Object rowObj = rows.get(z);
            if (!(rowObj instanceof List<?> row)) {
                logger.warning("Invalid current row format at z=" + z + ", expected list.");
                continue;
            }

            for (int x = 0; x < Math.min(width, row.size()); x++) {
                Object cellObj = row.get(x);
                if (!(cellObj instanceof List<?> cell) || cell.size() < 2) {
                    logger.warning("Invalid current cell format at x=" + x + " z=" + z + ", expected [vx,vz].");
                    continue;
                }

                Double vx = toDouble(cell.get(0));
                Double vz = toDouble(cell.get(1));
                if (vx == null || vz == null) {
                    logger.warning("Invalid numeric current vector at x=" + x + " z=" + z + ".");
                    continue;
                }

                grid[z][x] = new Vector(vx, 0, vz);
            }
        }

        logger.info("Loaded current grid " + width + "x" + height + " with cellSize=" + cellSize + ".");
        return new CurrentField(cellSize, grid, width, height);
    }

    public Vector getCurrentAt(double worldX, double worldZ) {
        if (width == 0 || height == 0) {
            return new Vector(0, 0, 0);
        }

        int cellX = floorDiv(worldX, cellSize);
        int cellZ = floorDiv(worldZ, cellSize);
        if (cellX < 0 || cellX >= width || cellZ < 0 || cellZ >= height) {
            return new Vector(0, 0, 0);
        }

        return grid[cellZ][cellX].clone();
    }

    public int getCellSize() {
        return cellSize;
    }

    public int getCellX(double worldX) {
        return floorDiv(worldX, cellSize);
    }

    public int getCellZ(double worldZ) {
        return floorDiv(worldZ, cellSize);
    }

    private static int floorDiv(double value, int divisor) {
        return (int) Math.floor(value / divisor);
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
