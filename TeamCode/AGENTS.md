# AGENTS.md — AI context for CursorBasicRewrite

Read this file **before** editing TeamCode. Also read [`docs/CODE_GUIDE.md`](../docs/CODE_GUIDE.md) (human map), [`docs/BUILD_AND_DEPLOY.md`](../docs/BUILD_AND_DEPLOY.md).

## Repository

- **GitHub:** https://github.com/Pleasantnoob/CursorBasicRewrite
- **Branch:** `CursorBasicRewrite` (rewrite work); `master` for stable merges
- **SDK:** FTC 11.1.0, Pedro Pathing 2.0.6, FTCLib 2.1.1

## Package map (active code)

```
org.firstinspires.ftc.teamcode/
├── robot/                    # All robot hardware + logic (USE THIS)
│   ├── RobotContainer.java   # Single init point; wires subsystems
│   ├── RobotState.java       # Pose, goal, alliance, shot distances (no OpMode statics)
│   ├── hardware/DeviceNames.java
│   ├── drive/                # DriveSubsystem, PedroMecanumDrive, SwerveDriveSkeleton
│   ├── field/                # ShotPhysics, ShootingZones, CameraRelocalization
│   ├── turret/               # TurretSubsystem, TurretAim
│   ├── shooter/              # FlywheelSubsystem, HoodSubsystem, ShootSequence, ShooterTeleop
│   ├── intake/               # IntakeSubsystem, TransferSubsystem, BallSensorArray, AirSort
│   ├── led/LedSubsystem.java
│   ├── vision/AprilTagVision.java
│   ├── teleop/               # TeleopBindings, TeleopController
│   └── auto/actions/         # Shared autonomous actions
├── opmodes/
│   ├── teleop/MainTeleop.java    # Production teleop (replaces MainDrive)
│   └── teleop/DemoTeleop.java
│   └── auto/                     # Pedro competition autos
├── pedroPathing/Constants.java # DriveConfiguration: MECANUM (default) | SWERVE_SKELETON
├── util/                         # Pose2d, Actions (Pedro auto helpers)
├── RR/                           # PoseBridge, Drawing only (RR drive archived)
└── _archive/                     # Dead code — do not import from active code
```

## Hardware config names (Robot Configuration on Driver Station)

| Device | Name in config |
|--------|----------------|
| Mecanum FL/BL/FR/BR | `fl`, `bl`, `fr`, `br` |
| Pinpoint odometry | `pinpoint` |
| Intake motor | `intake` |
| Flywheel | `launch` |
| Transfer belt | `trans` |
| Hood servo | `hood` |
| Turret | `turret` |
| Color sensors | `c1`, `c2`, `c3` |
| LED | `LED` |
| Webcam | `Webcam 1` (with space) |

**Swerve (future):** `fl_pod_drive`, `fl_pod_angle`, etc. — see `docs/SWERVE_MIGRATION.md`

## Production OpModes (Driver Station)

**TeleOp:** `MainTeleop`, `Demo / Showcase`

**Autonomous:** `Auto2025RedNear`, `Auto2025BlueNear`, `Auto2025RedFar`, `Auto2025BlueFar`, `Auto 12-Ball (Blue)`

## Do not edit

- `FtcRobotController/` (SDK module)
- `_archive/` except to add more retired files
- `local.properties` (machine-specific, gitignored)

## Subsystem ownership rules

- **One Pinpoint owner:** Pedro Follower via `PedroMecanumDrive` — subsystems must NOT construct their own Pinpoint.
- **No gamepad reads in subsystems** except via `TeleopBindings` passed into update methods.
- **Shot distance** comes from `RobotState`, never from OpMode static getters.
- **No production imports** from `org.firstinspires.ftc.teamcode.Toros.Drive`.
- **Comments:** Javadoc on every public class; `// --- Section ---` in long methods; explain *why*.

## Dashboard migration notes

- Dashboard launcher tuning lives in `ShooterTeleop.*` and `FlywheelSubsystem.*`.
- Legacy `IntakeV2.*` dashboard fields are retired; migrate any saved configs to the new classes.

## Coordinates

**Official FTC field frame** (center origin, inches): +X right from Red wall, +Y toward Blue alliance, heading 0 = +X CCW+. Read `docs/FIELD_COORDINATES.md` and `util/FieldCoordinates.java` before adding waypoints. Pedro corner frame (+72 offset) is converted only in `PedroMecanumDrive` — never negate axes ad hoc in OpModes.
