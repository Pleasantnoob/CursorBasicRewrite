import { AgentSnapshot } from "./types.js";
export declare class PanelsClient {
    private readonly host;
    private readonly wsPort;
    private readonly httpPort;
    private ws;
    private readonly pending;
    private lastSnapshot;
    private connected;
    private requestCounter;
    constructor(host?: string, wsPort?: number, httpPort?: number);
    connect(timeoutMs?: number): Promise<{
        ok: boolean;
        message: string;
    }>;
    checkHealth(): Promise<void>;
    isConnected(): boolean;
    getLastSnapshot(): AgentSnapshot;
    send(pluginID: string, messageID: string, data?: unknown, timeoutMs?: number): Promise<unknown>;
    sendAgent(messageID: string, data?: unknown): Promise<unknown>;
    private handleMessage;
    disconnect(): void;
}
