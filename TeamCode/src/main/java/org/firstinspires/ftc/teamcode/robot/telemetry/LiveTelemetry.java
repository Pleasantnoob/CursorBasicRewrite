package org.firstinspires.ftc.teamcode.robot.telemetry;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.RR.Drawing;
import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.field.ShootingZones;
import org.firstinspires.ftc.teamcode.robot.field.ShotPhysics;
import org.firstinspires.ftc.teamcode.robot.intake.AirSort;
import org.firstinspires.ftc.teamcode.robot.shooter.FlywheelSubsystem;
import org.firstinspires.ftc.teamcode.robot.shooter.ShooterTeleop;
import org.firstinspires.ftc.teamcode.robot.turret.TurretAim;
import org.firstinspires.ftc.teamcode.robot.turret.TurretSubsystem;
import org.firstinspires.ftc.teamcode.util.Pose2d;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.LinkedList;

/**
 * Live FTC Dashboard field overlay + telemetry packets, with optional Panels (bylazar) forwarding.
 *
 * <p>When {@code usePanelsDashboard} is false (default): mirrors Driver Station telemetry to
 * FTC Dashboard and sends {@link TelemetryPacket} with field drawing each loop.
 *
 * <p>When true: telemetry also goes to <a href="https://panels.bylazar.com">Panels</a> via reflection;
 * FTC Dashboard field overlay is skipped (use Panels Field widget). Requires {@code com.bylazar:fullpanels}.
 */
public class LiveTelemetry {
    private static final int POSE_HISTORY_MAX = 200;

    private final LinkedList<Pose2d> poseHistory = new LinkedList<>();
    private Object panelsTelemetry;
    private boolean usePanelsDashboard;
    private boolean attached;

    /** Call once from OpMode init after {@link RobotContainer#init}. */
    public void attach(LinearOpMode opMode, boolean usePanels) {
        if (attached || opMode == null) return;
        usePanelsDashboard = usePanels;
        if (usePanelsDashboard) {
            panelsTelemetry = getPanelsTelemetry();
        } else {
            opMode.telemetry = new MultipleTelemetry(opMode.telemetry, FtcDashboard.getInstance().getTelemetry());
        }
        attached = true;
    }

    public boolean isAttached() {
        return attached;
    }

    /** Forward Driver Station telemetry to Panels after {@code telemetry.update()}. */
    public void forwardPanelsOnly(LinearOpMode opMode) {
        if (attached && usePanelsDashboard && panelsTelemetry != null && opMode != null) {
            updatePanelsTelemetry(panelsTelemetry, opMode.telemetry);
        }
    }

    /** Record pose trail and publish Dashboard packet / Panels update. */
    public void publish(LinearOpMode opMode, RobotContainer robot, Frame frame) {
        if (!attached || opMode == null || robot == null || frame == null || frame.pose == null) return;

        poseHistory.add(frame.pose);
        while (poseHistory.size() > POSE_HISTORY_MAX) {
            poseHistory.removeFirst();
        }

        if (!usePanelsDashboard) {
            sendFtcDashboardPacket(robot, frame);
        }

        if (usePanelsDashboard && panelsTelemetry != null) {
            updatePanelsTelemetry(panelsTelemetry, opMode.telemetry);
        }
    }

