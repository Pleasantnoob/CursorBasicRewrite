package org.firstinspires.ftc.teamcode.robot.auto.actions;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.util.Action;

import java.util.function.BooleanSupplier;

/** Fades LED between min and max values during autonomous. */
public class LedFadeAction implements Action {
    public static final double LED_MIN = 0.2799;
    public static final double LED_MAX = 0.728;
    public static final double FADE_PERIOD_SEC = 2.0;

    private final Servo led;
    private final BooleanSupplier keepRunning;
    private final ElapsedTime timer = new ElapsedTime();

    public LedFadeAction(Servo led, BooleanSupplier keepRunning) {
        this.led = led;
        this.keepRunning = keepRunning;
    }

    public static double fadePosition(double elapsedSec) {
        double t = elapsedSec / FADE_PERIOD_SEC;
        return LED_MIN + (LED_MAX - LED_MIN) * (0.5 + 0.5 * Math.sin(2 * Math.PI * t));
    }

    public static double positionAtSeconds(double elapsedSec) {
        return fadePosition(elapsedSec);
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        if (!keepRunning.getAsBoolean()) return false;
        led.setPosition(fadePosition(timer.seconds()));
        return true;
    }
}
