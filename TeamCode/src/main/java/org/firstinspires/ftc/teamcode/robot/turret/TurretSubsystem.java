package org.firstinspires.ftc.teamcode.robot.turret;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.controller.PIDController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.SimpleMotorFeedforward;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Turret subsystem: rotates the launcher turret and tracks its angle using motor encoder odometry.
 *
 * Odometry: The turret motor has an encoder. We convert encoder ticks to turret angle (degrees) using:
 *   - TICKS_PER_MOTOR_REV: encoder ticks per one full rotation of the motor shaft (384.5 for the motor used).
 *   - GEAR_RATIO: output rotation / motor rotation (2/5 means turret rotates 2/5 of a full turn per motor revolution).
 *
 * Target is the turret's field angle (set to angle to goal = line robot→goal). When robot-relative angle exceeds ±wrapLimitDeg,
 * the encoder is reset via setCurrentPosition to the wrapped equivalent so the turret stays in [-180, 180] and wires don't tangle.
 * At init: encoder 0 → turret output 0 → turret field angle = robot heading (turret same as robot). So robot at 90° → turret at 90° field, 0° relative to robot.
 * When on target, curAng (turret field angle) should match tgtAng (angle to goal).
 */
@Config
public class TurretSubsystem {
    /** Keeps compatibility with old `robot.turret.turret` access pattern. */
    public final TurretSubsystem turret = this;

    /** When false, turret motor gets 0 power (target angle still computed and logged). true = turret moves to hold field angle. */
    public static boolean turretDriveEnabled = true;

    // ========== Tuning constants ==========
    public static double p1 = 0.0037, i1 = 0.0001, d1 = 0.00058;
    public static double kS = 0, kV = 0, kA = 0;

    /** Encoder ticks per one full motor revolution. Used for encoder ↔ angle conversion. */
    public static final double TICKS_PER_MOTOR_REV = 384.5;
    /** Turret output rotation per motor rotation (e.g. 2/5 = 0.4). Inverse is used for angle→ticks. */
    public static final double GEAR_RATIO = 2.0 / 5.0;
    private static final double INV_GEAR_RATIO = 5.0 / 2.0;

    private final DcMotorEx turretMotor;
    private final PIDController controller;
    /** Desired turret angle in field frame (degrees). */
    public double targetAngle = 0;
    /** Current encoder position (ticks); updated each loop for telemetry/debug. */
    public double motorPosition;
    /** Last commanded motor power (for debugging). */
    public double power;
    /** Target position in encoder ticks (computed from targetAngle + heading). */
    public double targetPos;
    /** Robot heading from odometry (degrees). */
    public double botHeading;
    /** Current turret angle in field frame (degrees). */
    double currentAngle;
    /** Turret output angle (degrees, robot-relative). For logging. */
    public double outputDeg;
    /** Target turret output angle (degrees). For logging. */
    public double targetOutputDeg;
    public double ticksPerSecond = 1500;

    public static class Params {
        public double parYTicks = 0.0;
        public double perpXTicks = 0.0;
    }

    public static Params PARAMS = new Params();

    public GoBildaPinpointDriver driver;
    public GoBildaPinpointDriver.EncoderDirection initialParDirection, initialPerpDirection;

    /** Wrap angle to [0, 360) degrees. */
    public static double wrapDeg360(double a) {
        while (a >= 360) a -= 360;
        while (a < 0) a += 360;
        return a;
    }

