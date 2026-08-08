import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, PlayCircle, Calendar, Clock } from 'lucide-react';
import { listWorkspaceSessions } from '@/api/sessions';
import { getErrorMessage } from '@/api/client';
import { toast } from '@/components/ui/Toast';
import { Spinner, Button } from '@/components/ui/primitives';
import type { RecordingSession } from '@/types/api';

interface Props {
  workspaceId: string;
  onClose: () => void;
}

export function WorkspaceHistoryModal({ workspaceId, onClose }: Props) {
  const [sessions, setSessions] = useState<RecordingSession[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    listWorkspaceSessions(workspaceId)
      .then(setSessions)
      .catch((err) => toast.error(getErrorMessage(err, 'Could not load history')))
      .finally(() => setLoading(false));
  }, [workspaceId]);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ width: '500px' }}>
        <div className="modal-header">
          <h2 className="glow-text">Playback History</h2>
          <button className="icon-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ maxHeight: '400px', overflowY: 'auto' }}>
          {loading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}>
              <Spinner size={24} />
            </div>
          ) : sessions.length === 0 ? (
            <p className="chat-empty-hint" style={{ textAlign: 'center', padding: '2rem' }}>
              No recorded sessions found for this workspace.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {sessions.map((session) => (
                <div key={session.id} style={{ 
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center', 
                  padding: '1rem', backgroundColor: 'var(--color-bg-base)', 
                  border: '1px solid var(--color-border)', borderRadius: '6px' 
                }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--color-ink)', fontSize: '0.9rem' }}>
                      <Calendar size={14} color="var(--color-primary)" />
                      {new Date(session.startedAt).toLocaleString()}
                    </div>
                    {session.durationSeconds !== null && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--color-ink-dim)', fontSize: '0.8rem' }}>
                        <Clock size={12} />
                        Duration: {Math.floor(session.durationSeconds / 60)}m {session.durationSeconds % 60}s
                      </div>
                    )}
                  </div>
                  <Button onClick={() => navigate(`/dashboard/session/${session.id}/playback`)}>
                    <PlayCircle size={14} /> Watch
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}