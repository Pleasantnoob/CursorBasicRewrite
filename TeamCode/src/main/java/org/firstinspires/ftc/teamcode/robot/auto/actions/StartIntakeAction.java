package org.firstinspires.ftc.teamcode.robot.auto.actions;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.util.Action;

/** Instant action to start intake for far autos. */
public class StartIntakeAction implements Action {
    private final RobotContainer robot;

    public StartIntakeAction(RobotContainer robot) {
        this.robot = robot;
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        robot.shootSequence.setIntakeMotorAuto(-1.0);
        robot.shootSequence.setTransferMotorAuto(0.0);
        return false;
    }
}
