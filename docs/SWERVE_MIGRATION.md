# Coaxial Swerve Migration (Pedro Pathing)

Mecanum remains the **production** drivetrain until swerve hardware is built and tuned.

## Switching drive mode

In `pedroPathing/Constants.java`:

```java
public static DriveConfiguration DRIVE_CONFIGURATION = DriveConfiguration.MECANUM;
```

Change to `SWERVE_SKELETON` only after hardware config and tuning are complete.

## Robot config (placeholders)

Add eight devices (names in `SwerveDriveSkeleton.java`):

| Pod | Angle motor | Drive motor |
|-----|-------------|-------------|
| FL | `fl_pod_angle` | `fl_pod_drive` |
| FR | `fr_pod_angle` | `fr_pod_drive` |
| BL | `bl_pod_angle` | `bl_pod_drive` |
| BR | `br_pod_angle` | `br_pod_drive` |

Keep `pinpoint` for localization.

## Tuning checklist

1. **Motor directions** — verify each pod angle/drive spins correctly in isolation.
2. **Analog encoder min/max** — pass real values into `SwerveDriveSkeleton.createPod(...)`.
3. **Pod offset (radians)** — measure mechanical zero vs field-forward per Pedro swerve setup.
4. **SwerveConstants** — set `xVelocity` / `yVelocity` from measured max speeds.
5. **Follower PID** — reuse or retune `FOLLOWER_CONSTANTS` translational/heading gains.
6. **Pedro swerve tuners** — run official Pedro swerve tuning OpModes after deps are available.
7. **Path smoke test** — short straight + turn path in teleop sandbox before enabling in autos.

## Code map

- `robot/drive/SwerveDriveSkeleton.java` — CoaxialPod factories and `createFollower()`
- `robot/drive/PedroMecanumDrive.java` — current production drive
- `pedroPathing/Constants.java` — `DriveConfiguration` flag and `createFollower()` switch

## Do not enable in match OpModes until

- [ ] All eight motors configured on Driver Station
- [ ] Pod offsets and analog ranges tuned
- [ ] Teleop drive feels stable (no module flip / wrong heading)
- [ ] Auto paths complete without large tracking error
