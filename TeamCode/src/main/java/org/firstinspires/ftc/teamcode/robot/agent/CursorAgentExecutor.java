package org.firstinspires.ftc.teamcode.robot.agent;

import com.bylazar.cursoragent.AgentSnapshot;
import com.bylazar.cursoragent.CommandResult;
import com.bylazar.cursoragent.CursorAgentRuntime;
import com.bylazar.cursoragent.DriveCommand;
import com.bylazar.cursoragent.MotorCommand;
import com.bylazar.cursoragent.PathCommand;
import com.bylazar.cursoragent.PathWaypoint;
import com.bylazar.cursoragent.ServoCommand;
import com.bylazar.cursoragent.VisionTagSnapshot;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.RobotState;
import org.firstinspires.ftc.teamcode.robot.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;
import org.firstinspires.ftc.teamcode.robot.vision.AprilTagVision;
import org.firstinspires.ftc.teamcode.util.Pose2d;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Executes Cursor MCP commands against {@link RobotContainer} hardware.
 */
public final class CursorAgentExecutor implements CursorAgentRuntime.AgentExecutor {
    private static final double DRIVE_SCALE = 0.75;

    private final RobotContainer robot;
    private final HardwareMap hardwareMap;
    private final boolean opModeActive;
    private PathChain activePath;

    public CursorAgentExecutor(RobotContainer robot, HardwareMap hardwareMap, boolean opModeActive) {
        this.robot = robot;
        this.hardwareMap = hardwareMap;
        this.opModeActive = opModeActive;
    }

    @Override
    public CommandResult executeDrive(DriveCommand command) {
        if (!opModeActive) return new CommandResult(false, "OpMode not running", "");
        DriveSubsystem drive = robot.drive;
        if (drive == null) return new CommandResult(false, "Drive not initialized", "");

        double x = clamp(command.getX()) * DRIVE_SCALE;
        double y = clamp(command.getY()) * DRIVE_SCALE;
        double rotate = clamp(command.getRotate()) * DRIVE_SCALE;
        drive.setTeleOpDrive(y, -x, -rotate, command.getFieldCentric());
        drive.update();
        return new CommandResult(true, "drive applied", "");
    }

    @Override
    public CommandResult executeMotor(MotorCommand command) {
        if (!opModeActive) return new CommandResult(false, "OpMode not running", "");
        String name = resolveMotorName(command.getName());
        if (name == null) return new CommandResult(false, "unknown motor: " + command.getName(), "");

        try {
            DcMotorEx motor = hardwareMap.get(DcMotorEx.class, name);
            String mode = command.getMode() == null ? "power" : command.getMode().toLowerCase(Locale.US);
            double value = command.getPower();
            if ("velocity".equals(mode)) {
                motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                motor.setVelocity(value);
            } else {
                motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motor.setPower(clamp(value));
            }
            return new CommandResult(true, "motor " + name + " set", "");
        } catch (Exception e) {
            return new CommandResult(false, "motor error: " + e.getMessage(), "");
        }
    }

    @Override
    public CommandResult executeServo(ServoCommand command) {
        if (!opModeActive) return new CommandResult(false, "OpMode not running", "");
        String name = resolveServoName(command.getName());
        if (name == null) return new CommandResult(false, "unknown servo: " + command.getName(), "");

        try {
            Servo servo = hardwareMap.get(Servo.class, name);
            servo.setPosition(clamp(command.getPosition()));
            return new CommandResult(true, "servo " + name + " set", "");
        } catch (Exception e) {
            return new CommandResult(false, "servo error: " + e.getMessage(), "");
        }
    }

    @Override
    public CommandResult executePath(PathCommand command) {
        if (!opModeActive) return new CommandResult(false, "OpMode not running", "");
        DriveSubsystem drive = robot.drive;
        if (drive == null) return new CommandResult(false, "Drive not initialized", "");
        if (command.getWaypoints() == null || command.getWaypoints().size() < 2) {
            return new CommandResult(false, "path needs at least 2 waypoints", "");
        }

        List<PathWaypoint> points = command.getWaypoints();
        Pose2d[] poses = new Pose2d[points.size()];
        for (int i = 0; i < points.size(); i++) {
            PathWaypoint wp = points.get(i);
            double headingRad = Double.isNaN(wp.getHeadingDeg())
                    ? drive.getPose().heading
                    : Math.toRadians(wp.getHeadingDeg());
            poses[i] = new Pose2d(wp.getX(), wp.getY(), headingRad);
        }

        activePath = drive.buildPathChain(poses);
        drive.followPath(activePath, command.getHoldEnd());
        return new CommandResult(true, "path started", "");
    }

