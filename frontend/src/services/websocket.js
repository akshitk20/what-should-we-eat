import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let stompClient = null;

/**
 * Connect to a room's WebSocket channel.
 * @param {string} roomCode - The room code to subscribe to.
 * @param {function} onMessage - Callback invoked with parsed message payload.
 * @returns {Client} The active STOMP client instance.
 */
export const connectToRoom = (roomCode, onMessage) => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 3000,
    onConnect: () => {
      console.log(`[WS] Connected — subscribing to /topic/room/${roomCode}`);
      stompClient.subscribe(`/topic/room/${roomCode}`, (message) => {
        try {
          const data = JSON.parse(message.body);
          onMessage(data);
        } catch (err) {
          console.error('[WS] Failed to parse message:', err);
        }
      });
    },
    onDisconnect: () => {
      console.log('[WS] Disconnected');
    },
    onStompError: (frame) => {
      console.error('[WS] STOMP error:', frame);
    },
  });

  stompClient.activate();
  return stompClient;
};

/**
 * Send a swipe event to the server.
 * @param {string} roomCode
 * @param {{ memberId: string, restaurantId: string, direction: 'LEFT'|'RIGHT' }} swipeData
 */
export const sendSwipe = (roomCode, swipeData) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: `/app/room/${roomCode}/swipe`,
      body: JSON.stringify(swipeData),
    });
  } else {
    console.warn('[WS] sendSwipe called but client is not connected');
  }
};

/**
 * Notify the room that a new member joined.
 * @param {string} roomCode
 * @param {string} nickname
 */
export const notifyJoin = (roomCode, nickname) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: `/app/room/${roomCode}/join`,
      body: JSON.stringify({ nickname }),
    });
  }
};

/**
 * Notify the room that swiping has started.
 * @param {string} roomCode
 * @param {string} memberId
 */
export const notifyStart = (roomCode, memberId) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: `/app/room/${roomCode}/start`,
      body: JSON.stringify({ memberId }),
    });
  }
};

/**
 * Disconnect the current STOMP client.
 */
export const disconnect = () => {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
};