    private void sendFtcDashboardPacket(RobotContainer robot, Frame frame) {
        TelemetryPacket packet = new TelemetryPacket();
        Pose2d pose = frame.pose;
        Pose2d displayPose = frame.cameraPose != null ? frame.cameraPose : pose;
        double goalX = robot.state.getGoalX();
        double goalY = robot.state.getGoalY();

        Drawing.drawPoseHistory(packet.fieldOverlay(), poseHistory, "#3F51B5");
        Drawing.drawRobot(packet.fieldOverlay(), displayPose, 1,
                frame.cameraPose != null ? "#FF5722" : "#3F51B5");
        if (frame.cameraPose != null) {
            Drawing.drawRobot(packet.fieldOverlay(), pose, 1, "#9FA8DA");
        }
        Drawing.drawGoal(packet.fieldOverlay(), goalX, goalY, "#4CAF50");
        Drawing.drawCloseLaunchTriangle(packet.fieldOverlay(),
                ShootingZones.closeTriX1, ShootingZones.closeTriY1,
                ShootingZones.closeTriX2, ShootingZones.closeTriY2,
                ShootingZones.closeTriX3, ShootingZones.closeTriY3, "#80E27E");
        Drawing.drawFarZoneTriangle(packet.fieldOverlay(),
                ShootingZones.farTriX1, ShootingZones.farTriY1,
                ShootingZones.farTriX2, ShootingZones.farTriY2,
                ShootingZones.farTriX3, ShootingZones.farTriY3, "#4FC3F7");

        TurretAim.AimSolution aim = frame.aim;
        if (aim != null) {
            if (frame.velocityCompOn) {
                Drawing.drawVirtualGoal(packet.fieldOverlay(), aim.aimGoalX, aim.aimGoalY, "#FF9800");
            }
            Drawing.drawRobotToGoalLine(packet.fieldOverlay(), displayPose, aim.aimGoalX, aim.aimGoalY, "#FFC107");
        }

        ShootingZones.Zone zone = ShootingZones.getShootingZone(
                pose.position.x, pose.position.y, goalX, goalY);
        packet.put("shooting_zone", zone.name().toLowerCase());
        packet.put("in_field", ShootingZones.isInsideField(pose.position.x, pose.position.y));
        packet.put("artifact_count", frame.artifactCount);
        packet.put("ready_to_shoot", frame.readyToShoot);

        double distToGoalIn = robot.state.getDistanceToGoalInches();
        double distForShot = robot.state.getShotDistanceInches();
        double hoodDeg = ShooterTeleop.manualMode
                ? ShooterTeleop.manualHoodAngleDeg
                : ShotPhysics.hoodAndSpeedFromDistanceInches(distForShot)[0];
        double[] shot = ShotPhysics.speedAndTimeInAir(distForShot, hoodDeg);

        double camDist = robot.state.getCameraDistanceInches();
        boolean camValid = Double.isFinite(camDist) && camDist >= 12 && camDist <= 200;

        packet.put("dist_to_goal_in", distToGoalIn);
        packet.put("dist_for_shot_in", distForShot);
        packet.put("dist_src", camValid ? "camera" : "odom");
        packet.put("speed_needed_mps", shot[0]);
        packet.put("time_in_air_s", shot[1]);
        packet.put("odom_x", pose.position.x);
        packet.put("odom_y", pose.position.y);
        packet.put("est_x", displayPose.position.x);
        packet.put("est_y", displayPose.position.y);
        packet.put("odom_heading_deg", Math.toDegrees(pose.heading));
        packet.put("pose_src", frame.cameraPose != null ? "camera (reloc)" : "Pinpoint (pods+IMU)");
        if (aim != null) {
            packet.put("angle_to_goal_deg", aim.angleToGoalDeg);
            packet.put("aim_goal_x", aim.aimGoalX);
            packet.put("aim_goal_y", aim.aimGoalY);
            packet.put("time_of_flight_s", aim.timeOfFlightSeconds);
        }
        if (robot.turret != null) {
            packet.put("turret_field_deg", robot.turret.getTurretAngleField());
            packet.put("turret_robot_deg", robot.turret.getTurretAngleRobot());
            packet.put("field_hold_deg", frame.fieldHoldAngleDeg);
        }
        packet.put("vel_comp_on", frame.velocityCompOn);
        packet.put("position_hold", frame.positionHoldEnabled);
        packet.put("locked_on", frame.lockedOn);
        packet.put("launcher_mode", launcherModeLabel(robot));
        packet.put("airsort_on", ShooterTeleop.airSortEnabled);

        if (ShooterTeleop.airSortEnabled && robot.airSort != null) {
            AirSort airSort = robot.airSort;
            packet.put("airsort_shot", airSort.getShotIndex() + 1);
            packet.put("airsort_mode", airSort.getShotMode().name());
            packet.put("airsort_ball", airSort.getBallAtLauncher());
            packet.put("airsort_want", airSort.getDesiredColorForCurrentShot());
            packet.put("airsort_motif_stored", airSort.isMotifStored());
            packet.put("airsort_advance_delay_ms", airSort.getAdvanceDelayMs());
            if (robot.vision != null && robot.vision.processor != null) {
                StringBuilder tagIds = new StringBuilder();
                for (AprilTagDetection d : robot.vision.processor.getDetections()) {
                    if (d.metadata == null) continue;
                    if (tagIds.length() > 0) tagIds.append(",");
                    tagIds.append(d.id);
                }
                packet.put("airsort_detected_tag_ids", tagIds.length() > 0 ? tagIds.toString() : "none");
            }
        }

        packet.put("manual_hood_deg", ShooterTeleop.manualHoodAngleDeg);
        packet.put("manual_target_vel", ShooterTeleop.manualTargetTicksPerSec);
        packet.put("flywheel_p", FlywheelSubsystem.p);
        packet.put("flywheel_i", FlywheelSubsystem.i);
        packet.put("flywheel_d", FlywheelSubsystem.d);
        packet.put("flywheel_kS", FlywheelSubsystem.kS);
        packet.put("flywheel_kV", FlywheelSubsystem.kV);
        packet.put("flywheel_kA", FlywheelSubsystem.kA);
        packet.put("flywheel_accel", FlywheelSubsystem.accel);
        packet.put("world_vel_x_in_s", frame.worldVx);
        packet.put("world_vel_y_in_s", frame.worldVy);

        if (frame.cameraPose != null) {
            packet.put("camera_x", frame.cameraPose.position.x);
            packet.put("camera_y", frame.cameraPose.position.y);
        }
        if (robot.flywheel != null) {
            packet.put("flywheel_vel", robot.flywheel.getVelocity());
            packet.put("flywheel_target", robot.flywheel.getTargetTicksPerSec());
            packet.put("flywheel_at_target", robot.flywheel.isAtTarget());
        }

        FtcDashboard.getInstance().sendTelemetryPacket(packet);
    }

