package org.firstinspires.ftc.teamcode.robot.teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.field.CameraRelocalization;
import org.firstinspires.ftc.teamcode.robot.field.ShootingZones;
import org.firstinspires.ftc.teamcode.robot.shooter.ShooterTeleop;
import org.firstinspires.ftc.teamcode.robot.telemetry.LiveTelemetry;
import org.firstinspires.ftc.teamcode.robot.turret.TurretAim;
import org.firstinspires.ftc.teamcode.robot.turret.TurretSubsystem;
import org.firstinspires.ftc.teamcode.util.Pose2d;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

/**
 * Main teleop loop for {@link org.firstinspires.ftc.teamcode.opmodes.teleop.MainTeleop}.
 *
 * <p>Owns per-frame orchestration: odometry, aim, drive, shooter, LED, airsort, telemetry.
 * Live FTC Dashboard field overlay + optional Panels via {@link LiveTelemetry}.
 */
public class TeleopController {
    private static final double CAMERA_MIN_IN = 12.0;
    private static final double CAMERA_MAX_IN = 200.0;
    private static final double M_TO_IN = 39.37007874;

    private final LinearOpMode opMode;
    private final RobotContainer robot;
    private final LiveTelemetry liveTelemetry = new LiveTelemetry();

    // --- Turret lock-on state ---
    private boolean lockedOn;
    private boolean prevBack;
    private boolean prevStart;
    private boolean prevReadyToShoot;
    private boolean justResynced;
    private double manualTurretDeg;
    private double manualHeadingDeg;
    private double fieldHoldAngleDeg;

    // --- Cached for dashboard frame ---
    private TurretAim.AimSolution lastAim;
    private Pose2d cameraRelocPose;
    private boolean inCloseZone;
    private boolean inFarZone;

    public TeleopController(LinearOpMode opMode, RobotContainer robot) {
        this.opMode = opMode;
        this.robot = robot;
        if (robot.bindings != null && robot.bindings.operator != null && robot.turret != null) {
            fieldHoldAngleDeg = robot.turret.getTurretAngleField();
        }
    }

    /** Wire FTC Dashboard / Panels telemetry (call once after RobotContainer.init). */
    public void attachLiveTelemetry(boolean usePanelsDashboard) {
        liveTelemetry.attach(opMode, usePanelsDashboard);
    }

    /** One teleop tick. No-op if robot not initialized or operator gamepad missing. */
    public void update(Settings settings) {
        if (settings == null || !robot.isInitialized() || robot.bindings == null) return;
        if (robot.bindings.operator == null) return;

        robot.bindings.updateSlowToggles();
        robot.updateOdometry();
        Pose2d pose = robot.state.getPose();

        robot.state.setAllianceBlue(settings.allianceBlue);
        robot.state.updateFromPose(pose);
        robot.turret.setRobotHeadingDeg(Math.toDegrees(pose.heading));

        cameraRelocPose = null;
        updateVisionDistance(pose, settings.useCameraRelocalization);
        updateAimAndTurret(settings);
        updateShootingZoneAndLed(settings.closeZoneRumbleMs);
        updateDrive(settings);
        updateAirSort(settings.airsortAdvanceDelayMs);
        robot.shooterTeleop.updateTeleop(robot.bindings);
        telemetry(settings);
    }

    private void updateVisionDistance(Pose2d pose, boolean useCameraRelocalization) {
        if (robot.vision == null || robot.vision.processor == null) return;
        robot.state.setCameraDistanceInches(Double.NaN);
        for (AprilTagDetection d : robot.vision.processor.getDetections()) {
            if (d.metadata == null || d.ftcPose == null) continue;
            if (d.id == 20 || d.id == 24) {
                double rangeIn = d.ftcPose.range * M_TO_IN;
                if (rangeIn >= CAMERA_MIN_IN && rangeIn <= CAMERA_MAX_IN) {
                    robot.state.setCameraDistanceInches(rangeIn);
                }
            }
            if (useCameraRelocalization && d.id == CameraRelocalization.BLUE_GOAL_TAG_ID) {
                Pose2d cameraPose = CameraRelocalization.robotPoseFromTag(
                        d,
                        CameraRelocalization.BLUE_GOAL_TAG_X,
                        CameraRelocalization.BLUE_GOAL_TAG_Y,
                        pose.heading
                );
                if (cameraPose != null) {
                    cameraRelocPose = cameraPose;
                    robot.state.setPose(cameraPose);
                }
            }
        }
    }

