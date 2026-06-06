package org.firstinspires.ftc.teamcode.robot.vision;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.robot.hardware.DeviceNames;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/** Webcam + AprilTag processor setup. Single webcam name from {@link DeviceNames}. */
public class AprilTagVision {
    public final AprilTagProcessor processor;
    public final VisionPortal portal;

    public AprilTagVision(HardwareMap hardwareMap, boolean useWebcam) {
        processor = new AprilTagProcessor.Builder()
                .setTagLibrary(AprilTagGameDatabase.getCurrentGameTagLibrary())
                .setLensIntrinsics(909.963833736, 909.963833736, 634.495942075, 347.541434786)
                .build();
        processor.setDecimation(3);

        VisionPortal.Builder builder = new VisionPortal.Builder();
        if (useWebcam) {
            builder.setCamera(hardwareMap.get(WebcamName.class, DeviceNames.WEBCAM));
            builder.setCameraResolution(new Size(1280, 720));
        } else {
            builder.setCamera(BuiltinCameraDirection.BACK);
        }
        builder.setStreamFormat(VisionPortal.StreamFormat.MJPEG);
        builder.addProcessor(processor);
        portal = builder.build();
    }

    public void setDecimation(int level) {
        processor.setDecimation(level);
    }
}
