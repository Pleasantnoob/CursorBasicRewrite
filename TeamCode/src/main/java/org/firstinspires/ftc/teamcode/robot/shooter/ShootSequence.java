package org.firstinspires.ftc.teamcode.robot.shooter;

import org.firstinspires.ftc.teamcode.robot.RobotState;
import org.firstinspires.ftc.teamcode.robot.intake.AirSort;
import org.firstinspires.ftc.teamcode.robot.intake.BallSensorArray;
import org.firstinspires.ftc.teamcode.robot.intake.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.robot.intake.TransferSubsystem;

/**
 * Autonomous-oriented launcher/intake sequencing API.
 */
public class ShootSequence {
    public final FlywheelSubsystem flywheel;
    public final HoodSubsystem hood;
    public final IntakeSubsystem intake;
    public final TransferSubsystem transfer;
    public final BallSensorArray balls;
    public final ShotCalculator shotCalculator;

    private final RobotState robotState;
    private boolean airSortPresetActive;
    private double airSortHoodDeg = 55.0;
    private double airSortTargetTicksPerSec = -1600.0;

    public ShootSequence(RobotState robotState,
                         FlywheelSubsystem flywheel,
                         HoodSubsystem hood,
                         IntakeSubsystem intake,
                         TransferSubsystem transfer,
                         BallSensorArray balls) {
        this.robotState = robotState;
        this.flywheel = flywheel;
        this.hood = hood;
        this.intake = intake;
        this.transfer = transfer;
        this.balls = balls;
        this.shotCalculator = new ShotCalculator();
    }

    public void setHoodAndFlywheelFromDistance(double distanceInches) {
        setHoodAndFlywheelFromDistance(distanceInches, 0);
    }

    public void setHoodAndFlywheelFromDistance(double distanceInches, double flywheelBoost) {
        if (!Double.isFinite(distanceInches)) {
            distanceInches = robotState.getDistanceToGoalInches();
        }
        ShotCalculator.ShotTargets t = shotCalculator.fromDistanceInches(distanceInches, flywheelBoost);
        hood.setAngleDeg(t.hoodAngleDeg);
        flywheel.setTargetTicksPerSec(t.flywheelTicksPerSec);
    }

    public void updateHoodAndFlywheelFromOdometry() {
        setHoodAndFlywheelFromDistance(robotState.getShotDistanceInches());
    }

    public void runLauncherAuto(boolean feed) {
        flywheel.updateSpinning();
        if (feed && flywheel.isAtTarget()) {
            transfer.setPower(-1.0);
            intake.setPower(-1.0);
        }
    }

    public void stopLauncherAuto() {
        flywheel.stop();
        transfer.stop();
        intake.stop();
    }

    public void runIntakeAuto(boolean run) {
        if (run) {
            intake.setPower(-1.0);
            transfer.setPower(-0.2);
        } else {
            intake.stop();
            transfer.stop();
        }
    }

    public void setIntakeMotorAuto(double power) { intake.setPower(power); }

    public void setTransferMotorAuto(double power) { transfer.setPower(power); }

    public void setAirSortPreset(AirSort.ShotMode mode, double distanceInches) {
        if (!ShooterTeleop.airSortEnabled) return;
        ShotCalculator.ShotTargets t = shotCalculator.airSortPreset(mode, distanceInches);
        airSortHoodDeg = t.hoodAngleDeg;
        airSortTargetTicksPerSec = t.flywheelTicksPerSec;
        airSortPresetActive = true;
        hood.invalidateCache();
    }

    public void clearAirSortPreset() { airSortPresetActive = false; }

    public boolean isAirSortPresetActive() { return airSortPresetActive; }

    public double getAirSortHoodDeg() { return airSortHoodDeg; }

    public double getAirSortTargetTicksPerSec() { return airSortTargetTicksPerSec; }

    public double getTargetTicksPerSec() { return flywheel.getTargetTicksPerSec(); }

    public double getFlywheelVelocity() { return flywheel.getVelocity(); }

    public boolean isAtTargetSpeed() { return flywheel.isAtTarget(); }

    public RobotState getRobotState() { return robotState; }
}
