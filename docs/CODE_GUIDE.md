# Code Guide — Where Everything Lives & How It Works

Quick reference for new programmers on the team. Read this before diving into Java files.

**Also see:** [`TeamCode/AGENTS.md`](../TeamCode/AGENTS.md) (AI/package rules), [`FIELD_COORDINATES.md`](FIELD_COORDINATES.md) (waypoints), [`BUILD_AND_DEPLOY.md`](BUILD_AND_DEPLOY.md) (build + Dashboard).

---

## Big picture

```
Driver Station picks OpMode
        │
        ▼
┌───────────────────┐     init once      ┌─────────────────────┐
│ MainTeleop        │ ─────────────────► │ RobotContainer      │
│ DemoTeleop        │                    │ (all subsystems)    │
│ Auto2025*         │                    └──────────┬──────────┘
└───────────────────┘                               │
        │                                           │
        │  each loop (20 ms)                        │
        ▼                                           ▼
┌───────────────────┐                    ┌─────────────────────┐
│ TeleopController  │ ◄── uses ────────│ drive, shooter,     │
│ (teleop only)     │                    │ turret, intake, led │
└───────────────────┘                    └─────────────────────┘
        │
        │  autos use shared actions instead
        ▼
┌───────────────────────────────────────────┐
│ robot/auto/actions/*  +  util/Actions     │
└───────────────────────────────────────────┘
```

**Golden rule:** OpModes are thin. They create `RobotContainer`, then call `update()` or run action sequences. **Never** construct motors/servos directly in an OpMode.

---

## Folder map (active production code)

| Path | What it is |
|------|------------|
| `opmodes/teleop/MainTeleop.java` | Competition teleop entry point (~100 lines) |
| `opmodes/teleop/DemoTeleop.java` | Showcase mode — drive off, G2 controls shooter |
| `opmodes/auto/Auto2025*.java`, `Auto12Ball.java` | Competition autonomous routines |
| `robot/RobotContainer.java` | **Single hardware init** — wires every subsystem |
| `robot/RobotState.java` | Pose, goal, alliance, shot distances (shared state) |
| `robot/hardware/DeviceNames.java` | Config names (`fl`, `launch`, `Webcam 1`, …) |
| `robot/drive/` | Drive abstraction + Pedro mecanum implementation |
| `robot/shooter/` | Flywheel, hood, shot math, teleop/auto shoot logic |
| `robot/turret/` | Turret motor PID + aim math |
| `robot/intake/` | Intake motor, transfer belt, ball sensors, AirSort |
| `robot/vision/` | Webcam + AprilTag processor |
| `robot/led/` | LED servo colors |
| `robot/field/` | Shot physics, shooting zones, camera relocalization |
| `robot/teleop/` | Gamepad bindings + main teleop loop |
| `robot/auto/actions/` | Reusable autonomous `Action` classes |
| `pedroPathing/Constants.java` | Pedro follower tuning (mecanum vs swerve skeleton) |
| `util/` | `Pose2d`, `Action`, `FollowPathAction`, path helpers |
| `RR/PoseBridge.java` | Pass pose/alliance from auto → teleop between OpModes |
| `RR/Drawing.java` | FTC Dashboard field overlay helpers (optional) |
| `_archive/` | Old code — **do not import** |

---

## Startup flow (teleop)

1. **MainTeleop** sets bulk caching on hubs, syncs alliance constants to `RobotState`.
2. **`RobotContainer.init(hardwareMap, gamepad1, gamepad2, enableVision=true)`**
   - Builds flywheel, hood, intake, transfer, ball sensors, LED.
   - Builds `ShootSequence` (auto API) and `ShooterTeleop` (teleop API).
   - Reads `PoseBridge` if coming from auto; else uses alliance start pose.
   - Creates `PedroMecanumDrive` (owns Pinpoint + mecanum motors).
   - Creates `TurretSubsystem`.
   - If vision enabled: `AprilTagVision` + `AirSort`.
3. **Alliance select** in init loop (G1 dpad left/right).
4. **`waitForStart()`** then loop:
   - `robot.updateOdometry()` (inside TeleopController)
   - `TeleopController.update(settings)` — drive, aim, shoot, LED, airsort
   - `sleep(20)`

---

## Subsystem ownership (who owns what hardware)