    private static String launcherModeLabel(RobotContainer robot) {
        if (ShooterTeleop.airSortEnabled && robot.shootSequence != null
                && robot.shootSequence.isAirSortPresetActive()) {
            return "airsort (fast/slow)";
        }
        if (ShooterTeleop.manualMode) return "manual (Dashboard)";
        return "auto (from distance)";
    }

    /** Per-loop snapshot for dashboard publishing. */
    public static class Frame {
        public Pose2d pose;
        public Pose2d cameraPose;
        public TurretAim.AimSolution aim;
        public double fieldHoldAngleDeg;
        public boolean lockedOn;
        public boolean velocityCompOn;
        public boolean positionHoldEnabled;
        public int artifactCount;
        public boolean readyToShoot;
        public double worldVx;
        public double worldVy;
    }

    private static Object getPanelsTelemetry() {
        try {
            Class<?> panelsClass = Class.forName("com.bylazar.ftcontrol.panels.Panels");
            return panelsClass.getMethod("getTelemetry").invoke(null);
        } catch (Throwable t) {
            try {
                Class<?> panelsClass = Class.forName("com.bylazar.panels.Panels");
                return panelsClass.getMethod("getTelemetry").invoke(null);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    private static void updatePanelsTelemetry(Object panelsTelemetry, Telemetry telemetry) {
        try {
            panelsTelemetry.getClass()
                    .getMethod("update", Telemetry.class)
                    .invoke(panelsTelemetry, telemetry);
        } catch (Throwable ignored) {
            // Panels not on classpath or API mismatch — DS telemetry still works
        }
    }
}
