package org.firstinspires.ftc.teamcode.robot.auto.actions;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.util.Action;

/** Runs intake motor for a fixed duration. */
public class IntakeRunAction implements Action {
    private final RobotContainer robot;
    private final double durationSec;
    private final double runPower;
    private final double stopPower;
    private final ElapsedTime timer = new ElapsedTime();
    private boolean started;

    public IntakeRunAction(RobotContainer robot, double durationSec, double runPower) {
        this(robot, durationSec, runPower, 0.0);
    }

    public IntakeRunAction(RobotContainer robot, double durationSec, double runPower, double stopPower) {
        this.robot = robot;
        this.durationSec = durationSec;
        this.runPower = runPower;
        this.stopPower = stopPower;
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        if (!started) {
            timer.reset();
            started = true;
            robot.shootSequence.setIntakeMotorAuto(runPower);
        }
        if (timer.seconds() >= durationSec) {
            robot.shootSequence.setIntakeMotorAuto(stopPower);
            return false;
        }
        return true;
    }
}
