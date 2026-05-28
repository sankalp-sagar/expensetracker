import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

/**
 * Subscribes to /topic/balances/{groupId} on the settlement-service WebSocket endpoint.
 * Reconnects automatically. Calls `onMessage` with the parsed balances payload.
 *
 * settlement-service runs at :8085 in docker-compose. Override via REACT_APP_WS_BASE.
 */
export function useBalanceSocket(groupId, onMessage) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!groupId) return undefined;

    const wsBase = process.env.REACT_APP_WS_BASE || "http://localhost:8085";
    const client = new Client({
      webSocketFactory: () => new SockJS(`${wsBase}/ws`),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      onConnect: () => {
        client.subscribe(`/topic/balances/${groupId}`, (msg) => {
          try {
            const payload = JSON.parse(msg.body);
            onMessage(payload);
          } catch (e) {
            // ignore malformed frame
          }
        });
      },
      onStompError: () => { /* silent: backend may be offline */ },
    });

    client.activate();
    clientRef.current = client;

    return () => { client.deactivate(); };
  }, [groupId, onMessage]);
}
