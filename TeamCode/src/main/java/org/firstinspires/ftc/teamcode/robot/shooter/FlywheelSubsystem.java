package org.firstinspires.ftc.teamcode.robot.shooter;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.controller.PIDController;
import com.arcrobotics.ftclib.controller.wpilibcontroller.SimpleMotorFeedforward;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;

/** Flywheel motor PID + feedforward only. */
@Config
public class FlywheelSubsystem {
    public static double p = 0.006, i = 0.005, d = 0.0001;
    public static double kS = 0, kV = 0.00047, kA = 0.005;
    public static double accel = -30;
    public static int shootToleranceTicksPerSec = 20;
    public static int shootToleranceOverride = 0;

    public static double flywheelRadiusM = 0.048;
    public static double ticksPerRev = 28.0;

    private final DcMotorEx motor;
    private final PIDController controller;
    private double targetTicksPerSec;

    public FlywheelSubsystem(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotorEx.class, DeviceNames.LAUNCH);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        controller = new PIDController(p, i, d);
    }

    public void setTargetTicksPerSec(double target) {
        targetTicksPerSec = target;
    }

    public double getTargetTicksPerSec() { return targetTicksPerSec; }

    public double getVelocity() { return motor.getVelocity(); }

    /**
     * True when flywheel velocity is within tolerance of target.
     * Returns false if target is effectively zero (not spinning for a shot).
     */
    public boolean isAtTarget() {
        if (Math.abs(targetTicksPerSec) < 50) return false;
        int tol = shootToleranceOverride > 0 ? shootToleranceOverride : shootToleranceTicksPerSec;
        return Math.abs(motor.getVelocity() - targetTicksPerSec) <= tol;
    }

    /** PID+FF spin toward target. */
    public void updateSpinning() {
        controller.setPID(p, i, d);
        double vel = motor.getVelocity();
        SimpleMotorFeedforward ff = new SimpleMotorFeedforward(kS, kV, kA);
        double power = controller.calculate(vel, targetTicksPerSec) + ff.calculate(targetTicksPerSec, accel);
        motor.setPower(power);
    }

    public void stop() { motor.setPower(0); }

    public static double launchSpeedMpsFromTicksPerSec(double ticksPerSec) {
        return Math.abs(ticksPerSec) * 2 * Math.PI * flywheelRadiusM / ticksPerRev;
    }

    public static double ticksPerSecFromLaunchSpeedMps(double mps) {
        if (mps <= 0) return 0;
        return mps * ticksPerRev / (2 * Math.PI * flywheelRadiusM);
    }
}
