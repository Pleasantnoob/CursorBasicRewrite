package org.firstinspires.ftc.teamcode.robot.auto.actions;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.shooter.ShooterTeleop;
import org.firstinspires.ftc.teamcode.robot.turret.TurretAim;
import org.firstinspires.ftc.teamcode.util.Action;

import java.util.Objects;

/** Revs launcher and aims turret at goal for a fixed duration. */
public class RevAndAimAction implements Action {
    private final RobotContainer robot;
    private final double goalX;
    private final double goalY;
    private final double durationSec;
    private final ElapsedTime timer = new ElapsedTime();
    private boolean started;

    public RevAndAimAction(RobotContainer robot, double goalX, double goalY, double durationSec) {
        this.robot = Objects.requireNonNull(robot, "robot");
        if (!robot.isInitialized()) {
            throw new IllegalArgumentException("RobotContainer must be initialized before creating RevAndAimAction");
        }
        if (durationSec <= 0) {
            throw new IllegalArgumentException("durationSec must be positive");
        }
        this.goalX = goalX;
        this.goalY = goalY;
        this.durationSec = durationSec;
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        if (!started) {
            timer.reset();
            started = true;
        }
        robot.updateOdometry();
        double[] vel = robot.drive.getVelocity();
        TurretAim.AimSolution aim = TurretAim.solveAim(
                robot.state.getPose(),
                goalX, goalY,
                0,
                vel[0], vel[1],
                robot.state.turretVelocityCompensation,
                0.5, 1.5,
                ShooterTeleop.manualMode,
                ShooterTeleop.manualHoodAngleDeg,
                ShooterTeleop.manualTargetTicksPerSec
        );
        robot.state.distanceForHoodSpeedInches = aim.distanceForHoodSpeedInches;
        robot.shootSequence.setHoodAndFlywheelFromDistance(robot.state.getShotDistanceInches());
        robot.shootSequence.runLauncherAuto(false);
        robot.turret.targetAngle = aim.angleToGoalDeg;
        robot.turret.runTurretGyro();
        return timer.seconds() < durationSec;
    }
}
