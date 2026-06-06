package org.firstinspires.ftc.teamcode.robot.shooter;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.robot.teleop.TeleopBindings;

/**
 * Teleop launcher/intake/transfer behavior for competition and demo modes.
 */
@Config
public class ShooterTeleop {
    public static boolean manualMode = false;
    public static double manualHoodAngleDeg = 55.0;
    public static double manualTargetTicksPerSec = -1600.0;
    public static boolean airSortEnabled = false;

    public static int DEMO_VEL_STEP = 100;
    public static double demoTargetTicksPerSec = -1600.0;
    public static final double DEMO_VEL_MIN = -2000.0;
    public static final double DEMO_VEL_MAX = -800.0;
    public static int demoRumbleMs = 300;

    private final ShootSequence sequence;
    private boolean demoPrevAtSpeed;

    public ShooterTeleop(ShootSequence sequence) {
        this.sequence = sequence;
    }

    public void updateTeleop(TeleopBindings bindings) {
        if (bindings == null || bindings.operator == null) return;
        Gamepad g2 = bindings.operator;
        Gamepad g1 = bindings.driver;

        applyLauncherMode(g2, g1);
        applyVelocityNudge(g1);

        if (g1 != null && g1.b) {
            sequence.intake.stop();
            sequence.transfer.stop();
        }

        if (g2.left_trigger > 0.1) {
            sequence.flywheel.updateSpinning();
        } else {
            sequence.flywheel.stop();
        }

        if (g2.right_trigger > 0.1) {
            if (sequence.flywheel.isAtTarget()) {
                sequence.transfer.setPower(-1.0);
                sequence.intake.setPower(-1.0);
            } else {
                sequence.transfer.stop();
                sequence.intake.stop();
            }
        } else {
            sequence.transfer.stop();
            if (g2.left_trigger < 0.1) sequence.intake.stop();
        }

        updateIntakeTeleop(bindings);
        updateTransferTeleop(bindings);
    }

    private void applyLauncherMode(Gamepad g2, Gamepad g1) {
        if (g2.left_bumper) {
            sequence.flywheel.setTargetTicksPerSec(-1480);
            sequence.hood.setServoPosition(0.8);
            sequence.hood.invalidateCache();
        } else if (manualMode) {
            sequence.hood.setAngleDeg(manualHoodAngleDeg);
            sequence.flywheel.setTargetTicksPerSec(manualTargetTicksPerSec);
        } else if (airSortEnabled && sequence.isAirSortPresetActive()) {
            sequence.hood.setAngleDeg(sequence.getAirSortHoodDeg());
            sequence.flywheel.setTargetTicksPerSec(sequence.getAirSortTargetTicksPerSec());
        } else {
            ShotCalculator.ShotTargets t = sequence.shotCalculator.fromRobotState(sequence.getRobotState());
            sequence.hood.setAngleDeg(t.hoodAngleDeg);
            sequence.flywheel.setTargetTicksPerSec(t.flywheelTicksPerSec);
        }
    }

    private void applyVelocityNudge(Gamepad g1) {
        if (g1 == null) return;
        double target = sequence.flywheel.getTargetTicksPerSec();
        if (g1.dpad_up && !g1.dpad_down) {
            if (g1.dpadUpWasPressed()) target -= 50;
        } else if (g1.dpadDownWasPressed()) {
            target += 50;
        }
        sequence.flywheel.setTargetTicksPerSec(target);
    }

    private void updateIntakeTeleop(TeleopBindings bindings) {
        Gamepad g1 = bindings.driver;
        Gamepad g2 = bindings.operator;
        if (g1 == null) return;
        if (g2 != null && g2.right_trigger > 0.1) return;

        if (g1.right_trigger > 0.25) {
            sequence.intake.setPower(-1);
        } else if (g1.left_trigger > 0.25) {
            sequence.intake.setPower(g1.left_trigger);
        } else if (g1.left_trigger < 0.25 && g1.right_trigger < 0.25) {
            if (g2 == null || g2.right_trigger < 0.25) sequence.intake.stop();
        }
        if (g1.b) {
            sequence.intake.stop();
            sequence.transfer.stop();
        }
        if (sequence.balls.hasThreeBalls() && g1 != null) g1.rumble(1500);
    }

    private void updateTransferTeleop(TeleopBindings bindings) {
        Gamepad g1 = bindings.driver;
        Gamepad g2 = bindings.operator;
        if (g1 == null || g2 == null) return;
        if (g2.right_trigger > 0.1) return;

        if (g1.right_trigger > 0.25) {
            sequence.transfer.setPower(sequence.balls.hasBallAtLauncher() ? 0 : -0.2);
            return;
        }
        if (g1.right_bumper) {
            sequence.transfer.setPower(sequence.balls.hasBallAtLauncher() ? 0.15 : -0.35);
        } else if (g1.left_bumper) {
            sequence.transfer.setPower(0.35);
        } else {
            sequence.transfer.stop();
        }
    }

    public void updateDemo(Gamepad g2) {
        if (g2 == null) return;
        if (g2.b) {
            sequence.flywheel.stop();
            sequence.intake.stop();
            sequence.transfer.stop();
            demoPrevAtSpeed = false;
            return;
        }

        if (g2.dpadLeftWasPressed()) manualHoodAngleDeg -= 2;
        else if (g2.dpadRightWasPressed()) manualHoodAngleDeg += 2;
        manualHoodAngleDeg = Math.max(HoodSubsystem.minAngleDeg, Math.min(HoodSubsystem.maxAngleDeg, manualHoodAngleDeg));
        sequence.hood.setAngleDeg(manualHoodAngleDeg);

        if (g2.dpadUpWasPressed()) demoTargetTicksPerSec -= DEMO_VEL_STEP;
        else if (g2.dpadDownWasPressed()) demoTargetTicksPerSec += DEMO_VEL_STEP;
        demoTargetTicksPerSec = Math.max(DEMO_VEL_MIN, Math.min(DEMO_VEL_MAX, demoTargetTicksPerSec));
        sequence.flywheel.setTargetTicksPerSec(demoTargetTicksPerSec);

        boolean atSpeed = false;
        if (g2.left_trigger > 0.1) {
            sequence.flywheel.updateSpinning();
            atSpeed = sequence.flywheel.isAtTarget();
        } else {
            sequence.flywheel.stop();
        }
        if (atSpeed && !demoPrevAtSpeed) g2.rumble(demoRumbleMs);
        demoPrevAtSpeed = atSpeed && g2.left_trigger > 0.1;

        if (g2.right_trigger > 0.1) {
            if (sequence.flywheel.isAtTarget()) {
                sequence.transfer.setPower(-1.0);
                sequence.intake.setPower(-1.0);
            } else {
                sequence.transfer.stop();
                sequence.intake.stop();
            }
        } else {
            sequence.transfer.stop();
            if (g2.left_trigger < 0.1) sequence.intake.stop();
        }
        updateDemoIntake(g2);
    }

    public void updateDemoIntake(Gamepad g2) {
        if (g2 == null || g2.b || g2.right_trigger > 0.1) return;
        if (g2.right_bumper) {
            sequence.intake.setPower(-1.0);
            sequence.transfer.setPower(-0.2);
        } else if (g2.left_bumper) {
            sequence.intake.setPower(1.0);
            sequence.transfer.stop();
        } else if (g2.left_trigger < 0.1) {
            sequence.intake.stop();
            sequence.transfer.stop();
        }
    }
}
