import { useCallback, useEffect, useRef, useState } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WS_URL } from '@/lib/config';
import { useAuthStore } from '@/store/authStore';
import type {
  ChatMessage,
  CodeOperation,
  CursorPayload,
  ExecutionResultPayload,
  PresencePayload,
  PrivateChatMessage,
  TypingPayload,
  WebsocketErrorPayload,
} from '@/types/api';

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected';

interface Handlers {
  onRemoteOperation: (op: CodeOperation) => void;
  onCursor: (cursor: CursorPayload) => void;
  onPresenceChange: (payload: PresencePayload) => void;
  onExecutionResult: (result: ExecutionResultPayload) => void;
  onWsError?: (err: WebsocketErrorPayload) => void;
  onChatMessage?: (message: ChatMessage) => void;
  onPrivateMessage?: (message: PrivateChatMessage) => void;
  onTyping?: (payload: TypingPayload) => void;
  onFileEvent?: (payload: { eventType: string; fileId: string; newName?: string }) => void;
}

const CURSOR_THROTTLE_MS = 100;

/**
 * Incoming events are delivered via direct callback invocation, not React
 * state — a stream of operations arriving in the same tick must never be
 * coalesced into "just the latest one", which is exactly what would
 * happen if this were exposed as `latestOperation` state.
 */
export function useWorkspaceSocket(workspaceId: string, handlers: Handlers) {
  const [status, setStatus] = useState<ConnectionStatus>('connecting');
  const clientRef = useRef<Client | null>(null);
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  const lastCursorSentAt = useRef(0);
  const cursorTimeoutRef = useRef<number | null>(null);

  useEffect(() => {
    const token = useAuthStore.getState().accessToken;
    const currentUserId = useAuthStore.getState().user?.id;

    setStatus('connecting');

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      setStatus('connected');

      client.subscribe(`/topic/workspace/${workspaceId}/code`, (message: IMessage) => {
        handlersRef.current.onRemoteOperation(JSON.parse(message.body) as CodeOperation);
      });
      client.subscribe(`/topic/workspace/${workspaceId}/cursor`, (message: IMessage) => {
        handlersRef.current.onCursor(JSON.parse(message.body) as CursorPayload);
      });
      client.subscribe(`/topic/workspace/${workspaceId}/presence`, (message: IMessage) => {
        handlersRef.current.onPresenceChange(JSON.parse(message.body) as PresencePayload);
      });
      client.subscribe(`/topic/workspace/${workspaceId}/terminal`, (message: IMessage) => {
        handlersRef.current.onExecutionResult(JSON.parse(message.body) as ExecutionResultPayload);
      });
      client.subscribe('/user/queue/errors', (message: IMessage) => {
        handlersRef.current.onWsError?.(JSON.parse(message.body) as WebsocketErrorPayload);
      });
      client.subscribe(`/topic/workspace/${workspaceId}/chat`, (message: IMessage) => {
        handlersRef.current.onChatMessage?.(JSON.parse(message.body) as ChatMessage);
      });
      client.subscribe(`/topic/workspace/${workspaceId}/typing`, (message: IMessage) => {
        handlersRef.current.onTyping?.(JSON.parse(message.body) as TypingPayload);
      });
     client.subscribe(`/topic/workspace/${workspaceId}/file`, (message: IMessage) => {
    handlersRef.current.onFileEvent?.(JSON.parse(message.body));
  });
      
      if (currentUserId) {
        client.subscribe(`/topic/user/${currentUserId}/private`, (message: IMessage) => { 
          let raw;
          try {
            raw = JSON.parse(message.body);
            // Defensive: if Spring double-serialized the JSON, parse it again
            if (typeof raw === 'string') {
              raw = JSON.parse(raw);
            }
          } catch (e) {
            console.error('[Nexis] Failed to parse incoming DM', e);
            return;
          }

          // messageType also carries ERROR/INVITE over this same channel;
          // only DIRECT_MESSAGE is chat.
          if (raw.messageType && raw.messageType !== 'DIRECT_MESSAGE') return;

          const mappedMessage: PrivateChatMessage = {
            userId: raw.senderId,
            recipientId: raw.receiverId,
            message: raw.content,
            workspaceId: workspaceId,
            time: new Date().toISOString(),
          };

          handlersRef.current.onPrivateMessage?.(mappedMessage);
        });
      }
    };

    client.onWebSocketClose = () => setStatus('disconnected');
    client.onStompError = () => setStatus('disconnected');

    client.activate();
    clientRef.current = client;

    return () => {
      if (cursorTimeoutRef.current) {
        window.clearTimeout(cursorTimeoutRef.current);
        cursorTimeoutRef.current = null;
      }
      client.deactivate();
      clientRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId]);

  const sendOperation = useCallback(
    (op: Omit<CodeOperation, 'userId'>) => {
      const client = clientRef.current;
      const userId = useAuthStore.getState().user?.id;
      if (!client || !client.connected || !userId) return;
      client.publish({
        destination: `/app/workspace/${workspaceId}/code`,
        body: JSON.stringify({ ...op, userId }),
      });
    },
    [workspaceId]
  );

  const sendCursor = useCallback(
    (line: number, characterIndex: number) => {
      const client = clientRef.current;
      const userId = useAuthStore.getState().user?.id;
      if (!client || !client.connected || !userId) return;

      const publish = () => {
        lastCursorSentAt.current = Date.now();
        client.publish({
          destination: `/app/workspace/${workspaceId}/cursor`,
          body: JSON.stringify({ userId, line, characterIndex }),
        });
      };

      const elapsed = Date.now() - lastCursorSentAt.current;
      if (elapsed >= CURSOR_THROTTLE_MS) {
        publish();
      } else {
        if (cursorTimeoutRef.current) window.clearTimeout(cursorTimeoutRef.current);
        cursorTimeoutRef.current = window.setTimeout(publish, CURSOR_THROTTLE_MS - elapsed);
      }
    },
    [workspaceId]
  );

  const sendChatMessage = useCallback(
    (message: string) => {
      const client = clientRef.current;
      const userId = useAuthStore.getState().user?.id;
      if (!client || !client.connected || !userId) return;
      // `time` is deliberately omitted — the server appends server-time
      // on receipt (WebsocketController.handleChat), so a client-set
      // timestamp would just be overwritten.
      client.publish({
        destination: `/app/workspace/${workspaceId}/chat`,
        body: JSON.stringify({ userId, workspaceId, message }),
      });
    },
    [workspaceId]
  );

  const sendPrivateMessage = useCallback(
    (recipientId: string, message: string) => {
      const client = clientRef.current;
      const userId = useAuthStore.getState().user?.id;
      if (!client || !client.connected || !userId) return;

      client.publish({
        destination: `/app/private`,
        body: JSON.stringify({
          senderId: userId, // Backend expects senderId
          receiverId: recipientId, // Backend expects receiverId
          content: message, // Backend expects content
          messageType: 'DIRECT_MESSAGE',
        }),
      });
    },
    [workspaceId]
  );

  const sendTyping = useCallback(
    (isTyping: boolean) => {
      const client = clientRef.current;
      const userId = useAuthStore.getState().user?.id;
      if (!client || !client.connected || !userId) return;
      client.publish({
        destination: `/app/workspace/${workspaceId}/typing`,
        body: JSON.stringify({ userId, isTyping }),
      });
    },
    [workspaceId]
  );

  return { status, sendOperation, sendCursor, sendChatMessage, sendPrivateMessage, sendTyping };
}
