import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal } from '@/components/ui/Modal';
import { Button, Input, Select } from '@/components/ui/primitives';
import { toast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/api/client';
import { createWorkspace } from '@/api/workspaces';
import type { Workspace, WorkspaceVisibility } from '@/types/api';

interface Props {
  onClose: () => void;
  onCreated: (workspace: Workspace) => void;
}

export function CreateWorkspaceModal({ onClose, onCreated }: Props) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [visibility, setVisibility] = useState<WorkspaceVisibility>('PRIVATE');
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) { 
      toast.error('Give your workspace a name'); 
      return; 
    }
    
    setBusy(true);
    try {
      const response = await createWorkspace({ 
        name: name.trim(), 
        description: description.trim(), 
        visibility 
      });
      
      // Defensively unwrap the response in case your API client returns the raw Axios object
      const newWorkspace = (response as any).data || response;

      toast.success('Workspace created');
      
      // Update the dashboard state instantly
      onCreated(newWorkspace);
      
      // Force the modal to close immediately
      onClose(); 
      
      // Only navigate if we successfully extracted a valid ID
      if (newWorkspace?.id) {
        navigate(`/workspace/${newWorkspace.id}`, { state: { workspace: newWorkspace } });
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not create workspace'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="New Workspace" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input 
          label="Workspace Name" 
          value={name} 
          onChange={(e) => setName(e.target.value)} 
          required 
          autoFocus 
          disabled={busy} 
        />
        <Input 
          label="Description (optional)" 
          value={description} 
          onChange={(e) => setDescription(e.target.value)} 
          disabled={busy} 
        />
        <Select
          label="Visibility"
          value={visibility}
          onChange={(e) => setVisibility(e.target.value as WorkspaceVisibility)}
          disabled={busy}
          options={[
            { value: 'PRIVATE', label: 'Private' }, 
            { value: 'PUBLIC', label: 'Public' }
          ]}
        />

        <Button type="submit" loading={busy} className="mt-4 w-full">
          {busy ? 'Creating...' : 'Create Workspace'}
        </Button>
      </form>
    </Modal>
  );
}