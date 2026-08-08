import { useState, type FormEvent } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Button, Input } from '@/components/ui/primitives';
import { toast } from '@/components/ui/Toast';
import { addWorkspaceMember } from '@/api/workspaces';
import { getErrorMessage } from '@/api/client';

interface Props {
  workspaceId: string;
  onClose: () => void;
}

export function InviteMemberModal({ workspaceId, onClose }: Props) {
  const [memberId, setMemberId] = useState('');
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!memberId.trim()) return;
    setBusy(true);
    try {
      await addWorkspaceMember(workspaceId, memberId.trim());
      toast.success('Member added successfully');
      onClose();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to add member. Check ID.'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Invite Member" onClose={onClose}>
      <form onSubmit={handleSubmit} className="auth-form">
        <Input
          label="User ID (UUID)"
          value={memberId}
          onChange={(e) => setMemberId(e.target.value)}
          placeholder="e.g. 123e4567-e89b-12d3..."
          required
          disabled={busy}
        />
        <Button type="submit" loading={busy} style={{ width: '100%', marginTop: '1rem' }}>
          Grant Access
        </Button>
      </form>
    </Modal>
  );
}