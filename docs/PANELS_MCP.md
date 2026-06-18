# Panels + Cursor MCP — Quick Start

Control this robot from Cursor AI via Panels WebSocket.

## One-time setup

1. **Clone/open this repo** in Cursor (`Pleasantnoob/CursorBasicRewrite`)
2. **Build robot APK** (Android Studio or `.\gradlew :TeamCode:assembleDebug`)
3. **Deploy** to Control Hub, join its WiFi (`192.168.43.1`)
4. **MCP** — `.cursor/mcp.json` is already configured. Reload MCP in Cursor (Settings → MCP)

Optional — rebuild MCP server after editing TypeScript:

```powershell
cd mcp-server
npm install
npm run build
```

## Usage

1. `robot_connect` — connect to Panels WebSocket
2. `robot_start_opmode` — start **Agent Teleop**
3. `robot_drive`, `robot_set_motor`, `robot_get_telemetry`, etc.
4. `robot_stop` — e-stop + stop OpMode

## What's in this repo

| Path | Purpose |
|------|---------|
| `CursorAgentPlugin/` | Panels plugin (`com.bylazar.cursoragent`) |
| `TeamCode/.../AgentTeleop.java` | OpMode that runs agent commands |
| `mcp-server/` | Cursor MCP bridge (TypeScript) |
| `.cursor/mcp.json` | MCP config (portable, repo-relative) |
| `docs/reference/` | Panels protocol + robot agent docs |

## Safety

- OpMode must be **RUNNING** before motion commands work
- Drive commands expire after **200ms** — repeat to sustain motion
- `robot_stop` for immediate e-stop

See also: [panels-protocol.md](reference/panels-protocol.md)
