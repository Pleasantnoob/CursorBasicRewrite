package org.firstinspires.ftc.teamcode.util;

/**
 * Robot pose in the official FTC field frame (center origin, inches, heading radians).
 * See {@link FieldCoordinates} for axis definitions and Pedro conversion.
 */
public final class Pose2d {

    public final Vector2d position;
    /** Heading in radians. */
    public final double heading;

    public Pose2d(double x, double y, double headingRad) {
        this.position = new Vector2d(x, y);
        this.heading = headingRad;
    }

    public Pose2d(Vector2d position, double headingRad) {
        this.position = position;
        this.heading = headingRad;
    }

    /** Unit vector in heading direction. */
    public Vector2d headingVec() {
        return new Vector2d(Math.cos(heading), Math.sin(heading));
    }
}
