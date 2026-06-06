package org.firstinspires.ftc.teamcode.robot.shooter;

import org.firstinspires.ftc.teamcode.robot.field.ShotPhysics;
import org.firstinspires.ftc.teamcode.robot.RobotState;

/**
 * Distance-to-hood/flywheel regression and AirSort preset math.
 * Reads shot distance from {@link RobotState}, not from OpMode statics.
 */
public class ShotCalculator {
    private static final double HOOD_A = -0.0000200129;
    private static final double HOOD_B = 0.00741561;
    private static final double HOOD_C = -0.953361;
    private static final double HOOD_D = 83.35366;
    private static final double FLYWHEEL_SLOPE = -4.86091;
    private static final double FLYWHEEL_INTERCEPT = -818.80229;
    private static final double MIN_DIST_IN = 12.0;
    private static final double MAX_DIST_IN = 180.0;
    private static final double AIRSORT_HOOD_BAND_DEG = 12.0;

    public static final double MAX_FLYWHEEL_TICKS_PER_SEC = 1700.0;

    public static class ShotTargets {
        public final double hoodAngleDeg;
        public final double flywheelTicksPerSec;

        public ShotTargets(double hoodAngleDeg, double flywheelTicksPerSec) {
            this.hoodAngleDeg = hoodAngleDeg;
            this.flywheelTicksPerSec = flywheelTicksPerSec;
        }
    }

    /** Regression from distance (inches) for competition shots. */
    public ShotTargets fromDistanceInches(double distanceInches) {
        return fromDistanceInches(distanceInches, 0);
    }

    public ShotTargets fromDistanceInches(double distanceInches, double flywheelBoostTicksPerSec) {
        double x = clampDistance(distanceInches);
        double hoodDeg = HOOD_A * x * x * x + HOOD_B * x * x + HOOD_C * x + HOOD_D;
        hoodDeg = clampHood(hoodDeg);
        double flywheel = FLYWHEEL_SLOPE * x + FLYWHEEL_INTERCEPT + flywheelBoostTicksPerSec;
        return new ShotTargets(hoodDeg, flywheel);
    }

    public ShotTargets fromRobotState(RobotState state) {
        return fromDistanceInches(state.getShotDistanceInches());
    }

    public ShotTargets airSortPreset(org.firstinspires.ftc.teamcode.robot.intake.AirSort.ShotMode mode, double distanceInches) {
        distanceInches = clampDistance(distanceInches);
        ShotTargets regression = fromDistanceInches(distanceInches);
        double vMax = FlywheelSubsystem.launchSpeedMpsFromTicksPerSec(MAX_FLYWHEEL_TICKS_PER_SEC);
        double[] hoodAndSpeed = mode == org.firstinspires.ftc.teamcode.robot.intake.AirSort.ShotMode.FAST_SHOT
                ? ShotPhysics.fastShotHoodAndSpeedMPS(distanceInches, vMax)
                : ShotPhysics.slowShotHoodAndSpeedMPS(distanceInches, vMax);
        double hoodDeg = hoodAndSpeed[0];
        double speedMps = hoodAndSpeed[1];
        double ticks = -FlywheelSubsystem.ticksPerSecFromLaunchSpeedMps(speedMps);
        if (ticks > regression.flywheelTicksPerSec) ticks = regression.flywheelTicksPerSec;
        double hoodMin = Math.max(HoodSubsystem.minAngleDeg, regression.hoodAngleDeg - AIRSORT_HOOD_BAND_DEG);
        double hoodMax = Math.min(HoodSubsystem.maxAngleDeg, regression.hoodAngleDeg + AIRSORT_HOOD_BAND_DEG);
        hoodDeg = Math.max(hoodMin, Math.min(hoodMax, hoodDeg));
        double dM = Math.max(0.3, Math.min(3.5, distanceInches * 0.0254));
        double required = ShotPhysics.speedForShot(dM, Math.toRadians(hoodDeg));
        if (required > speedMps) speedMps = required;
        speedMps = Math.min(speedMps, vMax);
        ticks = -FlywheelSubsystem.ticksPerSecFromLaunchSpeedMps(speedMps);
        if (ticks > regression.flywheelTicksPerSec) ticks = regression.flywheelTicksPerSec;
        return new ShotTargets(hoodDeg, ticks);
    }

    private static double clampDistance(double d) {
        return Math.max(MIN_DIST_IN, Math.min(MAX_DIST_IN, d));
    }

    private static double clampHood(double deg) {
        return Math.max(HoodSubsystem.minAngleDeg, Math.min(HoodSubsystem.maxAngleDeg, deg));
    }
}
