package org.firstinspires.ftc.teamcode.robot.auto.actions;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.util.Action;

import java.util.function.BooleanSupplier;

/** Continuously aims turret at goal while keepRunning is true. */
public class TurretAimAction implements Action {
    private final RobotContainer robot;
    private final double goalX;
    private final double goalY;
    private final BooleanSupplier keepRunning;

    public TurretAimAction(RobotContainer robot, double goalX, double goalY, BooleanSupplier keepRunning) {
        this.robot = robot;
        this.goalX = goalX;
        this.goalY = goalY;
        this.keepRunning = keepRunning;
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        if (!keepRunning.getAsBoolean()) {
            robot.turret.turretPow(0);
            return false;
        }
        robot.updateOdometry();
        double dx = goalX - robot.state.getPose().position.x;
        double dy = goalY - robot.state.getPose().position.y;
        robot.turret.targetAngle = org.firstinspires.ftc.teamcode.robot.turret.TurretSubsystem.wrapDeg360(
                Math.toDegrees(Math.atan2(dy, dx))
        );
        robot.turret.runTurretGyro();
        return true;
    }
}
