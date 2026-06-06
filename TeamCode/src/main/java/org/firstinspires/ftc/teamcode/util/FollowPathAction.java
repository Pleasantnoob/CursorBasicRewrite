package org.firstinspires.ftc.teamcode.util;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.pedropathing.paths.PathChain;

import org.firstinspires.ftc.teamcode.robot.drive.DriveSubsystem;

/** Runs a Pedro path until complete. Call once per path segment. */
public class FollowPathAction implements Action {
    private final DriveSubsystem drive;
    private final PathChain pathChain;
    private final boolean holdEnd;
    private boolean started;

    public FollowPathAction(DriveSubsystem drive, PathChain pathChain) {
        this(drive, pathChain, false);
    }

    public FollowPathAction(DriveSubsystem drive, PathChain pathChain, boolean holdEnd) {
        this.drive = drive;
        this.pathChain = pathChain;
        this.holdEnd = holdEnd;
    }

    @Override
    public boolean run(TelemetryPacket packet) {
        if (!started) {
            drive.followPath(pathChain, holdEnd);
            started = true;
        }
        drive.update();
        return drive.isBusy();
    }
}
