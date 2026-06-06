package org.firstinspires.ftc.teamcode.robot.auto.actions;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.util.Action;

/** Stops flywheel, intake, and transfer motors. */
public class StopLauncherAction implements Action {
    private final RobotContainer robot;

    public StopLauncherAction(RobotContainer robot) {
        this.robot = robot;
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        robot.shootSequence.stopLauncherAuto();
        return false;
    }
}
