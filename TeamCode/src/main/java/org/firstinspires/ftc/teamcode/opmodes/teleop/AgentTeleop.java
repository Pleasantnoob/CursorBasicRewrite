package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.cursoragent.CursorAgentRuntime;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.robot.RobotContainer;
import org.firstinspires.ftc.teamcode.robot.RobotState;
import org.firstinspires.ftc.teamcode.robot.agent.CursorAgentExecutor;

import java.util.List;

/**
 * TeleOp entry point for Cursor MCP agent control. Start this OpMode, then use MCP tools
 * to drive, set motors/servos, follow paths, and read telemetry.
 */
@TeleOp(name = "Agent Teleop", group = "Agent")
public class AgentTeleop extends LinearOpMode {
    private static final int LOOP_SLEEP_MS = 20;

    @Override
    public void runOpMode() {
        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : hubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        RobotState.blueStartX = -50.0;
        RobotState.blueStartY = -50.0;
        RobotState.blueStartHeadingDeg = -128.0;
        RobotState.blueGoalX = -70.0;
        RobotState.blueGoalY = -70.0;
        RobotState.redStartX = -50.0;
        RobotState.redStartY = 50.0;
        RobotState.redStartHeadingDeg = 128.0;
        RobotState.redGoalX = -70.0;
        RobotState.redGoalY = 70.0;

        RobotContainer robot = new RobotContainer();
        robot.init(hardwareMap, null, null, true);
        robot.drive.startTeleopDrive();
        robot.state.setAllianceBlue(true);

        PanelsTelemetry.telemetry.debug("Cursor Agent Teleop ready");

        while (!isStarted() && !isStopRequested()) {
            telemetry.addLine("Agent Teleop — waiting for START");
            telemetry.addLine("Connect Cursor MCP to Control Hub WiFi");
            telemetry.addData("Panels WS", "192.168.43.1:8002");
            telemetry.update();
            sleep(LOOP_SLEEP_MS);
        }

        waitForStart();
        if (isStopRequested()) return;

        CursorAgentExecutor executor = new CursorAgentExecutor(robot, hardwareMap, true);
        CursorAgentRuntime.registerExecutor(executor);
        CursorAgentRuntime.estop.set(false);

        while (opModeIsActive()) {
            robot.drive.update();
            robot.state.updateFromPose(robot.drive.getPose());
            CursorAgentRuntime.processLoopTick();
            PanelsTelemetry.telemetry.debug(String.format(
                    "pose: %.1f, %.1f @ %.0f° | estop=%s",
                    robot.state.getPose().position.x,
                    robot.state.getPose().position.y,
                    Math.toDegrees(robot.state.getPose().heading),
                    CursorAgentRuntime.estop.get()
            ));
            PanelsTelemetry.telemetry.update(telemetry);
            sleep(LOOP_SLEEP_MS);
        }

        CursorAgentRuntime.registerExecutor(null);
        executor.executeEstop();
    }
}
