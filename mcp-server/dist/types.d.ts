export declare const PLUGIN_ID = "com.bylazar.cursoragent";
export declare const OPMODE_PLUGIN_ID = "com.bylazar.opmodecontrol";
export declare const GAMEPAD_PLUGIN_ID = "com.bylazar.gamepad";
export declare const CONFIGURABLES_PLUGIN_ID = "com.bylazar.configurables";
export declare const DEFAULT_HOST: string;
export declare const DEFAULT_WS_PORT: number;
export declare const DEFAULT_HTTP_PORT: number;
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
