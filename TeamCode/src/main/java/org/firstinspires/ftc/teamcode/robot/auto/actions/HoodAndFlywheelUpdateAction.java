package org.firstinspires.ftc.teamcode.robot.auto.actions;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.util.Action;

import java.util.function.BooleanSupplier;

/** Continuously updates hood/flywheel from odometry distance while running. */
public class HoodAndFlywheelUpdateAction implements Action {
    private final RobotContainer robot;
    private final double goalX;
    private final double goalY;
    private final BooleanSupplier keepRunning;
    private final BooleanSupplier feedBalls;

    public HoodAndFlywheelUpdateAction(RobotContainer robot, double goalX, double goalY, BooleanSupplier keepRunning) {
        this(robot, goalX, goalY, keepRunning, () -> false);
    }

    public HoodAndFlywheelUpdateAction(RobotContainer robot,
                                       double goalX,
                                       double goalY,
                                       BooleanSupplier keepRunning,
                                       BooleanSupplier feedBalls) {
        this.robot = robot;
        this.goalX = goalX;
        this.goalY = goalY;
        this.keepRunning = keepRunning;
        this.feedBalls = feedBalls;
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        if (!keepRunning.getAsBoolean()) return false;
        robot.updateOdometry();
        double dist = Math.hypot(goalX - robot.state.getPose().position.x, goalY - robot.state.getPose().position.y);
        robot.shootSequence.setHoodAndFlywheelFromDistance(dist);
        robot.shootSequence.runLauncherAuto(feedBalls.getAsBoolean());
        return true;
    }
}
