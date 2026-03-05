# OceanDynamics

OceanDynamics is a brand-new Paper plugin for **Minecraft 1.21.x** that adds meaningful ocean route gameplay via a current grid + global wind model.

## Core design

This plugin uses **VehicleMoveEvent velocity override (SpeedyBoats-style)** for smooth motion:
- no repeating physics task that nudges boats,
- velocity continuously aligned to boat facing direction,
- currents/wind translated into stable forward speed changes.

## Features

- **500x500 cell vector field currents** (configurable `cellSize` and full 2D vector grid).
- **Global wind** with random or gradual direction updates every configurable minutes.
- **With current fast / against current slow** speed model.
- **2-player crew bonus** multiplier.
- **Compass actionbar UI** while riding and holding a compass:
  - `WIND` mode: direction + degrees + strength
  - `CURRENT` mode: local cell current direction + degrees + magnitude
- **Commands**:
  - `/oceandynamics reload`
  - `/oceandynamics wind`
  - `/oceandynamics debug`

## Build

```bash
mvn package
```

The jar is produced under `target/`.

## Install

1. Build with Maven.
2. Copy `target/OceanDynamics-1.0.0.jar` into `plugins/` on your Paper server.
3. Start the server once.
4. Edit `plugins/OceanDynamics/config.yml` as needed.
5. Run `/oceandynamics reload`.

## Tuning advice

Start here for noticeable but controllable travel:

- `baseSpeed`: your baseline cruise speed (always on).
- `currentPush` + `currentWithMultiplier`: biggest contributor to route reward.
- `currentAgainstPenalty`: make this meaningful so bad route choices are felt.
- `windPush` and `windAgainstPenalty`: typically weaker than current effects.
- `minSpeed` and `maxSpeed`: keep gameplay fair and avoid absurd boosts.
- `crewBonusMultiplier`: keep modest (`1.10`–`1.25`).
- `driftScaleCurrent` / `driftScaleWind`: leave low unless you want harder steering.
- `smoothingFactor`: use around `0.15` if cell transitions feel abrupt.

## Notes

- Uses boat facing direction from `location.getDirection()` (not current velocity direction) to avoid feedback jitter.
- Preserves vertical Y velocity while replacing horizontal XZ velocity each move event.
- Out-of-grid lookups return zero current.
