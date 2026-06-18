import WebSocket from "ws";
import {
  AgentResponseEnvelope,
  AgentSnapshot,
  DEFAULT_HOST,
  DEFAULT_HTTP_PORT,
  DEFAULT_WS_PORT,
  PLUGIN_ID,
  SocketMessage,
} from "./types.js";

type PendingRequest = {
  resolve: (value: unknown) => void;
  reject: (error: Error) => void;
  timer: NodeJS.Timeout;
};

export class PanelsClient {
  private ws: WebSocket | null = null;
  private readonly pending = new Map<string, PendingRequest>();
  private lastSnapshot: AgentSnapshot = {};
  private connected = false;
  private requestCounter = 0;

  constructor(
    private readonly host = DEFAULT_HOST,
    private readonly wsPort = DEFAULT_WS_PORT,
    private readonly httpPort = DEFAULT_HTTP_PORT,
  ) {}

  async connect(timeoutMs = 8000): Promise<{ ok: boolean; message: string }> {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return { ok: true, message: "already connected" };
    }

    try {
      await this.checkHealth();
    } catch (err) {
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

  async checkHealth(): Promise<void> {
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
    } finally {
      clearTimeout(timer);
    }
  }

  isConnected(): boolean {
    return this.connected && this.ws?.readyState === WebSocket.OPEN;
  }

  getLastSnapshot(): AgentSnapshot {
    return this.lastSnapshot;
  }

  async send(
    pluginID: string,
    messageID: string,
    data: unknown = null,
    timeoutMs = 5000,
  ): Promise<unknown> {
    if (!this.isConnected()) {
      const result = await this.connect();
      if (!result.ok) throw new Error(result.message);
    }

    const requestId = `req-${++this.requestCounter}`;
    const payload =
      data && typeof data === "object"
        ? { ...(data as Record<string, unknown>), requestId }
        : { requestId, value: data };

    const message: SocketMessage = { pluginID, messageID, data: payload };

    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(requestId);
        reject(new Error(`timeout waiting for ${pluginID}/${messageID}`));
      }, timeoutMs);

      this.pending.set(requestId, { resolve, reject, timer });
      this.ws!.send(JSON.stringify(message));
    });
  }

  async sendAgent(messageID: string, data: unknown = null): Promise<unknown> {
    return this.send(PLUGIN_ID, messageID, data);
  }

  private handleMessage(raw: string): void {
    let parsed: SocketMessage;
    try {
      parsed = JSON.parse(raw) as SocketMessage;
    } catch {
      return;
    }

    if (parsed.pluginID === PLUGIN_ID && parsed.messageID === "agentSnapshot") {
      this.lastSnapshot = (parsed.data ?? {}) as AgentSnapshot;
      return;
    }

    if (parsed.pluginID === PLUGIN_ID && parsed.messageID === "agentResponse") {
      const envelope = (parsed.data ?? {}) as AgentResponseEnvelope;
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

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
    this.connected = false;
  }
}

function extractRequestId(data: unknown): string {
  if (!data || typeof data !== "object") return "";
  const record = data as Record<string, unknown>;
  return typeof record.requestId === "string" ? record.requestId : "";
}
