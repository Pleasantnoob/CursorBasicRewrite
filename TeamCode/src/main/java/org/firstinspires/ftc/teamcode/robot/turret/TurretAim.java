package org.firstinspires.ftc.teamcode.robot.turret;

import org.firstinspires.ftc.teamcode.robot.field.ShotPhysics;
import org.firstinspires.ftc.teamcode.robot.shooter.FlywheelSubsystem;
import org.firstinspires.ftc.teamcode.util.Pose2d;

/**
 * Field-aim helper with optional velocity compensation.
 */
public final class TurretAim {
    private TurretAim() {}

    public static final class AimSolution {
        public final double aimGoalX;
        public final double aimGoalY;
        public final double distanceForHoodSpeedInches;
        public final double timeOfFlightSeconds;
        public final double angleToGoalDeg;

        public AimSolution(double aimGoalX,
                           double aimGoalY,
                           double distanceForHoodSpeedInches,
                           double timeOfFlightSeconds,
                           double angleToGoalDeg) {
            this.aimGoalX = aimGoalX;
            this.aimGoalY = aimGoalY;
            this.distanceForHoodSpeedInches = distanceForHoodSpeedInches;
            this.timeOfFlightSeconds = timeOfFlightSeconds;
            this.angleToGoalDeg = angleToGoalDeg;
        }
    }

    /** Robot-frame (vx, vy) to world-frame [worldVx, worldVy] given heading (rad). */
    public static double[] robotVelToWorld(double vx, double vy, double headingRad) {
        double c = Math.cos(headingRad);
        double s = Math.sin(headingRad);
        return new double[]{c * vx - s * vy, s * vx + c * vy};
    }

    /**
     * Solves velocity-compensated turret aim and hood/flywheel distance.
     */
    public static AimSolution solveAim(Pose2d pose,
                                       double goalX,
                                       double goalY,
                                       double turretAngleOffsetDeg,
                                       double robotVxInPerSec,
                                       double robotVyInPerSec,
                                       boolean velocityCompensationEnabled,
                                       double turretVelocityCompGain,
                                       double hoodSpeedVelocityCompGain,
                                       boolean manualMode,
                                       double manualHoodAngleDeg,
                                       double manualTargetTicksPerSec) {
        if (pose == null || pose.position == null) {
            return new AimSolution(goalX, goalY, 0, 0, 0);
        }
        double[] worldVel = robotVelToWorld(robotVxInPerSec, robotVyInPerSec, pose.heading);
        double worldVx = worldVel[0];
        double worldVy = worldVel[1];

        double distRaw = Math.hypot(pose.position.x - goalX, pose.position.y - goalY);
        double[] hoodSpeed = ShotPhysics.hoodAndSpeedFromDistanceInches(distRaw);
        double hoodDegForVelComp = manualMode ? manualHoodAngleDeg : hoodSpeed[0];
        double speedMpsForVelComp = manualMode
                ? FlywheelSubsystem.launchSpeedMpsFromTicksPerSec(manualTargetTicksPerSec)
                : hoodSpeed[1];
        double timeOfFlightS = ShotPhysics.timeInAir(speedMpsForVelComp, Math.toRadians(hoodDegForVelComp));

        double aimGoalX = goalX;
        double aimGoalY = goalY;
        double distForShotVel = distRaw;
        if (velocityCompensationEnabled) {
            if (turretVelocityCompGain != 0) {
                double scaleTurret = turretVelocityCompGain * timeOfFlightS;
                aimGoalX = goalX - worldVx * scaleTurret;
                aimGoalY = goalY - worldVy * scaleTurret;
            }
            if (hoodSpeedVelocityCompGain != 0) {
                double scaleHood = hoodSpeedVelocityCompGain * timeOfFlightS;
                double vgX = goalX - worldVx * scaleHood;
                double vgY = goalY - worldVy * scaleHood;
                distForShotVel = Math.hypot(pose.position.x - vgX, pose.position.y - vgY);
            }
        }
        double angleToGoalRad = Math.atan2(aimGoalY - pose.position.y, aimGoalX - pose.position.x);
        double angleToGoalDeg = TurretSubsystem.wrapDeg360(Math.toDegrees(angleToGoalRad) + turretAngleOffsetDeg);
        return new AimSolution(aimGoalX, aimGoalY, distForShotVel, timeOfFlightS, angleToGoalDeg);
    }
}
