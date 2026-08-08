import { apiClient } from './client';
import { useAuthStore } from '@/store/authStore';
import { getBoilerplate } from '@/languages/boilerplates';
import type { SupportedLanguage, UploadIntentResponse } from '@/types/api';

function requireUserId(): string {
  const userId = useAuthStore.getState().user?.id;
  if (!userId) throw new Error('Not authenticated');
  return userId;
}

async function uploadBinaryToMinio(url: string, content: string): Promise<void> {
  const blob = new Blob([content]);
  const response = await fetch(url, { method: 'PUT', body: blob });
  if (!response.ok) {
    throw new Error(`MinIO upload failed with status ${response.status}`);
  }
}

async function requestUploadIntent(
  workspaceId: string,
  fileName: string,
  size: number
): Promise<UploadIntentResponse> {
  const { data } = await apiClient.post<UploadIntentResponse>(
    '/files/upload',
    { workspaceId, fileName, size },
    { headers: { 'X-User-Id': requireUserId() } }
  );
  return data;
}

async function commitUpload(params: {
  workspaceId: string;
  fileId: string;
  fileName: string;
  sizeBytes: number;
  versionNum: number;
}): Promise<void> {
  await apiClient.post(
    '/files/upload-complete',
    {
      workspaceId: params.workspaceId,
      fileId: params.fileId,
      versionNum: params.versionNum,
      fileName: params.fileName,
      sizeBytes: params.sizeBytes,
    },
    { headers: { 'X-User-Id': requireUserId() } }
  );
}

export async function uploadFile(
  workspaceId: string,
  fileName: string,
  content: string,
  expectedVersionNum: number = 1
): Promise<{ versionNum: number }> {
  const sizeBytes = new TextEncoder().encode(content).byteLength;
  const intent = await requestUploadIntent(workspaceId, fileName, sizeBytes);
  await uploadBinaryToMinio(intent.url, content);
  const raw = intent as unknown as Record<string, unknown>;
  const serverVersion = raw.version ?? raw.versionNum ?? raw.currentVersion;
  const versionNum = typeof serverVersion === 'number' ? serverVersion : expectedVersionNum;
  await commitUpload({ workspaceId, fileId: intent.fileId, fileName, sizeBytes, versionNum });
  return { versionNum };
}

export async function seedWorkspaceWithBoilerplate(
  workspaceId: string,
  language: SupportedLanguage
): Promise<void> {
  const boilerplate = getBoilerplate(language);
  await uploadFile(workspaceId, boilerplate.fileName, boilerplate.content);
}