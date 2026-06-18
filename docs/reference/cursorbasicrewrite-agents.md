# CursorBasicRewrite — Agent Context

Cached from [CursorBasicRewrite/TeamCode/AGENTS.md](https://github.com/Pleasantnoob/CursorBasicRewrite/blob/master/TeamCode/AGENTS.md) and [ROBOT_CAPABILITIES.md](https://github.com/Pleasantnoob/CursorBasicRewrite/blob/master/docs/ROBOT_CAPABILITIES.md).

## Repository

- **GitHub:** https://github.com/Pleasantnoob/CursorBasicRewrite
- **SDK:** FTC 11.1.0, Pedro Pathing 2.0.6, FTCLib 2.1.1
- **RC:** REV Control Hub (`192.168.43.1`)

## Agent Teleop

New OpMode: **`Agent Teleop`** — start via MCP `robot_start_opmode` or Driver Station.

## Hardware names

| Device | Config name |
|--------|-------------|
| Mecanum | `fl`, `bl`, `fr`, `br` |
| Odometry | `pinpoint` |
| Intake | `intake` |
| Flywheel | `launch` |
| Transfer | `trans` |
| Hood | `hood` |
| Turret | `turret` |
| Colors | `c1`, `c2`, `c3` |
| Webcam | `Webcam 1` |

## MCP command mapping

| MCP tool | Robot action |
|----------|--------------|
| `robot_drive` | `DriveSubsystem.setTeleOpDrive` |
| `robot_set_motor` | Direct motor power/velocity |
| `robot_set_servo` | hood / turret position |
| `robot_follow_path` | Pedro `buildPathChain` + `followPath` |
| `robot_get_vision` | AprilTag detections from `AprilTagVision` |
| `robot_set_config` | Panels Configurables live tuning |

## Robot capabilities (summary)

- **Drive:** Mecanum, Pinpoint odometry, Pedro pathing
- **Shooter:** Flywheel PID, hood servo, regression-based auto shots
- **Turret:** Field-angle aim, lock-on via AprilTags 20/24
- **Intake/transfer:** Trigger/bumper controlled
- **Vision:** AprilTags for goal (20/24) and obelisk motif (21-23)
- **Coordinates:** Official FTC center-origin inches (+X from Red wall, +Y toward Blue)

## Subsystem rules

- One Pinpoint owner via `PedroMecanumDrive`
- No gamepad reads inside subsystems
- Use `RobotState` for pose and shot distance
- Field coords: see `docs/FIELD_COORDINATES.md` in robot repo
