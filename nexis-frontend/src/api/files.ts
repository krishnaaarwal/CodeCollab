import { apiClient } from './client';
import type { WorkspaceFileMeta } from '@/types/api';

// Add these imports at the top
import { useAuthStore } from '@/store/authStore';

// Add this helper if it isn't in files.ts already
function requireUserId(): string {
  const userId = useAuthStore.getState().user?.id;
  if (!userId) throw new Error('Not authenticated');
  return userId;
}

export async function renameFile(fileId: string, workspaceId: string, newName: string): Promise<void> {
  await apiClient.patch(
    `/files/${fileId}/rename`, // <--- Removed /api
    { workspaceId, newName },
    { headers: { 'X-User-Id': requireUserId() } }
  );
}

export async function deleteFile(fileId: string, workspaceId: string): Promise<void> {
  await apiClient.delete(`/files/${fileId}`, { // <--- Removed /api
    params: { workspaceId },
    headers: { 'X-User-Id': requireUserId() },
  });
}

function normalizeFileMeta(raw: unknown): WorkspaceFileMeta {
  const record = raw as Record<string, unknown>;
  const id = record.id ?? record.fileId;
  if (typeof id !== 'string') {
    // eslint-disable-next-line no-console
    console.error('[Nexis] File entry has no usable id (checked "id" and "fileId"):', raw);
  }
  return { ...(record as object), id: typeof id === 'string' ? id : '' } as WorkspaceFileMeta;
}

export async function listWorkspaceFiles(workspaceId: string): Promise<WorkspaceFileMeta[]> {
  const { data } = await apiClient.get<unknown>(`/files/workspace/${workspaceId}`); //

  if (Array.isArray(data)) {
    return data.map(normalizeFileMeta);
  }

  console.error('[Nexis] Expected an array from /api/files/workspace/{id}, got:', data);

  if (data && typeof data === 'object') {
    const record = data as Record<string, unknown>;
    if (Array.isArray(record.content)) return record.content.map(normalizeFileMeta);
    if (Array.isArray(record.files)) return record.files.map(normalizeFileMeta);
  }

  throw new Error('Unexpected response shape from file list endpoint — see console for the raw body');
}


export async function fetchFileContent(fileId: string): Promise<string> {
  if (!fileId) {
    throw new Error('fetchFileContent called with an empty file id');
  }
  const { data } = await apiClient.get<{ url: string }>(`/files/${fileId}/download`); // <--- Removed /api
  const res = await fetch(data.url);
  if (!res.ok) throw new Error(`Failed to fetch file content (${res.status})`);
  return res.text();
}
