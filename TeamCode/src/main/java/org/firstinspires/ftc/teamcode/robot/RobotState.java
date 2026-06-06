package org.firstinspires.ftc.teamcode.robot;

import com.acmerobotics.dashboard.config.Config;
import org.firstinspires.ftc.teamcode.util.Pose2d;
import org.firstinspires.ftc.teamcode.util.Vector2d;

/**
 * Shared field state updated each loop. All positions use the official FTC field frame
 * ({@link org.firstinspires.ftc.teamcode.util.FieldCoordinates}).
 */
@Config
public class RobotState {
    // --- Alliance poses (FTC inches / degrees). Goals: red at (−X, +Y), blue at (−X, −Y). ---
    public static double blueStartX = -50.0, blueStartY = -50.0, blueStartHeadingDeg = -128.0;
    public static double blueGoalX = -70.0, blueGoalY = -70.0;
    public static double redStartX = -50.0, redStartY = 50.0, redStartHeadingDeg = 128.0;
    public static double redGoalX = -70.0, redGoalY = 70.0;

    /** Velocity compensation for hood/speed distance (set by teleop when enabled). */
    public boolean turretVelocityCompensation = false;
    public double distanceForHoodSpeedInches = Double.NaN;

    private boolean blueAlliance = true;
    private double startX, startY, startHeadingDeg;
    private double goalX, goalY;
    private Pose2d pose = new Pose2d(0, 0, 0);
    private double distanceToGoalInches;
    private double deltaXToGoal;
    private double deltaYToGoal;
    private double cameraDistanceInches = Double.NaN;

    public void setAllianceBlue(boolean blue) {
        blueAlliance = blue;
        if (blue) {
            startX = blueStartX; startY = blueStartY; startHeadingDeg = blueStartHeadingDeg;
            goalX = blueGoalX; goalY = blueGoalY;
        } else {
            startX = redStartX; startY = redStartY; startHeadingDeg = redStartHeadingDeg;
            goalX = redGoalX; goalY = redGoalY;
        }
    }

    public boolean isBlueAlliance() { return blueAlliance; }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getStartHeadingDeg() { return startHeadingDeg; }
    public double getGoalX() { return goalX; }
    public double getGoalY() { return goalY; }

    public Pose2d getPose() { return pose; }
    public void setPose(Pose2d p) { pose = p; }

    /** Call each loop after odometry update. Ignores null pose. */
    public void updateFromPose(Pose2d currentPose) {
        if (currentPose == null || currentPose.position == null) return;
        pose = currentPose;
        deltaXToGoal = pose.position.x - goalX;
        deltaYToGoal = pose.position.y - goalY;
        distanceToGoalInches = Math.hypot(deltaXToGoal, deltaYToGoal);
    }

    public double getDistanceToGoalInches() { return distanceToGoalInches; }
    public double getDeltaXToGoal() { return deltaXToGoal; }
    public double getDeltaYToGoal() { return deltaYToGoal; }

    public double getCameraDistanceInches() { return cameraDistanceInches; }
    public void setCameraDistanceInches(double inches) { cameraDistanceInches = inches; }

    /** Distance used for hood/flywheel regression (velocity comp when enabled). */
    public double getShotDistanceInches() {
        if (turretVelocityCompensation && Double.isFinite(distanceForHoodSpeedInches)) {
            return distanceForHoodSpeedInches;
        }
        return distanceToGoalInches;
    }

    public Vector2d getGoalPosition() {
        return new Vector2d(goalX, goalY);
    }
}
