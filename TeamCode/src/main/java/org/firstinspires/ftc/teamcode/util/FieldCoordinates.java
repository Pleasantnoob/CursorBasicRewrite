package org.firstinspires.ftc.teamcode.util;

import com.pedropathing.geometry.Pose;

/**
 * FTC official field coordinate frame used everywhere in team code.
 *
 * <p>Reference: <a href="https://ftc-docs.firstinspires.org/en/latest/game_specific_resources/field_coordinate_system/field-coordinate-system.html">
 * FIRST Tech Challenge Field Coordinate System</a> (viewed from the Red Alliance wall).
 *
 * <ul>
 *   <li><b>Origin</b> — center of the mat (0, 0), Z = 0 on tile surface</li>
 *   <li><b>+X</b> — to the right (parallel to the Red wall)</li>
 *   <li><b>+Y</b> — away from the Red wall toward the Blue alliance</li>
 *   <li><b>Heading</b> — radians; 0 = facing +X; positive rotation = counterclockwise (right-hand rule about +Z)</li>
 *   <li><b>Units</b> — inches in {@link Pose2d} and all OpMode waypoints</li>
 * </ul>
 *
 * <p>Square field (Into The Deep / similar): +X points toward the rear of the field (away from the
 * audience); the audience side is negative X. Goals on the back wall use X ≈ −70; red goal is +Y,
 * blue goal is −Y.
 *
 * <p><b>Pedro Pathing</b> uses a corner origin on [0, 144] × [0, 144]. Convert at the drive layer
 * only — never store Pedro corner coordinates in OpModes or subsystems.
 */
public final class FieldCoordinates {
    /** Half-width of a 12×12 ft field in inches (±72 from center). */
    public static final double FIELD_HALF_SIZE_IN = 72.0;

    /** Add to FTC center X/Y to get Pedro corner-frame coordinates. */
    public static final double PEDRO_CENTER_OFFSET_IN = 72.0;

    private FieldCoordinates() {}

    /** FTC center-frame pose → Pedro internal corner-frame pose (heading unchanged). */
    public static Pose toPedro(Pose2d ftc) {
        return new Pose(
                ftc.position.x + PEDRO_CENTER_OFFSET_IN,
                ftc.position.y + PEDRO_CENTER_OFFSET_IN,
                ftc.heading);
    }

    /** Pedro internal corner-frame pose → FTC center-frame pose (heading unchanged). */
    public static Pose2d fromPedro(Pose pedro) {
        return new Pose2d(
                pedro.getX() - PEDRO_CENTER_OFFSET_IN,
                pedro.getY() - PEDRO_CENTER_OFFSET_IN,
                pedro.getHeading());
    }

    /** Field-frame angle (radians) from (fromX, fromY) to (toX, toY) using FTC axes. */
    public static double angleToPointRad(double fromX, double fromY, double toX, double toY) {
        return Math.atan2(toY - fromY, toX - fromX);
    }

    /** True if (x, y) is inside the 144×144 inch field (center origin). */
    public static boolean isOnField(double x, double y) {
        return Math.abs(x) <= FIELD_HALF_SIZE_IN && Math.abs(y) <= FIELD_HALF_SIZE_IN;
    }
}
