import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, LogOut, MessageSquare, ShieldOff, X } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { logoutRequest } from '@/api/auth';
import { getWorkspaceMembers } from '@/api/workspaces';
import { listWorkspaceFiles, fetchFileContent } from '@/api/files';
import { uploadFile } from '@/api/storage';
import { runCode, killJob, getJobStatus } from '@/api/execution';
import { startSession, endSession } from '@/api/sessions';
import { getErrorMessage, getErrorStatus } from '@/api/client';
import { refreshTokenStorage } from '@/lib/refreshTokenStorage';
import { detectLanguageFromFileName } from '@/languages/boilerplates';
import { useWorkspaceSocket } from '@/hooks/useWorkspaceSocket';
import { toast } from '@/components/ui/Toast';
import { Button, Spinner } from '@/components/ui/primitives';
import { FileTree } from '@/components/ide/FileTree';
import { EditorPane, type EditorPaneHandle } from '@/components/ide/EditorPane';
import { PresenceBar } from '@/components/ide/PresenceBar';
import { Terminal, type TerminalLine } from '@/components/ide/Terminal';
import { ChatPanel, threadKey, type ChatThread } from '@/components/ide/ChatPanel';
import { UserPlus } from 'lucide-react';
import { InviteMemberModal } from '@/components/ide/InviteMemberModal';

import type {
  ChatMessage,
  ExecutionResultPayload,
  PrivateChatMessage,
  Workspace,
  WorkspaceFileMeta,
  WorkspaceMember,
} from '@/types/api';

interface LocationState {
  workspace?: Workspace;
}

/**
 * The docs' example WS terminal payload uses `jobId`, but the file-tree
 * endpoint turned out to use different field names than documented too —
 * so this checks both `jobId` and a plain `id`, whichever the backend
 * actually sends, instead of assuming the doc's naming is exact.
 */
function extractJobId(payload: ExecutionResultPayload): string | undefined {
  const record = payload as unknown as Record<string, unknown>;
  const candidate = record.jobId ?? record.id;
  return typeof candidate === 'string' ? candidate : undefined;
}

