export const PLUGIN_ID = "com.bylazar.cursoragent";
export const OPMODE_PLUGIN_ID = "com.bylazar.opmodecontrol";
export const GAMEPAD_PLUGIN_ID = "com.bylazar.gamepad";
export const CONFIGURABLES_PLUGIN_ID = "com.bylazar.configurables";

export const DEFAULT_HOST = process.env.PANELS_ROBOT_HOST ?? "192.168.43.1";
export const DEFAULT_WS_PORT = Number(process.env.PANELS_WS_PORT ?? "8002");
export const DEFAULT_HTTP_PORT = Number(process.env.PANELS_HTTP_PORT ?? "8001");

export interface SocketMessage {
  pluginID: string;
  messageID: string;
  data?: unknown;
}

export interface AgentResponseEnvelope {
  requestId?: string;
  type?: string;
  payload?: unknown;
}

export interface AgentSnapshot {
  connected?: boolean;
  estop?: boolean;
  opModeActive?: boolean;
  poseX?: number;
  poseY?: number;
  headingDeg?: number;
  velocityX?: number;
  velocityY?: number;
  lastCommand?: string;
  lastError?: string;
  pathBusy?: boolean;
  visionTags?: Array<{
    id: number;
    x: number;
    y: number;
    z: number;
    range: number;
    bearing: number;
  }>;
  telemetry?: Record<string, string>;
}

export interface CommandResult {
  ok: boolean;
  message?: string;
  requestId?: string;
}