    @Override
    public CommandResult executeEstop() {
        DriveSubsystem drive = robot.drive;
        if (drive != null) {
            drive.breakFollowing();
            drive.setTeleOpDrive(0, 0, 0, false);
            drive.update();
        }
        zeroMotor(DeviceNames.INTAKE);
        zeroMotor(DeviceNames.TRANSFER);
        zeroMotor(DeviceNames.LAUNCH);
        zeroMotor(DeviceNames.FL);
        zeroMotor(DeviceNames.BL);
        zeroMotor(DeviceNames.FR);
        zeroMotor(DeviceNames.BR);
        activePath = null;
        return new CommandResult(true, "estop executed", "");
    }

    @Override
    public AgentSnapshot captureSnapshot() {
        DriveSubsystem drive = robot.drive;
        Pose2d pose = drive != null ? drive.getPose() : new Pose2d(0, 0, 0);
        double[] vel = drive != null ? drive.getVelocity() : new double[]{0, 0, 0};
        boolean pathBusy = drive != null && drive.isBusy();

        Map<String, String> telemetry = new HashMap<>();
        telemetry.put("alliance", robot.state.isBlueAlliance() ? "blue" : "red");
        telemetry.put("distanceToGoal", String.format(Locale.US, "%.2f", robot.state.getDistanceToGoalInches()));
        if (robot.flywheel != null) {
            telemetry.put("flywheelVel", String.format(Locale.US, "%.1f", robot.flywheel.getVelocity()));
            telemetry.put("flywheelTarget", String.format(Locale.US, "%.1f", robot.flywheel.getTargetTicksPerSec()));
        }

        List<VisionTagSnapshot> tags = collectVisionTags();

        return new AgentSnapshot(
                true,
                CursorAgentRuntime.estop.get(),
                opModeActive,
                pose.position.x,
                pose.position.y,
                Math.toDegrees(pose.heading),
                vel.length > 0 ? vel[0] : 0,
                vel.length > 1 ? vel[1] : 0,
                "",
                "",
                pathBusy,
                tags,
                telemetry
        );
    }

    private List<VisionTagSnapshot> collectVisionTags() {
        List<VisionTagSnapshot> tags = new ArrayList<>();
        AprilTagVision vision = robot.vision;
        if (vision == null) return tags;

        List<AprilTagDetection> detections = vision.processor.getDetections();
        for (AprilTagDetection det : detections) {
            if (det.metadata == null) continue;
            tags.add(new VisionTagSnapshot(
                    det.id,
                    det.ftcPose.x,
                    det.ftcPose.y,
                    det.ftcPose.z,
                    det.ftcPose.range,
                    det.ftcPose.bearing
            ));
        }
        return tags;
    }

    private void zeroMotor(String name) {
        try {
            DcMotor motor = hardwareMap.get(DcMotor.class, name);
            motor.setPower(0);
        } catch (Exception ignored) {
        }
    }

    private static String resolveMotorName(String logical) {
        if (logical == null) return null;
        switch (logical.toLowerCase(Locale.US)) {
            case "fl": return DeviceNames.FL;
            case "bl": return DeviceNames.BL;
            case "fr": return DeviceNames.FR;
            case "br": return DeviceNames.BR;
            case "intake": return DeviceNames.INTAKE;
            case "launch":
            case "flywheel": return DeviceNames.LAUNCH;
            case "trans":
            case "transfer": return DeviceNames.TRANSFER;
            default: return logical;
        }
    }

    private static String resolveServoName(String logical) {
        if (logical == null) return null;
        switch (logical.toLowerCase(Locale.US)) {
            case "hood": return DeviceNames.HOOD;
            case "turret": return DeviceNames.TURRET;
            default: return logical;
        }
    }

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }
}