| Subsystem | Hardware | Does NOT own |
|-----------|----------|--------------|
| `PedroMecanumDrive` | `fl/bl/fr/br`, Pinpoint (via Pedro) | Turret, shooter |
| `FlywheelSubsystem` | `launch` motor | Intake routing |
| `HoodSubsystem` | `hood` servo | Distance regression (delegates to `ShotCalculator`) |
| `IntakeSubsystem` | `intake` motor | Flywheel PID |
| `TransferSubsystem` | `trans` motor | Gamepad reads |
| `BallSensorArray` | `c1`, `c2`, `c3` | AirSort motif logic |
| `TurretSubsystem` | `turret` motor | OpMode lock-on state machine |
| `AprilTagVision` | `Webcam 1` | AirSort business rules |
| `LedSubsystem` | `LED` servo | Zone geometry |
| `AirSort` | (no hardware — uses vision + c3) | Flywheel PID |

---

## Shooter pipeline

### Teleop (`ShooterTeleop.updateTeleop`)

1. **Mode selection** (G2 left bumper = preset, else manual Dashboard, else airsort preset, else distance regression).
2. **G2 left trigger** → `flywheel.updateSpinning()` (PID+FF toward target).
3. **G2 right trigger** → feed transfer+intake **only if** `flywheel.isAtTarget()`.
4. **G1** → intake/transfer overrides, velocity nudge (dpad up/down).

### Auto (`ShootSequence`)

| Method | Purpose |
|--------|---------|
| `setHoodAndFlywheelFromDistance(in)` | Regression from inches |
| `runLauncherAuto(feed)` | Spin flywheel; optionally feed when at speed |
| `stopLauncherAuto()` | Stop flywheel + intake + transfer |
| `runIntakeAuto(run)` | Intake + light transfer for picking up |

Distance comes from **`RobotState.getShotDistanceInches()`** (uses velocity-comp distance when enabled).

### Shot math chain

```
RobotState pose + goal
        → TurretAim.solveAim()     (optional velocity compensation)
        → ShotCalculator           (hood angle + flywheel ticks regression)
        → HoodSubsystem / FlywheelSubsystem
```

Tune regression on **FTC Dashboard** under `FlywheelSubsystem`, `ShooterTeleop`, `HoodSubsystem`.

---

## Turret

- **Field-relative aim (default):** `TurretSubsystem.runTurretGyro()` — holds `targetAngle` in field frame using robot heading from odometry.
- **Manual lock-on (G2 Y):** `runTurretNoGyro(frozenHeading)` — stick moves turret relative to robot.
- **Resync (G2 X or Start):** `resyncEncoder()` — fixes encoder drift after gear slip.
- **Aim math:** `TurretAim.solveAim()` — computes angle to goal + compensated virtual goal for hood distance.

---

## Drive (Pedro)

- **`PedroMecanumDrive`** wraps Pedro `Follower`.
- Poses in code use **official FTC center frame** (inches). Pedro corner frame (+72) is converted in `util/FieldCoordinates` only inside the drive layer.
- **Teleop:** `setTeleOpDrive(ly, -lx, -turn, fieldCentric=false)` from `TeleopController`.
- **Auto:** `buildPath` / `buildPathChain` → `FollowPathAction(robot.drive, path)`.

---

## Vision & AirSort

- **`AprilTagVision`:** AprilTag processor on `Webcam 1`. Decimation 3 normally, 2 when airsort active.
- **`AirSort`:** Reads tags 21/22/23 once to learn GPP motif. Uses **c3 only** to decide fast vs slow shot for current ball color.
- Toggle airsort: **G2 Back** in MainTeleop.

---

## Autonomous pattern

Every auto follows the same skeleton:

```java
RobotContainer robot = new RobotContainer();
robot.init(hardwareMap, gamepad1, gamepad2, true);  // or false if no vision
robot.state.setAllianceBlue(...);
robot.drive.setPose(startPose);

Action sequence = new SequentialAction(
    new FollowPathAction(robot.drive, path),
    new RevAndAimAction(robot, goalX, goalY, 1.0),
    new ShootAction(robot, goalX, goalY, 1.0),
    ...
);
Actions.runBlocking(sequence, telemetry);
PoseBridge.setPose(robot.drive.getPose());  // optional handoff to teleop
```

### Shared actions (`robot/auto/actions/`)

| Class | What it does |
|-------|--------------|
| `FollowPathAction` | Follow one Pedro path until done (`util/`) |
| `RevAndAimAction` | Spin flywheel, set hood, aim turret for N seconds |
| `ShootAction` | Same + feed balls when at speed |
| `IntakeRunAction` / `TransRunAction` | Run intake/transfer for N seconds (parallel with path) |
| `TurretAimAction` | Continuous aim while another action runs |
| `HoodAndFlywheelUpdateAction` | Update regression while moving |
| `StartIntakeAction` / `StopIntakeAction` | Far-auto intake on/off |
| `StopLauncherAction` | Stop shooter stack |
| `LedFadeAction` | Sine LED fade for demo/auto flair |