    private void updateAimAndTurret(Settings settings) {
        Pose2d pose = robot.state.getPose();
        double[] vel = robot.drive.getVelocity();
        lastAim = TurretAim.solveAim(
                pose,
                robot.state.getGoalX(),
                robot.state.getGoalY(),
                settings.turretAngleOffsetDeg,
                vel[0],
                vel[1],
                settings.turretVelocityCompensation,
                settings.turretVelocityCompGain,
                settings.hoodSpeedVelocityCompGain,
                ShooterTeleop.manualMode,
                ShooterTeleop.manualHoodAngleDeg,
                ShooterTeleop.manualTargetTicksPerSec
        );
        robot.state.turretVelocityCompensation = settings.turretVelocityCompensation;
        robot.state.distanceForHoodSpeedInches = lastAim.distanceForHoodSpeedInches;

        if (!lockedOn) {
            if (!justResynced) {
                if (robot.bindings.operator.dpad_up) fieldHoldAngleDeg = 0;
                else if (robot.bindings.operator.dpad_down) fieldHoldAngleDeg = 180;
                else if (Math.abs(robot.bindings.operator.left_stick_x) <= 0.1) {
                    fieldHoldAngleDeg = TurretSubsystem.wrapDeg360(lastAim.angleToGoalDeg + robot.turret.aimOffsetDeg);
                }
                robot.turret.targetAngle = fieldHoldAngleDeg;
            }
            robot.turret.applyOperatorNudge(robot.bindings.operator);
            robot.turret.runTurretGyro();
            fieldHoldAngleDeg = robot.turret.targetAngle;
        } else {
            updateManualTurretMode();
            robot.turret.runTurretNoGyro(manualHeadingDeg);
        }

        if (robot.bindings.operator.xWasPressed() || (robot.bindings.operator.start && !prevStart)) {
            robot.turret.resyncEncoder();
            justResynced = true;
            if (lockedOn) {
                manualTurretDeg = 0;
                robot.turret.targetAngle = manualHeadingDeg;
            } else {
                robot.turret.targetAngle = TurretSubsystem.wrapDeg360(lastAim.angleToGoalDeg + robot.turret.aimOffsetDeg);
                fieldHoldAngleDeg = robot.turret.targetAngle;
            }
            robot.bindings.operator.rumble(300);
        } else {
            justResynced = false;
        }
        prevStart = robot.bindings.operator.start;
        updateLockOn();
    }

    private void updateManualTurretMode() {
        if (robot.bindings.operator.aWasPressed()) manualTurretDeg = 0;
        manualTurretDeg += robot.bindings.operator.left_stick_x * 4.0;
        manualTurretDeg = Math.max(-TurretSubsystem.manualModeLimitDeg, Math.min(TurretSubsystem.manualModeLimitDeg, manualTurretDeg));
        robot.turret.setAngle(manualHeadingDeg + manualTurretDeg);
    }

    private void updateLockOn() {
        if (robot.bindings.operator.yWasPressed() && !lockedOn) {
            lockedOn = true;
            manualHeadingDeg = robot.turret.botHeading;
            manualTurretDeg = robot.turret.getTurretAngleRobot();
        } else if (robot.bindings.operator.bWasPressed() && lockedOn) {
            lockedOn = false;
        }
    }

    private void updateShootingZoneAndLed(int closeZoneRumbleMs) {
        Pose2d pose = robot.state.getPose();
        inCloseZone = ShootingZones.circleIntersectsCloseLaunchTriangle(
                pose.position.x, pose.position.y, ShootingZones.robotZoneCircleRadius);
        inFarZone = ShootingZones.circleIntersectsFarZoneTriangle(
                pose.position.x, pose.position.y, ShootingZones.robotZoneCircleRadius);
        boolean readyToShoot = (inCloseZone || inFarZone) && robot.balls.hasAtLeastTwoBalls();
        if (readyToShoot && !prevReadyToShoot) {
            robot.bindings.operator.rumble(closeZoneRumbleMs);
        }
        prevReadyToShoot = readyToShoot;
        if (robot.led != null) {
            robot.led.updateTeleop(inCloseZone || inFarZone, robot.balls);
        }
    }

    private void updateDrive(Settings settings) {
        if (robot.bindings.driver == null) return;
        double x = robot.bindings.driver.left_stick_x * robot.bindings.strafeScale();
        double y = -robot.bindings.driver.left_stick_y * robot.bindings.strafeScale();
        double turn = robot.bindings.driver.right_stick_x * robot.bindings.rotateScale();
        double db = settings.positionHoldDeadband;
        boolean idle = settings.positionHoldEnabled
                && Math.abs(x) < db && Math.abs(y) < db && Math.abs(turn) < db;
        if (idle) {
            robot.drive.setTeleOpDrive(0, 0, 0, false);
        } else {
            double scale = settings.driveScale > 0 ? settings.driveScale : 0.75;
            robot.drive.setTeleOpDrive(y * scale, -x * scale, -turn * scale, false);
        }
    }

