# OceanDynamics

OceanDynamics is a Paper 1.21.1 plugin that adds configurable ocean currents, global wind, and crew bonuses for boats/rafts.

## Features

- **Grid-based ocean currents** using a configurable 2D vector field (`cellSize` + `currents` grid).
- **Global wind system** with direction/strength rerolled every `windChangeMinutes`.
- **Performance-first active boat loop**: physics runs only for active boats, never for all entities.
- **2-player crew bonus** for driver + passenger boats.
- **Compass action bar guidance** while riding with a compass:
  - mode toggle: `WIND` / `CURRENT`
  - cardinal direction + degrees + strength
- **Commands**:
  - `/oceandynamics wind`
  - `/oceandynamics reload`
  - `/oceandynamics debug`

## Build

```bash
mvn clean package
```

Output jar is in `target/`.

## Install

1. Build the jar with Maven.
2. Drop `OceanDynamics-1.0.0.jar` into your server `plugins/` directory.
3. Start/restart server.
4. Edit `plugins/OceanDynamics/config.yml` and run `/oceandynamics reload`.

## Config tuning tips

- `tickInterval` defaults to `2` (10Hz) for better performance.
- `currentPush` is the most important knob for “noticeable” currents.
- `currentWithMultiplier` controls extra help when moving with a current.
- `currentAgainstDrag` controls extra slowdown when fighting current.
- `windPush` should usually stay weaker than currents.
- `maxHorizontalSpeed` protects against runaway acceleration.
- `crewBonusMultiplier` should be modest (e.g. `1.10` to `1.25`).
- Keep current vectors in the `[-0.2, 0.2]` range first, then tune.

## Notes

- Currents apply in the overworld only.
- Cell lookup uses floor division for negatives:
  - `cellX = floor(x / cellSize)`
  - `cellZ = floor(z / cellSize)`
- Out-of-range cells safely default to zero current.
- Compass mode preference is stored in memory per player.
