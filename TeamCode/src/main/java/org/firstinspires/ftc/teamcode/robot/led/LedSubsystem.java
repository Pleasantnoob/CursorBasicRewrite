package org.firstinspires.ftc.teamcode.robot.led;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.robot.auto.actions.LedFadeAction;
import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;
import org.firstinspires.ftc.teamcode.robot.intake.BallSensorArray;

/** LED servo: zone colors in teleop, sine fade in demo/auto. */
@Config
public class LedSubsystem {
    @Config
    public static class LedConfig {
        public static double posNormal = 0.35;
        public static double posReady = 0.5;
        public static double posRed = 0.2799;
    }

    private final Servo led;

    public LedSubsystem(HardwareMap hardwareMap) {
        led = hardwareMap.get(Servo.class, DeviceNames.LED);
    }

    public void setFadeAtElapsedSeconds(double seconds) {
        led.setPosition(LedFadeAction.positionAtSeconds(seconds));
    }

    public void updateTeleop(boolean inShootingZone, BallSensorArray balls) {
        if (balls == null) return;
        double pos;
        if (balls.hasAtLeastTwoBalls()) {
            pos = inShootingZone ? LedConfig.posReady : LedConfig.posNormal;
        } else {
            pos = LedConfig.posRed;
        }
        led.setPosition(pos);
    }
}
