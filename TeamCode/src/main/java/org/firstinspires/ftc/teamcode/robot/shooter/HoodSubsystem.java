package org.firstinspires.ftc.teamcode.robot.shooter;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;

/** Hood servo: maps launch angle (degrees) to servo position. */
@Config
public class HoodSubsystem {
    public static double minAngleDeg = 40.0;
    public static double maxAngleDeg = 70.0;
    public static double minServo = 0.0;
    public static double maxServo = 1.0;
    private static final double HOOD_DEADBAND = 0.015;

    private final Servo hood;
    private double lastServoPosition = -1;
    private double currentAngleDeg = 55.0;

    public HoodSubsystem(HardwareMap hardwareMap) {
        hood = hardwareMap.get(Servo.class, DeviceNames.HOOD);
        hood.setDirection(Servo.Direction.FORWARD);
    }

    public double getAngleDeg() { return currentAngleDeg; }

    public void setAngleDeg(double angleDeg) {
        currentAngleDeg = Math.max(minAngleDeg, Math.min(maxAngleDeg, angleDeg));
        double angleSpan = maxAngleDeg - minAngleDeg;
        if (angleSpan <= 0) return; // misconfigured Dashboard — avoid divide-by-zero
        double servoPos = minServo + ((maxAngleDeg - currentAngleDeg) / angleSpan) * (maxServo - minServo);
        if (lastServoPosition < 0 || Math.abs(servoPos - lastServoPosition) > HOOD_DEADBAND) {
            hood.setPosition(servoPos);
            lastServoPosition = servoPos;
        }
    }

    /** Direct servo position (preset shots). */
    public void setServoPosition(double position) {
        hood.setPosition(position);
        lastServoPosition = position;
    }

    public double getServoPosition() { return hood.getPosition(); }

    public void invalidateCache() { lastServoPosition = -1; }
}
