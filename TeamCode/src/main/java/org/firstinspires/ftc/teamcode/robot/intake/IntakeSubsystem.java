package org.firstinspires.ftc.teamcode.robot.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;

/** Intake roller motor only. */
public class IntakeSubsystem {
    private final DcMotorEx motor;

    public IntakeSubsystem(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotorEx.class, DeviceNames.INTAKE);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    /** Power clamped to [-1, 1] per FTC motor API contract. */
    public void setPower(double power) {
        motor.setPower(Math.max(-1.0, Math.min(1.0, power)));
    }

    public void stop() { motor.setPower(0); }
}
