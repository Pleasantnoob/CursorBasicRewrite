# Accessing Panels Web UI

Source: [Panels Accessing docs](https://panels.bylazar.com/docs/com.bylazar.docs/Accessing/)

## Steps

1. Connect to the Robot Controller WiFi (passphrase in DS Program & Manage menu)
2. Open the Panels dashboard:
   - **Control Hub:** http://192.168.43.1:8001
   - **Phone RC:** http://192.168.49.1:8001

## Cursor MCP connection

The MCP server connects to the WebSocket on port **8002** (same host):

- **Control Hub:** `ws://192.168.43.1:8002`
- **Phone RC:** `ws://192.168.49.1:8002`

Set `PANELS_ROBOT_HOST` in `.cursor/mcp.json` if using a different address.

## Agent workflow

1. Join Control Hub WiFi on your laptop
2. Deploy APK with Panels + CursorAgentPlugin to the robot
3. Enable MCP server in Cursor
4. Call `robot_connect` then `robot_start_opmode` with name `Agent Teleop`
5. Use `robot_drive`, `robot_set_motor`, etc.