---

## Gamepad map (MainTeleop)

| Input | Action |
|-------|--------|
| **G1** sticks | Drive (scaled by `driveScale`, slow toggles on dpad L/R press) |
| **G1** dpad U/D | Flywheel velocity nudge ±50 |
| **G1** triggers/bumpers | Intake in/out, transfer |
| **G2** left trigger | Rev flywheel |
| **G2** right trigger | Shoot (feed when at speed) |
| **G2** dpad | Turret field hold (up=0°, down=180°) or nudge via stick |
| **G2** Y / B | Lock-on manual turret / unlock |
| **G2** X or Start | Turret encoder resync |
| **G2** Back | Toggle AirSort |
| Init **G1** dpad L/R | Select Blue / Red alliance |

Demo mode moves **all** controls to G2; drive is forced to zero.

---

## Live telemetry (FTC Dashboard + Panels)

**MainTeleop** publishes every loop via `robot/telemetry/LiveTelemetry.java`:

- **FTC Dashboard** (default): connect to `http://192.168.43.1:8080/dash` — field overlay shows robot pose trail, goal (green), aim line (yellow), virtual goal (orange when vel comp on), close/far zone triangles, and numeric `packet.put` keys (`shooting_zone`, `flywheel_vel`, `airsort_*`, etc.).
- **Panels** (optional): set `MainTeleop.usePanelsDashboard = true` on Dashboard config, open [panels.bylazar.com](https://panels.bylazar.com), connect to robot. Telemetry mirrors Driver Station; use Panels Field widget for pose (FTC Dashboard overlay is skipped in this mode).

`DemoTeleop` also mirrors telemetry to FTC Dashboard (or Panels when toggled).

## FTC Dashboard tuning

Connect to `http://192.168.43.1:8080/dash` while OpMode runs.

| Config class | Tune what |
|--------------|-----------|
| `MainTeleop` | Alliance poses, velocity comp, drive scale |
| `RobotState` | Default start/goal (shared with autos) |
| `FlywheelSubsystem` | PID, FF, shoot tolerance |
| `ShooterTeleop` | Manual mode, hood/vel targets, airsort flag |
| `HoodSubsystem` | Angle ↔ servo mapping |
| `TurretSubsystem` | Turret PID, limits, nudge |
| `ShootingZones` | Close/far zone triangles |
| `CameraRelocalization` | Tag pose scale |
| `LedSubsystem.LedConfig` | LED servo positions |

Legacy `IntakeV2.*` Dashboard paths are gone — re-save under the classes above.

---

## Nullable fields (know before you edit)

| Field | When null | Safe usage |
|-------|-----------|------------|
| `robot.vision` | `init(..., enableVision=false)` | Check `!= null` before processor access |
| `robot.airSort` | Vision disabled | Check `!= null` before airsort update |
| `bindings.driver` | DemoTeleop (`null`, G2 only) | `TeleopBindings` methods guard this |
| `bindings.operator` | Should never be null in MainTeleop | TeleopController returns early if missing |

`RobotContainer.updateOdometry()` throws if `init()` was not called — intentional fail-fast.

---

## Common tasks — where to edit

| I want to… | Edit this |
|------------|-----------|
| Change auto waypoints | `opmodes/auto/Auto2025*.java` pose constants |
| Change start pose / goal | `MainTeleop` @Config or `RobotState` |
| Tune flywheel PID | `FlywheelSubsystem` @Config |
| Tune hood regression | `ShotCalculator` constants (or Dashboard manual mode) |
| Change intake behavior teleop | `ShooterTeleop.updateIntakeTeleop` |
| Change turret PID | `TurretSubsystem` @Config |
| Add a new auto action | `robot/auto/actions/NewAction.java`, use in auto file |
| Add hardware device | `DeviceNames.java` + new subsystem + `RobotContainer.init` |
| Change LED colors | `LedSubsystem.LedConfig` |
| Change shooting zones | `ShootingZones` @Config |

---

## What NOT to touch

- `FtcRobotController/` — SDK copy
- `_archive/` — dead code
- `Toros/` — legacy; no production imports
- Negating X/Y in OpModes for “coordinate fixes” — use `FieldCoordinates` / `docs/FIELD_COORDINATES.md`

---

## Build check

```powershell
.\gradlew :TeamCode:compileDebugJavaWithJavac
```

Must pass before pushing. Full APK build may hit local D8 issues; compile is the source of truth for Java errors.
