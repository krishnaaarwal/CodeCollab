import { apiClient } from './client';
import type { CreateWorkspaceRequest, Workspace, WorkspaceMember } from '@/types/api';

export async function listWorkspaces(): Promise<Workspace[]> {
  const { data } = await apiClient.get<Workspace[]>('/workspaces');
  return data;
}

// Inside src/api/workspaces.ts

export async function createWorkspace(payload: { name: string; description?: string; visibility: string }) {
  const response = await apiClient.post('/api/workspaces', payload);

  return response.data; 
}

export async function updateWorkspace(
  id: string,
  payload: CreateWorkspaceRequest
): Promise<Workspace> {
  const { data } = await apiClient.put<Workspace>(`/workspaces/${id}`, payload);
  return data;
}

export async function getWorkspaceMembers(id: string): Promise<WorkspaceMember[]> {
  const { data } = await apiClient.get<WorkspaceMember[]>(`/workspaces/${id}/members`);
  return data;
}

export async function addWorkspaceMember(workspaceId: string, memberId: string): Promise<Workspace> {
  const { data } = await apiClient.post<Workspace>(
    `/workspaces/${workspaceId}/members`,
    null,
    { params: { memberId } }
  );
  return data;
}

export async function removeWorkspaceMember(workspaceId: string, memberId: string): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/members/${memberId}`);
}

export async function transferOwnership(workspaceId: string, newOwnerId: string): Promise<Workspace> {
  const { data } = await apiClient.put<Workspace>(
    `/workspaces/${workspaceId}/transfer-ownership`,
    null,
    { params: { newOwnerId } }
  );
  return data;
}