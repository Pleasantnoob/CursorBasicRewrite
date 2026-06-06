package org.firstinspires.ftc.teamcode.robot.intake;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;

/** Color sensors c1/c2/c3 — ball presence along intake path. */
public class BallSensorArray {
    public static final int BALL_BLUE_THRESHOLD = 150;

    public final ColorSensor slot1;
    public final ColorSensor slot2;
    public final ColorSensor slot3;

    public BallSensorArray(HardwareMap hardwareMap) {
        slot1 = hardwareMap.get(ColorSensor.class, DeviceNames.COLOR_1);
        slot2 = hardwareMap.get(ColorSensor.class, DeviceNames.COLOR_2);
        slot3 = hardwareMap.get(ColorSensor.class, DeviceNames.COLOR_3);
    }

    /** True when sensor reads high blue (ball present heuristic). */
    public boolean hasBallAtSlot(ColorSensor sensor) {
        if (sensor == null) return false;
        return sensor.blue() > BALL_BLUE_THRESHOLD;
    }

    /** All three slots occupied (legacy rumble trigger). */
    public boolean hasThreeBalls() {
        return hasBallAtSlot(slot1) && hasBallAtSlot(slot2) && hasBallAtSlot(slot3);
    }

    /** At least two balls — LED "ready" threshold. */
    public boolean hasAtLeastTwoBalls() {
        int n = 0;
        if (hasBallAtSlot(slot1)) n++;
        if (hasBallAtSlot(slot2)) n++;
        if (hasBallAtSlot(slot3)) n++;
        return n >= 2;
    }

    public boolean hasBallAtLauncher() {
        return hasBallAtSlot(slot3);
    }

    public int getLauncherSlotRgbSum() {
        return slot3.red() + slot3.green() + slot3.blue();
    }
}
