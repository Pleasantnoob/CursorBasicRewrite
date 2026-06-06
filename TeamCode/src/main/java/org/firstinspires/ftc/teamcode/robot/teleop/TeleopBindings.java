package org.firstinspires.ftc.teamcode.robot.teleop;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Holds driver and operator gamepads. Subsystems receive this instead of reading gamepads directly.
 * driver may be null in demo mode (G2-only).
 */
public class TeleopBindings {
    public final Gamepad driver;
    public final Gamepad operator;

    public boolean slowStrafe;
    public boolean slowRotate;

    public TeleopBindings(Gamepad driver, Gamepad operator) {
        this.driver = driver;
        this.operator = operator;
    }

    /** Update edge-triggered toggles (call once per loop). */
    public void updateSlowToggles() {
        if (driver == null) return;
        if (driver.dpadLeftWasPressed()) slowStrafe = !slowStrafe;
        if (driver.dpadRightWasPressed()) slowRotate = !slowRotate;
    }

    public double strafeScale() { return slowStrafe ? 0.25 : 1.0; }

    public double rotateScale() { return slowRotate ? 0.25 : 1.0; }
}
