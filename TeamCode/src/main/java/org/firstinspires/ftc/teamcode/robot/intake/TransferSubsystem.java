package org.firstinspires.ftc.teamcode.robot.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;

/** Transfer belt motor only. */
public class TransferSubsystem {
    private final DcMotorEx motor;

    public TransferSubsystem(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotorEx.class, DeviceNames.TRANSFER);
        motor.setDirection(DcMotorSimple.Direction.FORWARD);
    }

    /** Power clamped to [-1, 1] per FTC motor API contract. */
    public void setPower(double power) {
        motor.setPower(Math.max(-1.0, Math.min(1.0, power)));
    }

    public void stop() { motor.setPower(0); }
}
