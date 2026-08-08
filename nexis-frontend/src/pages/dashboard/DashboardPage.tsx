import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Lock, Globe, FolderCode, History } from 'lucide-react';
import { listWorkspaces } from '@/api/workspaces';
import { getErrorMessage } from '@/api/client';
import { toast } from '@/components/ui/Toast';
import { Button, Spinner, Badge } from '@/components/ui/primitives';
import { Navbar } from '@/components/layout/Navbar';
import { CreateWorkspaceModal } from './CreateWorkspaceModal';
import { WorkspaceHistoryModal } from './WorkspaceHistoryModal';
import type { Workspace } from '@/types/api';

export function DashboardPage() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [historyWorkspaceId, setHistoryWorkspaceId] = useState<string | null>(null);
  const navigate = useNavigate();

  // ... (Keep useEffect and functions exactly the same) ...
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await listWorkspaces();
        if (!cancelled) setWorkspaces(data);
      } catch (err) {
        if (!cancelled) toast.error(getErrorMessage(err, 'Could not load workspaces'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  function handleCreated(workspace: Workspace) { setWorkspaces((prev) => [workspace, ...prev]); }
  function openWorkspace(workspace: Workspace) { navigate(`/workspace/${workspace.id}`, { state: { workspace } }); }

  return (
    <div className="min-h-screen bg-[#09090b]">
      <Navbar />
      <div className="mx-auto max-w-6xl px-6 py-10">
        
        {/* Header */}
        <div className="mb-10 flex flex-wrap items-end justify-between gap-4 border-b border-zinc-800 pb-6">
          <div>
            <h1 className="text-2xl font-semibold text-zinc-100 tracking-tight">Workspaces</h1>
            <p className="mt-1 text-[14px] text-zinc-400">Spin one up and start pairing in real time.</p>
          </div>
          <Button onClick={() => setModalOpen(true)}>
            <Plus size={16} /> New Workspace
          </Button>
        </div>

        {/* Content */}
        {loading ? (
          <div className="flex justify-center py-20"><Spinner size={28} /></div>
        ) : workspaces.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-zinc-800 bg-[#18181b] py-24 text-center">
            <FolderCode size={32} className="mb-4 text-zinc-600" />
            <div className="mb-2 text-lg font-medium text-zinc-200">No workspaces yet</div>
            <p className="mb-6 max-w-sm text-[13px] text-zinc-500">Create a new workspace, add your files, and start collaborating in real time.</p>
            <Button onClick={() => setModalOpen(true)}>
              <Plus size={16} /> New Workspace
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
            {workspaces.map((ws) => (
              <div 
                key={ws.id} 
                className="group flex flex-col rounded-xl border border-zinc-800 bg-[#18181b] p-5 transition-all hover:-translate-y-1 hover:border-zinc-600 hover:shadow-xl hover:shadow-black/20 cursor-pointer"
                onClick={() => openWorkspace(ws)}
              >
                <div className="mb-4 flex items-start justify-between">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400">
                    <FolderCode size={20} />
                  </div>
                </div>
                <div className="mb-1 text-base font-medium text-zinc-100">{ws.name}</div>
                <div className="mb-6 line-clamp-2 text-[13px] text-zinc-500 min-h-[40px]">
                  {ws.description || 'No description provided.'}
                </div>
                
                <div className="mt-auto flex items-center justify-between border-t border-zinc-800/50 pt-4">
                  <Badge tone={ws.visibility === 'PRIVATE' ? 'default' : 'success'}>
                    {ws.visibility === 'PRIVATE' ? <Lock size={12} /> : <Globe size={12} />}
                    {(ws.visibility || 'unknown').toLowerCase()}
                  </Badge>
                  
                  <button 
                    className="rounded p-1.5 text-zinc-500 opacity-0 transition-all hover:bg-zinc-800 hover:text-zinc-200 group-hover:opacity-100"
                    title="View History" 
                    onClick={(e) => {
                      e.stopPropagation();
                      setHistoryWorkspaceId(ws.id);
                    }}
                  >
                    <History size={15} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {modalOpen && <CreateWorkspaceModal onClose={() => setModalOpen(false)} onCreated={handleCreated} />}
      {historyWorkspaceId && <WorkspaceHistoryModal workspaceId={historyWorkspaceId} onClose={() => setHistoryWorkspaceId(null)} />}
    </div>
  );
}