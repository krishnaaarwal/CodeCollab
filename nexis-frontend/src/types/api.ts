/**
 * Shared API contracts, mirrored from the Nexis backend docs.
 *
 * Anything marked "ASSUMED" is not in the documented contract — the frontend
 * needs it, so a reasonable shape was chosen. Flag these in code review
 * against the real backend and adjust here (this file is the only place
 * that should need to change).
 */

// ---------------- Auth ----------------

export interface SignupRequest {
  email: string;
  password: string;
  fullname: string;
}

export interface SignupResponse {
  id: string;
  email: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: string;
  email: string;
  accessToken: string;
  refreshToken: string;
}

export interface UserProfile {
  id: string;
  fullname: string;
  email: string;
  avatar: string;
}

// ---------------- Workspace ----------------

export type WorkspaceVisibility = 'PRIVATE' | 'PUBLIC';
export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export interface Workspace {
  id: string;
  ownerId: string;
  name: string;
  description: string;
  visibility: WorkspaceVisibility;
}

export interface CreateWorkspaceRequest {
  name: string;
  description: string;
  visibility: WorkspaceVisibility;
}

export interface WorkspaceMember {
  userId: string;
  fullname: string;
  email: string;
  role: WorkspaceRole;
}

// ---------------- Supported languages ----------------

export type SupportedLanguage = 'JAVA' | 'PYTHON' | 'JAVASCRIPT' | 'CPP' | 'DART';

// ---------------- Storage (MinIO 3-phase upload) ----------------

export interface UploadIntentRequest {
  workspaceId: string;
  fileName: string;
  size: number;
}

export interface UploadIntentResponse {
  fileId: string;
  url: string;
}

export interface UploadCompleteRequest {
  workspaceId: string;
  fileId: string;
  versionNum: number;
  fileName: string;
  sizeBytes: number;
}

/**
 * Confirmed from a real backend log's SQL projection (2026-07-04), not a
 * guess: `select id, created_at, current_version, file_name, file_size,
 * file_type, storage_key, updated_at, workspace_id from files ...`.
 * The endpoint path itself is still unconfirmed — see README.
 */
export interface WorkspaceFileMeta {
  id: string;
  workspaceId: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  storageKey: string;
  currentVersion: number;
  createdAt: string;
  updatedAt: string;
}

// ---------------- Execution ----------------

export type ExecutionStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface RunCodeRequest {
  userId: string;
  workspaceId: string;
  codeLanguage: SupportedLanguage;
  code: string;
}

export interface RunCodeResponse {
  id: string;
  status: ExecutionStatus;
  output: string | null;
  error: string | null;
  executionDurationMs: number | null;
}

/** Pushed over /topic/workspace/{id}/terminal when a job finishes */
export interface ExecutionResultPayload {
  jobId: string;
  userId: string;
  workspaceId: string;
  status: ExecutionStatus;
  output: string | null;
  error: string | null;
}

// ---------------- Collaboration WebSocket ----------------

export type OperationType = 'INSERT' | 'DELETE' | 'RETAIN';

export interface CodeOperation {
  version: number;
  userId: string;
  operationType: OperationType;
  position: number;
  code: string;
  length: number;
  language: string;
}

export interface CursorPayload {
  userId: string;
  line: number;
  characterIndex: number;
}

export interface ChatMessage {
  userId: string;
  workspaceId: string;
  time: string;
  message: string;
}

/**
 * Confirmed from the actual backend (PrivateMessagePayload.java / WebsocketController.java):
 * publish to `/app/private` (not workspace-scoped), receive on `/user/queue/private` (not
 * `/user/queue/chat`). No `time` or `workspaceId` field, and different field names
 * (senderId/receiverId/content) than group ChatMessage. `messageType` also carries
 * ERROR/INVITE over this same channel — only DIRECT_MESSAGE is chat.
 *
 * This is the RAW wire shape. `useWorkspaceSocket` maps it to `PrivateChatMessage` below
 * (matching ChatMessage's field names) right at the subscription boundary, so the rest of
 * the app — ChatPanel, WorkspacePage — only ever deals with one consistent message shape
 * regardless of whether it came from the group or private channel.
 */
export type PrivateMessageType = 'ERROR' | 'INVITE' | 'DIRECT_MESSAGE';

export interface PrivateMessagePayload {
  senderId: string;
  receiverId: string;
  messageType: PrivateMessageType;
  content: string;
}

export interface PrivateChatMessage extends ChatMessage {
  recipientId: string;
}

export interface TypingPayload {
  userId: string;
  isTyping: boolean;
}

export type PresenceType = 'JOINED' | 'LEFT';

export interface PresencePayload {
  userId: string;
  presenceType: PresenceType;
}

export interface WebsocketErrorPayload {
  type: string;
  message: string;
  timestamp: string;
}

// ---------------- Recording / Time Machine ----------------

export interface RecordingSession {
  id: string;
  workspaceId: string;
  startedAt: string;
  endedAt: string | null;
  durationSeconds: number | null;
}

export type SessionEventType =
  | 'CODE_CHANGE'
  | 'CURSOR_MOVE'
  | 'USER_JOINED'
  | 'USER_LEFT'
  | 'CHAT_MESSAGE'
  | 'FILE_CREATED'
  | 'EXECUTION_STARTED';

export interface SessionEvent {
  id: number;
  eventType: SessionEventType;
  userId: string;
  payload: Record<string, unknown>;
  timestamp: string;
}

// ---------------- Generic API error shape ----------------

export interface ApiErrorBody {
  error?: string;
  message?: string;
  timestamp?: string;
  path?: string;
}
