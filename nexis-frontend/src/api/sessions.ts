import { apiClient } from './client';
import type { RecordingSession, SessionEvent } from '@/types/api';

export async function startSession(workspaceId: string, participants: string[]): Promise<RecordingSession> {
  const { data } = await apiClient.post<RecordingSession>('/sessions/start', {
    workspaceId,
    participants,
  });
  return data;
}

export async function endSession(sessionId: string): Promise<RecordingSession> {
  const { data } = await apiClient.post<RecordingSession>(`/sessions/${sessionId}/end`);
  return data;
}

export async function getSession(sessionId: string): Promise<RecordingSession> {
  const { data } = await apiClient.get<RecordingSession>(`/sessions/${sessionId}`);
  return data;
}

export async function getSessionEvents(sessionId: string): Promise<SessionEvent[]> {
  const { data } = await apiClient.get(`/sessions/${sessionId}/events`, {
    responseType: 'text',
  });

  if (typeof data !== 'string') return data;

  return data
    .split('\n')
    .filter((line) => line.trim().length > 0)
    .map((line) => JSON.parse(line));
}

export async function listWorkspaceSessions(workspaceId: string): Promise<RecordingSession[]> {
  const { data } = await apiClient.get<RecordingSession[]>(`/sessions/workspace/${workspaceId}`);
  return data;
}