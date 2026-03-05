# OceanDynamics

OceanDynamics is a Paper plugin for smooth, route-based ocean travel with **SpeedyBoats-style velocity override** in `VehicleMoveEvent`.

## Highlights

- Tickless movement architecture for currents/wind (no repeating physics push loop).
- 500x500 (configurable) current vector cells with O(1) lookup.
- Global wind with random or gradual direction updates every N minutes.
- Strong “with current fast / against current slow” behavior.
- 2-player crew bonus multiplier.
- Compass actionbar while riding:
  - toggle between `WIND` and `CURRENT` via right-click compass
  - displays 16-point heading + degrees + magnitude.

## Build

```bash
mvn clean package
```

Jar output: `target/OceanDynamics-1.0.0.jar`

## Install

1. Build the plugin.
2. Copy jar to your Paper server `plugins/` folder.
3. Start or restart the server.
4. Edit `plugins/OceanDynamics/config.yml`.
5. Run `/oceandynamics reload`.

## Commands

- `/oceandynamics reload` (permission: `oceandynamics.admin`)
- `/oceandynamics wind`
- `/oceandynamics debug`

## Tuning advice

Start here for noticeable, controllable behavior:

- `baseSpeed`: Always-on cruise speed. Raise to make ocean travel generally faster.
- `currentPush` + `currentWithMultiplier`: Main route-learning reward knobs.
- `currentAgainstPenalty`: Main anti-current slowdown knob.
- `windPush` + `windWithMultiplier`: Secondary boost source.
- `windAgainstPenalty`: Keep lower than current penalty for better feel.
- `minSpeed`: Prevent total stall when heavily anti-aligned.
- `maxSpeed` + `maxHorizontalSpeed`: Safety caps.
- `driftScaleCurrent` / `driftScaleWind`: Small lateral realism; keep subtle.
- `smoothingFactor`: 0.0 = immediate changes, ~0.15 = smoother cell transitions.

## Architecture note

Boat movement uses **boat facing direction** (`Location#getDirection`) and overrides horizontal velocity on `VehicleMoveEvent`, preserving Y velocity. This follows the SpeedyBoats-style approach for smooth handling without scheduler fight/jitter.
