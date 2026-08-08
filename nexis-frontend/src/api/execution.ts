import { apiClient } from './client';
import type { RunCodeRequest, RunCodeResponse } from '@/types/api';

export async function runCode(payload: RunCodeRequest): Promise<RunCodeResponse> {
  const { data } = await apiClient.post<RunCodeResponse>('/execute/run', payload);
  return data;
}

export async function killJob(jobId: string): Promise<void> {
  await apiClient.post(`/execute/kill/${jobId}`);
}

export async function getJobStatus(jobId: string): Promise<RunCodeResponse> {
  const { data } = await apiClient.get<RunCodeResponse>(`/execute/status/${jobId}`);
  return data;
}