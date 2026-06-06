# Field coordinates

All TeamCode poses use the **official FTC field coordinate system**. Do not invent per-OpMode axis flips.

**Official reference:** [FTC Field Coordinate System](https://ftc-docs.firstinspires.org/en/latest/game_specific_resources/field_coordinate_system/field-coordinate-system.html)

## Axes (square field, viewed from Red Alliance wall)

| Axis | Direction | Range (12 ft field) |
|------|-----------|---------------------|
| **X** | Right (+X increases to the right) | −72 … +72 in from center |
| **Y** | Away from Red wall toward Blue (+Y increases outward) | −72 … +72 in from center |
| **Z** | Up from the mat | inches above tiles |
| **Heading** | 0 rad = facing **+X**; positive = **counterclockwise** | radians in code, degrees in Dashboard |

On a typical square field (Into The Deep style):

- **Audience** is on the **negative X** side.
- **Rear / back wall** is on the **positive X** side.
- **Red goal** is on the back wall at **positive Y** (e.g. `(-70, +70)`).
- **Blue goal** is on the back wall at **negative Y** (e.g. `(-70, -70)`).

## Code conventions

- Store poses as `util.Pose2d` in **FTC center frame** only.
- Canonical helpers: `util.FieldCoordinates` (conversion, angle helpers).
- **Pedro Pathing** uses corner origin `[0, 144]` — conversion is automatic in `PedroMecanumDrive` via `+72` on X and Y. Never add your own ±72 in OpModes.
- **FTC Dashboard** `Drawing` overlays use the same center frame (matches official Field View).
- **AprilTag** field positions in the game manual / SDK use this same frame — tag constants should match without extra negation.

## Alliance start poses

`RobotState` / `MainTeleop` start and goal constants are in FTC inches. Tune them on the Dashboard; they should match where the robot physically sits on the mat and where you aim.

## Common mistakes to avoid

1. **Do not** document “−X = right” or “−Y = forward” — that is not the FTC frame.
2. **Do not** negate X/Y “because red alliance” — alliance is handled by separate red/blue constants, not by flipping axes.
3. **Do not** mix Pedro corner coords `(0–144)` into autos or teleop; always use center coords.

## Related files

- `util/FieldCoordinates.java` — single source of truth in code
- `robot/drive/PedroMecanumDrive.java` — Pedro ↔ FTC conversion at drive boundary
- `RR/Drawing.java` — Dashboard overlay (center frame)
- `Toros/Drive/CameraRelocalization.java` — tag pose math in center frame
