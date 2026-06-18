# Panels WebSocket Protocol

Panels runs two servers on the Robot Controller:

| Service | Port | URL (Control Hub) |
|---------|------|-------------------|
| Web UI | 8001 | http://192.168.43.1:8001 |
| WebSocket | 8002 | ws://192.168.43.1:8002 |

Phone RC uses `192.168.49.1` instead.

## Message format

All WebSocket messages are JSON:

```json
{
  "pluginID": "com.bylazar.cursoragent",
  "messageID": "drive",
  "data": { "x": 0.5, "y": 0, "rotate": 0, "requestId": "req-1" }
}
```

## Cursor Agent plugin (`com.bylazar.cursoragent`)

### Commands (client → robot)

| messageID | data | Description |
|-----------|------|-------------|
| `drive` | `{ x, y, rotate, fieldCentric? }` | Mecanum drive (-1..1), expires after 200ms |
| `setMotor` | `{ name, power, mode? }` | Motor power or velocity |
| `setServo` | `{ name, position }` | Servo position 0-1 |
| `followPath` | `{ waypoints: [{x,y,headingDeg?}], holdEnd? }` | Pedro path in FTC field inches |
| `estop` | `{}` | Emergency stop |
| `clearEstop` | `{}` | Clear e-stop |
| `getSnapshot` | `{}` | Full state snapshot |
| `getTelemetry` | `{}` | Same as getSnapshot |

### Events (robot → client)

| messageID | Description |
|-----------|-------------|
| `agentResponse` | `{ requestId, type, payload }` command ack |
| `agentSnapshot` | Periodic pose/telemetry state |
| `agentReady` | Whether AgentTeleop executor is registered |

## Other plugin IDs (proxied by MCP)

| pluginID | messageID | Purpose |
|----------|-----------|---------|
| `com.bylazar.opmodecontrol` | `initOpMode`, `startActiveOpMode`, `stopActiveOpMode` | OpMode lifecycle |
| `com.bylazar.gamepad` | `gamepad0` | Virtual gamepad |
| `com.bylazar.configurables` | `updatedConfigurable` | Live PID/tuning |

## Health check

`GET http://192.168.43.1:8002/health` returns `OK` when Panels socket is up.