    private void updateAirSort(double airsortAdvanceDelayMs) {
        if (robot.airSort == null || robot.vision == null) return;

        if (robot.bindings.operator.back && !prevBack) {
            ShooterTeleop.airSortEnabled = !ShooterTeleop.airSortEnabled;
            if (ShooterTeleop.airSortEnabled) {
                robot.airSort.setShotIndex(0);
                robot.vision.setDecimation(2);
            } else {
                robot.shootSequence.clearAirSortPreset();
                robot.vision.setDecimation(3);
            }
        }
        prevBack = robot.bindings.operator.back;
        if (!ShooterTeleop.airSortEnabled) return;

        robot.airSort.setAdvanceDelayMs((long) airsortAdvanceDelayMs);
        robot.airSort.update();
        robot.shootSequence.setAirSortPreset(robot.airSort.getShotMode(), robot.state.getDistanceToGoalInches());
    }

    private int countArtifacts() {
        int n = 0;
        if (robot.balls.hasBallAtSlot(robot.balls.slot1)) n++;
        if (robot.balls.hasBallAtSlot(robot.balls.slot2)) n++;
        if (robot.balls.hasBallAtSlot(robot.balls.slot3)) n++;
        return n;
    }

    private void telemetry(Settings settings) {
        Pose2d pose = robot.state.getPose();
        double[] vel = robot.drive.getVelocity();
        double[] worldVel = TurretAim.robotVelToWorld(vel[0], vel[1], pose.heading);

        opMode.telemetry.addData("Alliance", settings.allianceBlue ? "BLUE" : "RED");
        opMode.telemetry.addData("Pose", pose);
        opMode.telemetry.addData("Goal", "%.1f, %.1f", robot.state.getGoalX(), robot.state.getGoalY());
        opMode.telemetry.addData("Distance", "%.1f in", robot.state.getDistanceToGoalInches());
        opMode.telemetry.addData("Shoot zone", inCloseZone ? "close" : (inFarZone ? "far" : "none"));
        opMode.telemetry.addData("Turret field/robot", "%.1f / %.1f",
                robot.turret.getTurretAngleField(), robot.turret.getTurretAngleRobot());
        opMode.telemetry.addData("Flywheel", "%.0f / %.0f (at speed: %s)",
                robot.flywheel.getVelocity(), robot.flywheel.getTargetTicksPerSec(),
                robot.flywheel.isAtTarget());
        opMode.telemetry.addData("Launcher mode",
                ShooterTeleop.airSortEnabled ? "airsort" : (ShooterTeleop.manualMode ? "manual" : "auto"));
        opMode.telemetry.addData("Airsort", ShooterTeleop.airSortEnabled);
        if (robot.bindings.driver != null) {
            opMode.telemetry.addData("Slow strafe/rotate", "%s / %s",
                    robot.bindings.slowStrafe, robot.bindings.slowRotate);
        }
        opMode.telemetry.update();

        LiveTelemetry.Frame frame = new LiveTelemetry.Frame();
        frame.pose = pose;
        frame.cameraPose = cameraRelocPose;
        frame.aim = lastAim;
        frame.fieldHoldAngleDeg = fieldHoldAngleDeg;
        frame.lockedOn = lockedOn;
        frame.velocityCompOn = settings.turretVelocityCompensation && settings.turretVelocityCompGain != 0;
        frame.positionHoldEnabled = settings.positionHoldEnabled;
        frame.artifactCount = countArtifacts();
        frame.readyToShoot = (inCloseZone || inFarZone) && frame.artifactCount >= 2;
        frame.worldVx = worldVel[0];
        frame.worldVy = worldVel[1];
        liveTelemetry.publish(opMode, robot, frame);
    }

    /** Runtime tuning copied from MainTeleop @Config each loop. */
    public static class Settings {
        public boolean allianceBlue;
        public boolean turretVelocityCompensation;
        public boolean useCameraRelocalization;
        public double turretVelocityCompGain;
        public double hoodSpeedVelocityCompGain;
        public double turretAngleOffsetDeg;
        public double driveScale;
        public boolean positionHoldEnabled;
        public double positionHoldDeadband;
        public int closeZoneRumbleMs;
        public double airsortAdvanceDelayMs;
    }
}