export function WorkspacePage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);

  const [workspace] = useState<Workspace | null>((location.state as LocationState | null)?.workspace ?? null);
  const [members, setMembers] = useState<Map<string, WorkspaceMember>>(new Map());
  const [presence, setPresence] = useState<Set<string>>(new Set());

  const [files, setFiles] = useState<WorkspaceFileMeta[]>([]);
  const [filesLoading, setFilesLoading] = useState(true);
  const [filesError, setFilesError] = useState<string | null>(null);
  const [accessDenied, setAccessDenied] = useState(false);
  const [activeFile, setActiveFile] = useState<WorkspaceFileMeta | null>(null);
  const [openTabs, setOpenTabs] = useState<WorkspaceFileMeta[]>([]);
  const [activeContent, setActiveContent] = useState('');
  const [contentLoading, setContentLoading] = useState(false);
  const [contentLoadError, setContentLoadError] = useState<string | null>(null);
  const [inviteModalOpen, setInviteModalOpen] = useState(false);

  const [terminalLines, setTerminalLines] = useState<TerminalLine[]>([]);
  const [isRunning, setIsRunning] = useState(false);
  const [terminalExpanded, setTerminalExpanded] = useState(true);

  const [chatOpen, setChatOpen] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'unsaved' | 'saving' | 'saved' | 'error'>('idle');
  const isSavingRef = useRef(false);
  const [activeThread, setActiveThread] = useState<ChatThread>({ kind: 'group' });
  const [groupMessages, setGroupMessages] = useState<ChatMessage[]>([]);
  const [privateThreads, setPrivateThreads] = useState<Map<string, PrivateChatMessage[]>>(new Map());
  const [typingUserIds, setTypingUserIds] = useState<Set<string>>(new Set());
  const [unreadThreads, setUnreadThreads] = useState<Set<string>>(new Set());

  const editorRef = useRef<EditorPaneHandle>(null);
  const activeJobIdRef = useRef<string | null>(null);
  const runTimeoutRef = useRef<number | null>(null);
  const sessionIdRef = useRef<string | null>(null);
  const membersRef = useRef(members);
  membersRef.current = members;
  const presenceRef = useRef(presence);
  presenceRef.current = presence;
  const chatOpenRef = useRef(chatOpen);
  chatOpenRef.current = chatOpen;
  const activeThreadRef = useRef(activeThread);
  activeThreadRef.current = activeThread;
  // Fingerprints of messages this client just sent optimistically, so the
  // server's own broadcast echo (if the backend includes the sender, as it
  // does for code ops/presence elsewhere in this app) doesn't double them up.
  const pendingOwnMessagesRef = useRef<Map<string, number>>(new Map());
  const typingClearTimersRef = useRef<Map<string, number>>(new Map());
  // Find the current user's role in the workspace
  const currentUserRole = user?.id ? members.get(user.id)?.role : null;
  const canInvite = currentUserRole === 'OWNER' || currentUserRole === 'ADMIN';

  const socket = useWorkspaceSocket(workspaceId ?? '', {
    onRemoteOperation: (op) => editorRef.current?.applyRemoteOperation(op),
    onCursor: (cursor) => {
      if (cursor.userId === user?.id) return;
      const name = membersRef.current.get(cursor.userId)?.fullname || 'anon';
      editorRef.current?.updateRemoteCursor(cursor.userId, name, cursor);
    },
    onPresenceChange: (p) => {
      setPresence((prev) => {
        const next = new Set(prev);
        if (p.presenceType === 'JOINED') {
          next.add(p.userId);
        } else {
          next.delete(p.userId);
          editorRef.current?.removeRemoteCursor(p.userId);
        }
        return next;
      });
    },
    onExecutionResult: (result) => {
      // eslint-disable-next-line no-console
      console.log('[Nexis] Terminal message received:', result);
      const incomingId = extractJobId(result);
      if (incomingId === undefined) {
        // eslint-disable-next-line no-console
        console.warn(
          '[Nexis] Could not find a jobId/id field on the terminal payload — showing it anyway since dropping it would silently hide real output. Check the logged payload above against extractJobId().'
        );
      } else if (incomingId !== activeJobIdRef.current) {
        return; // confirmed to belong to a different job — ignore
      }
      if (runTimeoutRef.current) {
        window.clearTimeout(runTimeoutRef.current);
        runTimeoutRef.current = null;
      }
      setIsRunning(false);
      if (result.status === 'COMPLETED') {
        setTerminalLines((prev) => [
          ...prev,
          { text: result.output || '(no output)', kind: 'output' },
          { text: 'Process finished.', kind: 'system' },
        ]);
      } else if (result.status === 'FAILED') {
        setTerminalLines((prev) => [...prev, { text: result.error || 'Execution failed.', kind: 'error' }]);
      }
    },
    onChatMessage: (message) => {
      if (message.userId === user?.id) {
        const fp = `group::${message.message}`;
        const expiry = pendingOwnMessagesRef.current.get(fp);
        if (expiry && expiry > Date.now()) {
          pendingOwnMessagesRef.current.delete(fp);
          return; // already shown optimistically when we sent it
        }
      }
      setGroupMessages((prev) => [...prev, message]);
      if (activeThreadRef.current.kind !== 'group' || !chatOpenRef.current) {
        setUnreadThreads((prev) => new Set(prev).add('group'));
      }
    },
    onPrivateMessage: (message) => {
      const other = message.userId === user?.id ? message.recipientId : message.userId;
      if (message.userId === user?.id) {
        const fp = `dm:${other}::${message.message}`;
        const expiry = pendingOwnMessagesRef.current.get(fp);
        if (expiry && expiry > Date.now()) {
          pendingOwnMessagesRef.current.delete(fp);
          return;
        }
      }
      setPrivateThreads((prev) => {
        const next = new Map(prev);
        next.set(other, [...(next.get(other) ?? []), message]);
        return next;
      });
      const key = `dm:${other}`;
      if (!(activeThreadRef.current.kind === 'dm' && activeThreadRef.current.userId === other) || !chatOpenRef.current) {
        setUnreadThreads((prev) => new Set(prev).add(key));
      }
    },
    onTyping: (payload) => {
      if (payload.userId === user?.id) return;
      setTypingUserIds((prev) => {
        const next = new Set(prev);
        if (payload.isTyping) next.add(payload.userId);
        else next.delete(payload.userId);
        return next;
      });
      const existing = typingClearTimersRef.current.get(payload.userId);
      if (existing) window.clearTimeout(existing);
      if (payload.isTyping) {
        // Auto-expire in case a "stopped typing" event is ever lost.
        const timer = window.setTimeout(() => {
          setTypingUserIds((prev) => {
            const next = new Set(prev);
            next.delete(payload.userId);
            return next;
          });
          typingClearTimersRef.current.delete(payload.userId);
        }, 3000);
        typingClearTimersRef.current.set(payload.userId, timer);
      } else {
        typingClearTimersRef.current.delete(payload.userId);
      }
    },
    onFileEvent: (event) => {
      if (event.eventType === 'FILE_DELETED') {
        setFiles((prev) => prev.filter((f) => f.id !== event.fileId));
        setOpenTabs((prev) => prev.filter((tab) => tab.id !== event.fileId));
        setActiveFile((prev) => (prev?.id === event.fileId ? null : prev));
      } else if (event.eventType === 'FILE_RENAMED') {
        setFiles((prev) => prev.map((f) => (f.id === event.fileId ? { ...f, fileName: event.newName! } : f)));
        setOpenTabs((prev) => prev.map((tab) => (tab.id === event.fileId ? { ...tab, fileName: event.newName! } : tab)));
        setActiveFile((prev) => (prev?.id === event.fileId ? { ...prev, fileName: event.newName! } : prev));
      }
    },
  });

  // Workspace members — used to resolve userId -> name for presence, cursors, chat later.
  useEffect(() => {
    if (!workspaceId) return;
    getWorkspaceMembers(workspaceId)
      .then((list) => setMembers(new Map(list.map((m) => [m.userId, m]))))
      .catch(() => {
        /* non-fatal — presence/cursors fall back to showing raw ids */
      });
  }, [workspaceId]);

  const loadFiles = useCallback(() => {
    if (!workspaceId) return;
    setFilesLoading(true);
    setFilesError(null);
    listWorkspaceFiles(workspaceId)
      .then((list) => {
        setFiles(list);
        if (list.length > 0) selectFile(list[0]);
      })
      .catch((err) => {
        if (getErrorStatus(err) === 403) {
          // 403 on the primary resource means "you don't belong here at
          // all" — a different situation from a transient/network error,
          // where a retry button in the sidebar doesn't make sense.
          setAccessDenied(true);
          return;
        }
        // Persistent, not a toast — this is exactly the class of failure
        // (an unconfirmed endpoint) where "it vanished after 5 seconds and
        // I'm not sure what it said" makes debugging much harder than it
        // needs to be. Full detail is also in the console via the client's
        // request-failure logger.
        setFilesError(getErrorMessage(err, 'Could not load files'));
      })
      .finally(() => {
        setFilesLoading(false);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId]);

  // File list, opening the first file by default.
  useEffect(() => {
    loadFiles();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId]);

  useEffect(() => {
    if (socket.status !== 'connected' || !workspaceId || !user || sessionIdRef.current) return;
    const participants = new Set(presence);
    participants.add(user.id);
    startSession(workspaceId, Array.from(participants)).then(
      (session) => {
        sessionIdRef.current = session.id;
      },
      () => {
        /* non-fatal — recording is a nice-to-have, not a blocker for collaborating */
      }
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [socket.status, workspaceId, user]);

  useEffect(() => {
    if (sessionIdRef.current && presence.size === 0) {
      const id = sessionIdRef.current;
      sessionIdRef.current = null;
      endSession(id).catch(() => {
        sessionIdRef.current = id; // couldn't confirm the end — a later drop can retry
      });
    }
  }, [presence]);

  // Best-effort fallback for the case where THIS tab is the one closing —
  // reads a ref (always current) rather than closing over a stale `presence`.
  useEffect(() => {
    return () => {
      if (sessionIdRef.current && presenceRef.current.size === 0) {
        endSession(sessionIdRef.current).catch(() => {});
      }
      if (runTimeoutRef.current) window.clearTimeout(runTimeoutRef.current);
    };
  }, []);

  // Rehydrate the terminal if the page was refreshed mid-execution (doc's documented fallback path).
  useEffect(() => {
    if (!workspaceId) return;
    const lastJobId = localStorage.getItem(`nexis:lastJob:${workspaceId}`);
    if (!lastJobId) return;
    getJobStatus(lastJobId).then(
      (job) => {
        activeJobIdRef.current = job.id;
        if (job.status === 'COMPLETED') {
          setTerminalLines([{ text: job.output || '(no output)', kind: 'output' }]);
        } else if (job.status === 'FAILED') {
          setTerminalLines([{ text: job.error || 'Execution failed.', kind: 'error' }]);
        } else {
          setIsRunning(true);
          setTerminalLines([{ text: '> Resuming previous execution...', kind: 'system' }]);
        }
      },
      () => {
        /* unknown/expired job id — ignore */
      }
    );
  }, [workspaceId]);

  useEffect(() => {
    function handler(e: BeforeUnloadEvent) {
      if (saveStatus !== 'unsaved') return;
      e.preventDefault();
      e.returnValue = '';
    }
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [saveStatus]);

  async function selectFile(file: WorkspaceFileMeta) {
    setActiveFile(file);
    setOpenTabs((prev) => (prev.some((tab) => tab.id === file.id) ? prev : [...prev, file]));
    setContentLoading(true);
    setContentLoadError(null);
    setSaveStatus('idle');
    try {
      const content = await fetchFileContent(file.id);
      setActiveContent(content.replace(/\r\n/g, '\n').replace(/\r/g, '\n'));
    } catch (err) {
      console.error('[Nexis] fetchFileContent failed for', file.fileName, err);
      const msg = getErrorMessage(err, `Could not load ${file.fileName}`);
      toast.error(msg);
      setContentLoadError(msg);
      setActiveContent('');
    } finally {
      setContentLoading(false);
    }
  }

  function closeTab(fileId: string, event?: React.MouseEvent) {
    event?.stopPropagation();
    const remainingTabs = openTabs.filter((tab) => tab.id !== fileId);
    setOpenTabs(remainingTabs);

    if (activeFile?.id === fileId) {
      const fallback = remainingTabs[remainingTabs.length - 1] ?? null;
      if (fallback) {
        void selectFile(fallback);
      } else {
        setActiveFile(null);
        setActiveContent('');
        setContentLoadError(null);
        setSaveStatus('idle');
      }
    }
  }

  function sendGroupMessage(text: string) {
    if (!user) return;
    const fp = `group::${text}`;
    pendingOwnMessagesRef.current.set(fp, Date.now() + 5000);
    setGroupMessages((prev) => [...prev, { userId: user.id, workspaceId: workspaceId ?? '', time: new Date().toISOString(), message: text }]);
    socket.sendChatMessage(text);
  }

  function sendPrivateMessageToUser(recipientId: string, text: string) {
    if (!user) return;
    const fp = `dm:${recipientId}::${text}`;
    pendingOwnMessagesRef.current.set(fp, Date.now() + 5000);
    setPrivateThreads((prev) => {
      const next = new Map(prev);
      const entry: PrivateChatMessage = {
        userId: user.id,
        workspaceId: workspaceId ?? '',
        time: new Date().toISOString(),
        message: text,
        recipientId,
      };
      next.set(recipientId, [...(next.get(recipientId) ?? []), entry]);
      return next;
    });
    socket.sendPrivateMessage(recipientId, text);
  }

  function selectChatThread(thread: ChatThread) {
    setActiveThread(thread);
    setUnreadThreads((prev) => {
      const next = new Set(prev);
      next.delete(threadKey(thread));
      return next;
    });
  }

  function openChat() {
    setChatOpen(true);
    setUnreadThreads((prev) => {
      const next = new Set(prev);
      next.delete(threadKey(activeThreadRef.current));
      return next;
    });
  }

  function handleFileCreated(file: WorkspaceFileMeta) {
    setFiles((prev) => [...prev, file]);
    selectFile(file);
  }

  // Checkpoints the live Monaco buffer to MinIO via the existing 3-phase
  // upload flow (see storage.ts). Manual-only now (Ctrl/Cmd+S) — auto-save
  // was removed; see README for why. Confirmed against real backend logs:
  // the backend DOES recognize a repeat upload to the same workspace+fileName
  // as a new version of the SAME file (it looked one up by
  // workspace_id+file_name and issued Version 2), so this was never a
  // duplicate-file risk — the actual bug was that every commit was sending
  // versionNum 1 regardless, which the backend's idempotency check rejects
  // once version 1 has already been committed once. Fixed by tracking
  // `currentVersion` on the file itself and always saving as `currentVersion
  // + 1`.
  async function handleSave() {
    if (!activeFile || !workspaceId || isSavingRef.current) return;
    isSavingRef.current = true;
    setSaveStatus('saving');
    try {
      const content = editorRef.current?.getValue() ?? '';
      const { versionNum } = await uploadFile(workspaceId, activeFile.fileName, content, activeFile.currentVersion + 1);
      setActiveFile((prev) => (prev ? { ...prev, currentVersion: versionNum } : prev));
      setFiles((prev) => prev.map((f) => (f.id === activeFile.id ? { ...f, currentVersion: versionNum } : f)));
      setSaveStatus('saved');
    } catch (err) {
      setSaveStatus('error');
      toast.error(getErrorMessage(err, `Could not save ${activeFile.fileName}`));
    } finally {
      isSavingRef.current = false;
    }
  }

  async function handleRun() {
    if (!activeFile || !workspaceId || !user) return;
    const { executionLanguage } = detectLanguageFromFileName(activeFile.fileName);
    if (!executionLanguage) {
      toast.error(`${activeFile.fileName} isn't a runnable language`);
      return;
    }

    setIsRunning(true);
    setTerminalLines([{ text: '> Starting execution environment...', kind: 'system' }]);
    
    try {
      const code = editorRef.current?.getValue() ?? activeContent;
      const job = await runCode({ userId: user.id, workspaceId, codeLanguage: executionLanguage, code });
      activeJobIdRef.current = job.id;
      localStorage.setItem(`nexis:lastJob:${workspaceId}`, job.id);

      if (runTimeoutRef.current) window.clearTimeout(runTimeoutRef.current);
      
      // BUMP TIMEOUT TO 60 SECONDS FOR LOCAL MINIKUBE JVM COLD STARTS
      runTimeoutRef.current = window.setTimeout(async () => {
        runTimeoutRef.current = null;
        if (activeJobIdRef.current !== job.id) return; 
        try {
          const status = await getJobStatus(job.id);
          if (status.status === 'COMPLETED' || status.status === 'FAILED') {
            setIsRunning(false);
            setTerminalLines((prev) => [
              ...prev,
              status.status === 'COMPLETED'
                ? { text: status.output || '(no output)', kind: 'output' as const }
                : { text: status.error || 'Execution failed.', kind: 'error' as const },
            ]);
          } else {
            setTerminalLines((prev) => [
              ...prev,
              { text: '> Still no response after 60s — the cluster is under heavy load. Try refreshing.', kind: 'error' },
            ]);
          }
        } catch {
          setTerminalLines((prev) => [
            ...prev,
            { text: '> No response after 60s and the status check failed. Check your cluster connection.', kind: 'error' },
          ]);
        }
      }, 60000); // 60000 ms = 1 minute
      
    } catch (err) {
      setIsRunning(false);
      toast.error(getErrorMessage(err, 'Could not start execution'));
    }
  }

  async function handleKill() {
    if (!activeJobIdRef.current) return;
    if (runTimeoutRef.current) {
      window.clearTimeout(runTimeoutRef.current);
      runTimeoutRef.current = null;
    }
    try {
      await killJob(activeJobIdRef.current);
      setTerminalLines((prev) => [...prev, { text: '> Killed by user.', kind: 'system' }]);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not stop job'));
    } finally {
      setIsRunning(false);
    }
  }

  async function handleLogout() {
    const refreshToken = refreshTokenStorage.get();
    try {
      if (refreshToken) await logoutRequest(refreshToken);
    } catch {
      /* clear local state regardless of network outcome */
    } finally {
      refreshTokenStorage.clear();
      useAuthStore.getState().logout();
      navigate('/login', { replace: true });
    }
  }

  if (!workspaceId) return null;

  if (accessDenied) {
    return (
      <div className="stub-page">
        <div className="stub-card">
          <ShieldOff size={28} color="var(--color-signal-red)" style={{ marginBottom: '1rem' }} />
          <h2 style={{ marginBottom: '0.5rem' }}>Access denied</h2>
          <p style={{ color: 'var(--color-ink-dim)', fontSize: 13, marginBottom: '1.5rem' }}>
            You aren't a member of this workspace, so there's nothing to show here.
          </p>
          <Button variant="ghost" onClick={() => navigate('/dashboard')}>
            <ArrowLeft size={14} />
            back to dashboard
          </Button>
        </div>
      </div>
    );
  }

  const { monacoLanguage, executionLanguage } = activeFile
    ? detectLanguageFromFileName(activeFile.fileName)
    : { monacoLanguage: 'plaintext', executionLanguage: null };

  // ... (Keep all your existing useEffects, states, and logic above this return exact same) ...

  return (
    <div className={`grid h-screen bg-[#09090b] text-zinc-200 ${chatOpen ? 'grid-cols-[240px_1fr_320px]' : 'grid-cols-[240px_1fr]'}`}>
      
      {/* SIDEBAR */}
      <div className="flex flex-col border-r border-zinc-800 bg-[#09090b]">
        <div className="px-4 py-3 flex items-center border-b border-zinc-800/50">
          <button
            type="button"
            className="flex items-center gap-2 text-[12px] font-medium text-zinc-400 transition-colors hover:text-zinc-100"
            onClick={() => navigate('/dashboard')}
          >
            <ArrowLeft size={14} />
            Back to Dashboard
          </button>
        </div>
        <FileTree
          workspaceId={workspaceId}
          files={files}
          activeFileId={activeFile?.id ?? null}
          loading={filesLoading}
          error={filesError}
          onSelect={selectFile}
          onCreated={handleFileCreated}
          onRetry={loadFiles}
        />
      </div>

      {/* MAIN EDITOR AREA */}
      <div className="flex min-w-0 flex-col bg-[#09090b]">
        
        {/* TOP NAVBAR */}
        <div className="flex h-12 items-center justify-between border-b border-zinc-800 px-4">
          <div className="flex min-w-0 items-center gap-3">
            <span className="truncate text-[13px] font-semibold text-zinc-100">{workspace?.name || 'Workspace'}</span>
            {activeFile && (
              <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium uppercase tracking-wider ${saveStatus === 'unsaved' ? 'bg-amber-500/10 text-amber-400' : saveStatus === 'saved' ? 'bg-emerald-500/10 text-emerald-400' : saveStatus === 'error' ? 'bg-red-500/10 text-red-400' : 'text-zinc-600'}`}>
                {saveStatus === 'saving' && 'saving...'}
                {saveStatus === 'saved' && 'saved'}
                {saveStatus === 'unsaved' && 'unsaved'}
                {saveStatus === 'error' && 'error'}
              </span>
            )}
          </div>
          <div className="flex items-center gap-3">
            <PresenceBar presence={presence} members={members} status={socket.status} />
            <div className="h-4 w-px bg-zinc-800" /> {/* Divider */}
            <div className="flex items-center gap-1">
              <button className="icon-btn relative" onClick={() => (chatOpen ? setChatOpen(false) : openChat())}>
                <MessageSquare size={16} />
                {!chatOpen && unreadThreads.size > 0 && <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-blue-500 border-2 border-[#09090b]" />}
              </button>
              {canInvite && (
                <button className="icon-btn" onClick={() => setInviteModalOpen(true)} title="Invite Member">
                  <UserPlus size={16} />
                </button>
              )}
              <button className="icon-btn" onClick={handleLogout} title="Log out">
                <LogOut size={16} />
              </button>
            </div>
          </div>
        </div>

        {/* EDITOR TABS */}
        {activeFile && (
          <div className="flex items-center overflow-x-auto bg-[#09090b] pt-2 px-2">
            {openTabs.map((tab) => {
              const active = tab.id === activeFile.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => selectFile(tab)}
                  className={`group relative flex items-center gap-2 rounded-t-lg px-4 py-2 text-[12px] transition-colors ${
                    active
                      ? 'bg-[#18181b] text-zinc-100 before:absolute before:top-0 before:left-0 before:w-full before:h-[2px] before:bg-blue-500'
                      : 'bg-transparent text-zinc-500 hover:bg-zinc-900 hover:text-zinc-300'
                  }`}
                >
                  <span className="max-w-[150px] truncate">{tab.fileName.split('/').pop()}</span>
                  <span
                    className={`rounded p-0.5 transition-opacity hover:bg-zinc-700 ${active ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}
                    onClick={(event) => closeTab(tab.id, event)}
                  >
                    <X size={12} />
                  </span>
                </button>
              );
            })}
          </div>
        )}

        {/* MONACO EDITOR CONTAINER */}
        <div className="min-h-0 flex-1 bg-[#18181b] relative">
          {/* Top internal border to separate tabs smoothly */}
          <div className="absolute top-0 left-0 w-full h-[1px] bg-[#18181b] z-10" /> 
          
          {contentLoading ? (
            <div className="flex h-full items-center justify-center"><Spinner size={24} /></div>
          ) : contentLoadError ? (
             <div className="flex h-full flex-col items-center justify-center gap-2 text-zinc-400">
               <p>Couldn't load file.</p>
               <button className="text-blue-400" onClick={() => selectFile(activeFile!)}>Try again</button>
             </div>
          ) : activeFile ? (
            <EditorPane
              key={activeFile.id}
              ref={editorRef}
              fileId={activeFile.id}
              initialContent={activeContent}
              initialVersion={0}
              monacoLanguage={monacoLanguage}
              myUserId={user?.id ?? ''}
              readOnly={socket.status !== 'connected'}
              sendOperation={socket.sendOperation}
              sendCursor={socket.sendCursor}
              onDirty={() => setSaveStatus((s) => (s === 'saving' ? s : 'unsaved'))}
              onSaveRequested={handleSave}
            />
          ) : (
            <div className="flex h-full items-center justify-center text-zinc-600 text-[13px]">
              {filesLoading ? <Spinner size={22} /> : 'Select a file to start coding.'}
            </div>
          )}
        </div>

        {/* BOTTOM TERMINAL */}
        <Terminal
          lines={terminalLines}
          isRunning={isRunning}
          canRun={!!activeFile && !!executionLanguage && !isRunning}
          isExpanded={terminalExpanded}
          onToggleExpanded={() => setTerminalExpanded((value) => !value)}
          onRun={handleRun}
          onKill={handleKill}
        />
      </div>

      {/* RIGHT SIDEBAR - CHAT */}
      {chatOpen && (
        <ChatPanel
          members={members}
          currentUserId={user?.id ?? ''}
          groupMessages={groupMessages}
          privateThreads={privateThreads}
          typingUserIds={typingUserIds}
          unreadThreads={unreadThreads}
          activeThread={activeThread}
          onSelectThread={selectChatThread}
          onSendGroup={sendGroupMessage}
          onSendPrivate={sendPrivateMessageToUser}
          onTyping={socket.sendTyping}
          onClose={() => setChatOpen(false)}
        />
      )}
      
      {inviteModalOpen && (
        <InviteMemberModal workspaceId={workspaceId} onClose={() => setInviteModalOpen(false)} />
      )}
    </div>
  );
}