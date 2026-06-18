#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { PanelsClient } from "./panels-client.js";
import { CONFIGURABLES_PLUGIN_ID, DEFAULT_HOST, OPMODE_PLUGIN_ID, } from "./types.js";
let client = new PanelsClient();
const server = new McpServer({
    name: "panels-robot",
    version: "1.0.0",
});
server.tool("robot_connect", "Connect to the FTC robot Panels WebSocket on the Control Hub", {
    host: z.string().optional().describe(`Robot host (default ${DEFAULT_HOST})`),
}, async ({ host }) => {
    if (host) {
        client = new PanelsClient(host);
    }
    const result = await client.connect();
    return {
        content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
    };
});
server.tool("robot_status", "Get robot agent snapshot: pose, estop, OpMode state, telemetry", {}, async () => {
    const snapshot = await client.sendAgent("getSnapshot", {});
    return {
        content: [{ type: "text", text: JSON.stringify(snapshot, null, 2) }],
    };
});
server.tool("robot_start_opmode", "Initialize and start an OpMode via Panels OpModeControl", {
    name: z.string().default("Agent Teleop").describe("OpMode name on Driver Station"),
}, async ({ name }) => {
    await client.send(OPMODE_PLUGIN_ID, "initOpMode", name);
    await client.send(OPMODE_PLUGIN_ID, "startActiveOpMode", null);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ ok: true, message: `Started OpMode: ${name}` }, null, 2),
            },
        ],
    };
});
server.tool("robot_stop", "Stop active OpMode and engage agent e-stop", {}, async () => {
    await client.sendAgent("estop", {});
    await client.send(OPMODE_PLUGIN_ID, "stopActiveOpMode", null);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ ok: true, message: "OpMode stopped and e-stop engaged" }, null, 2),
            },
        ],
    };
});
server.tool("robot_drive", "Drive mecanum: x=strafe, y=forward, rotate=turn (-1 to 1). Commands expire after 200ms — call repeatedly to sustain.", {
    x: z.number().min(-1).max(1).default(0),
    y: z.number().min(-1).max(1).default(0),
    rotate: z.number().min(-1).max(1).default(0),
    field_centric: z.boolean().default(false),
}, async ({ x, y, rotate, field_centric }) => {
    const result = await client.sendAgent("drive", {
        x,
        y,
        rotate,
        fieldCentric: field_centric,
    });
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
});
server.tool("robot_set_motor", "Set motor power or velocity. Names: fl, bl, fr, br, intake, launch, trans", {
    name: z.string(),
    power: z.number(),
    mode: z.enum(["power", "velocity"]).default("power"),
}, async ({ name, power, mode }) => {
    const result = await client.sendAgent("setMotor", { name, power, mode });
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
});
server.tool("robot_set_servo", "Set servo position (0-1). Names: hood, turret", {
    name: z.string(),
    position: z.number().min(0).max(1),
}, async ({ name, position }) => {
    const result = await client.sendAgent("setServo", { name, position });
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
});
server.tool("robot_set_config", "Tune a Panels Configurable field by id (live PID / tunables)", {
    id: z.string().describe("Configurable field id from Panels Configurables plugin"),
    value: z.string().describe("New value as string"),
}, async ({ id, value }) => {
    const result = await client.send(CONFIGURABLES_PLUGIN_ID, "updatedConfigurable", [
        { id, newValueString: value },
    ]);
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
});
server.tool("robot_get_telemetry", "Get latest agent telemetry snapshot from the robot", {}, async () => {
    const snapshot = await client.sendAgent("getTelemetry", {});
    return { content: [{ type: "text", text: JSON.stringify(snapshot, null, 2) }] };
});
server.tool("robot_follow_path", "Follow a path in official FTC field coordinates (inches, center origin)", {
    waypoints: z
        .array(z.object({
        x: z.number(),
        y: z.number(),
        heading_deg: z.number().optional(),
    }))
        .min(2),
    hold_end: z.boolean().default(true),
}, async ({ waypoints, hold_end }) => {
    const result = await client.sendAgent("followPath", {
        waypoints: waypoints.map((wp) => ({
            x: wp.x,
            y: wp.y,
            headingDeg: wp.heading_deg ?? Number.NaN,
        })),
        holdEnd: hold_end,
    });
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
});
server.tool("robot_get_vision", "Get AprilTag vision detections from the agent snapshot", {}, async () => {
    const snapshot = (await client.sendAgent("getSnapshot", {}));
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify(snapshot?.visionTags ?? [], null, 2),
            },
        ],
    };
});
server.tool("robot_clear_estop", "Clear agent e-stop after a stop command", {}, async () => {
    const result = await client.sendAgent("clearEstop", {});
    return { content: [{ type: "text", text: JSON.stringify(result, null, 2) }] };
});
async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
}
main().catch((err) => {
    console.error(err);
    process.exit(1);
});