    /** Wrap angle to [-180, 180) degrees (robot-centric). */
    public static double wrapDeg180(double a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    /** Ticks per degree of turret output rotation. */
    private static final double TICKS_PER_DEG = (TICKS_PER_MOTOR_REV / 360.0) * INV_GEAR_RATIO;

    /** Robot-relative angle limits (deg). */
    public static double wrapLimitDeg = 180.0;
    /** Max turret angle relative to robot (deg). */
    public static double robotRelativeLimitDeg = 140.0;
    /** Limit (deg) when in manual turret mode. */
    public static double manualModeLimitDeg = 175.0;
    /** If manual mode stick is reversed, set true. */
    public static boolean manualModeInvert = true;

    /** Nudge sensitivity: degrees added per loop per unit of left_stick_x. */
    public static double nudgeDegPerUnit = 5.0;
    /** Persistent aim offset (degrees). */
    public double aimOffsetDeg = 0;
    /** Max magnitude of aimOffsetDeg (degrees). */
    public static double aimOffsetMaxDeg = 170.0;

    public TurretSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, false);
    }

    /**
     * @param preserveEncoder When true (e.g. coming from auto), do not reset encoder so turret keeps its physical position.
     */
    public TurretSubsystem(HardwareMap hardwareMap, boolean preserveEncoder) {
        turretMotor = hardwareMap.get(DcMotorEx.class, "turret");
        turretMotor.setDirection(DcMotorSimple.Direction.REVERSE);
        if (!preserveEncoder) {
            turretMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        }
        turretMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        turretMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        controller = new PIDController(p1, i1, d1);
        controller.setPID(p1, i1, d1);

        // Pinpoint: use shared driver for IMU heading only.
        driver = hardwareMap.get(GoBildaPinpointDriver.class, "pinpoint");
        initialParDirection = GoBildaPinpointDriver.EncoderDirection.FORWARD;
        initialPerpDirection = GoBildaPinpointDriver.EncoderDirection.FORWARD;
    }

    /** Runs turret control using odometry heading (field-relative angle). */
    public void runTurretGyro() {
        double ticks = turretMotor.getCurrentPosition();
        double rawOutputDeg = (ticks / TICKS_PER_MOTOR_REV) * 360.0 * GEAR_RATIO;
        motorPosition = ticks;

        outputDeg = wrapDeg180(rawOutputDeg);
        double wrappedTicks = outputDeg * TICKS_PER_DEG;
        currentAngle = wrapDeg360(botHeading - rawOutputDeg);

        targetOutputDeg = wrapDeg180(botHeading - targetAngle);
        targetOutputDeg = Math.max(-robotRelativeLimitDeg, Math.min(robotRelativeLimitDeg, targetOutputDeg));
        double revs = (outputDeg - targetOutputDeg) / 360.0;
        double k = Math.round(revs);
        double targetAngleRobot = targetOutputDeg + 360.0 * k;
        targetAngleRobot = Math.max(-robotRelativeLimitDeg, Math.min(robotRelativeLimitDeg, targetAngleRobot));
        targetPos = targetAngleRobot * TICKS_PER_DEG;

        SimpleMotorFeedforward feedforward = new SimpleMotorFeedforward(kS, kV, kA);
        controller.setPID(p1, i1, d1);
        double pid2 = controller.calculate(wrappedTicks, targetPos);
        double ff = feedforward.calculate(0);
        power = pid2 + ff;
        turretMotor.setPower(turretDriveEnabled ? power : 0);
    }

    /**
     * Operator nudge/center reads moved out of runTurretGyro for cleaner ownership.
     */
    public void applyOperatorNudge(Gamepad gamepad2) {
        if (gamepad2 == null) return;
        if (Math.abs(gamepad2.left_stick_x) > 0.1) {
            aimOffsetDeg -= gamepad2.left_stick_x * nudgeDegPerUnit;
            aimOffsetDeg = Math.max(-aimOffsetMaxDeg, Math.min(aimOffsetMaxDeg, aimOffsetDeg));
        }
        if (gamepad2.aWasPressed()) {
            targetAngle = 0;
        }
    }

    /**
     * Runs turret control using a fixed heading value k (manual mode). Robot-relative limit = manualModeLimitDeg.
     */
    public void runTurretNoGyro(double k) {
        double ticks = turretMotor.getCurrentPosition();
        double rawOutputDeg = (ticks / TICKS_PER_MOTOR_REV) * 360.0 * GEAR_RATIO;
        motorPosition = ticks;

        outputDeg = wrapDeg180(rawOutputDeg);
        double wrappedTicks = outputDeg * TICKS_PER_DEG;
        currentAngle = wrapDeg360(rawOutputDeg + k);

        double limit = manualModeLimitDeg;
        targetOutputDeg = wrapDeg180(targetAngle - k);
        targetOutputDeg = Math.max(-limit, Math.min(limit, targetOutputDeg));
        double revs = (outputDeg - targetOutputDeg) / 360.0;
        double kRev = Math.round(revs);
        double targetAngleRobot = targetOutputDeg + 360.0 * kRev;
        targetAngleRobot = Math.max(-limit, Math.min(limit, targetAngleRobot));
        targetPos = targetAngleRobot * TICKS_PER_DEG;

        SimpleMotorFeedforward feedforward = new SimpleMotorFeedforward(kS, kV, kA);
        controller.setPID(p1, i1, d1);
        double pid2 = controller.calculate(wrappedTicks, targetPos);
        double ff = feedforward.calculate(0);
        power = pid2 + ff;
        double pwr = manualModeInvert ? -power : power;
        turretMotor.setPower(turretDriveEnabled ? pwr : 0);
    }

    /** Re-sync encoder to physical position (fixes drift after gear skip). */
    public void resyncEncoder() {
        turretMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        turretMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        controller.reset();
        targetAngle = botHeading;
    }

    /** Sets target turret angle in field frame (degrees). */
    public void setAngle(double target) {
        targetAngle = wrapDeg360(target);
    }

    /** Compatibility alias for callers that set heading on the wrapper. */
    public void setRobotHeadingDeg(double headingDeg) {
        botHeading = wrapDeg360(headingDeg);
    }

    /** Compatibility alias for wrapper API. */
    public void runFieldRelativeAim() { runTurretGyro(); }

    /** Compatibility alias for wrapper API. */
    public void runManualLockOn(double frozenHeadingDeg) { runTurretNoGyro(frozenHeadingDeg); }

    /** Compatibility alias for wrapper API. */
    public void setFieldTargetAngleDeg(double angleDeg) { setAngle(angleDeg); }

    /** Compatibility alias for wrapper API. */
    public double getRobotRelativeDeg() { return getTurretAngleRobot(); }

    /** Compatibility alias for wrapper API. */
    public double getFieldDeg() { return getTurretAngleField(); }

    public double getTurretAngle() {
        return currentAngle;
    }

    public double getTurretAngleField() {
        return currentAngle;
    }

    /** Returns current turret angle relative to robot (degrees), [-180, 180). */
    public double getTurretAngleRobot() {
        double ticks = turretMotor.getCurrentPosition();
        double robotDeg = (ticks / TICKS_PER_MOTOR_REV) * 360.0 * GEAR_RATIO;
        return wrapDeg180(robotDeg);
    }

    /** Direct power control (no PID). Use for manual testing only. */
    public void turretPow(double calc) {
        turretMotor.setPower(calc);
    }
}
