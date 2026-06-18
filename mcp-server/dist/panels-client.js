import WebSocket from "ws";
import { DEFAULT_HOST, DEFAULT_HTTP_PORT, DEFAULT_WS_PORT, PLUGIN_ID, } from "./types.js";
export class PanelsClient {
    host;
    wsPort;
    httpPort;
    ws = null;
    pending = new Map();
    lastSnapshot = {};
    connected = false;
    requestCounter = 0;
    constructor(host = DEFAULT_HOST, wsPort = DEFAULT_WS_PORT, httpPort = DEFAULT_HTTP_PORT) {
        this.host = host;
        this.wsPort = wsPort;
        this.httpPort = httpPort;
    }
    async connect(timeoutMs = 8000) {
        if (this.ws?.readyState === WebSocket.OPEN) {
            return { ok: true, message: "already connected" };
        }
        try {
            await this.checkHealth();
        }
        catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            return { ok: false, message };
        }
        return new Promise((resolve) => {
            const url = `ws://${this.host}:${this.wsPort}`;
            const socket = new WebSocket(url);
            const timer = setTimeout(() => {
                socket.terminate();
                resolve({ ok: false, message: `connection timeout to ${url}` });
            }, timeoutMs);
            socket.on("open", () => {
                clearTimeout(timer);
                this.ws = socket;
                this.connected = true;
                resolve({ ok: true, message: `connected to ${url}` });
            });
            socket.on("message", (raw) => this.handleMessage(raw.toString()));
            socket.on("close", () => {
                this.connected = false;
                this.ws = null;
                for (const [id, pending] of this.pending) {
                    clearTimeout(pending.timer);
                    pending.reject(new Error("WebSocket closed"));
                    this.pending.delete(id);
                }
            });
            socket.on("error", (err) => {
                clearTimeout(timer);
                resolve({ ok: false, message: err.message });
            });
        });
    }
    async checkHealth() {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), 5000);
        try {
            const res = await fetch(`http://${this.host}:${this.httpPort}/health`, {
                signal: controller.signal,
            }).catch(() => null);
            if (!res?.ok) {
                const wsHealth = await fetch(`http://${this.host}:${this.wsPort}/health`, {
                    signal: controller.signal,
                }).catch(() => null);
                if (!wsHealth?.ok) {
                    throw new Error(`Panels health check failed at ${this.host}`);
                }
            }
        }
        finally {
            clearTimeout(timer);
        }
    }
    isConnected() {
        return this.connected && this.ws?.readyState === WebSocket.OPEN;
    }
    getLastSnapshot() {
        return this.lastSnapshot;
    }
    async send(pluginID, messageID, data = null, timeoutMs = 5000) {
        if (!this.isConnected()) {
            const result = await this.connect();
            if (!result.ok)
                throw new Error(result.message);
        }
        const requestId = `req-${++this.requestCounter}`;
        const payload = data && typeof data === "object"
            ? { ...data, requestId }
            : { requestId, value: data };
        const message = { pluginID, messageID, data: payload };
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                this.pending.delete(requestId);
                reject(new Error(`timeout waiting for ${pluginID}/${messageID}`));
            }, timeoutMs);
            this.pending.set(requestId, { resolve, reject, timer });
            this.ws.send(JSON.stringify(message));
        });
    }
    async sendAgent(messageID, data = null) {
        return this.send(PLUGIN_ID, messageID, data);
    }
    handleMessage(raw) {
        let parsed;
        try {
            parsed = JSON.parse(raw);
        }
        catch {
            return;
        }
        if (parsed.pluginID === PLUGIN_ID && parsed.messageID === "agentSnapshot") {
            this.lastSnapshot = (parsed.data ?? {});
            return;
        }
        if (parsed.pluginID === PLUGIN_ID && parsed.messageID === "agentResponse") {
            const envelope = (parsed.data ?? {});
            const requestId = envelope.requestId ?? "";
            const pending = this.pending.get(requestId);
            if (pending) {
                clearTimeout(pending.timer);
                this.pending.delete(requestId);
                pending.resolve(envelope.payload ?? envelope);
            }
            return;
        }
        if (parsed.pluginID === PLUGIN_ID) {
            const requestId = extractRequestId(parsed.data);
            const pending = requestId ? this.pending.get(requestId) : undefined;
            if (pending) {
                clearTimeout(pending.timer);
                this.pending.delete(requestId);
                pending.resolve(parsed.data);
            }
        }
    }
    disconnect() {
        this.ws?.close();
        this.ws = null;
        this.connected = false;
    }
}
function extractRequestId(data) {
    if (!data || typeof data !== "object")
        return "";
    const record = data;
    return typeof record.requestId === "string" ? record.requestId : "";
}
