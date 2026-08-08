import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, Pause, FastForward } from 'lucide-react';
import { getSessionEvents, getSession } from '@/api/sessions';
import { getWorkspaceMembers } from '@/api/workspaces';
import { EditorPane, type EditorPaneHandle } from '@/components/ide/EditorPane';
import { Spinner } from '@/components/ui/primitives';
import { getErrorMessage } from '@/api/client';
import { toast } from '@/components/ui/Toast';
import type { CodeOperation, RecordingSession, SessionEvent, WorkspaceMember, ChatMessage } from '@/types/api';

export function PlaybackPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const editorRef = useRef<EditorPaneHandle>(null);

  // ... (Keep all state, refs, and the playback engine useEffects exactly the same) ...
  const [loading, setLoading] = useState(true);
  const [sessionInfo, setSessionInfo] = useState<RecordingSession | null>(null);
  const [events, setEvents] = useState<SessionEvent[]>([]);
  const [members, setMembers] = useState<Map<string, WorkspaceMember>>(new Map());
  const [chatLog, setChatLog] = useState<SessionEvent[]>([]);
  const [playbackLanguage, setPlaybackLanguage] = useState<string>('plaintext');
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [speed, setSpeed] = useState<number>(1);

  const isPlayingRef = useRef(isPlaying); isPlayingRef.current = isPlaying;
  const speedRef = useRef(speed); speedRef.current = speed;
  const currentIndexRef = useRef(currentIndex);
  const timerRef = useRef<number | null>(null);

  // 1. Initial Data Fetch
  useEffect(() => {
    if (!sessionId) return;
    Promise.all([getSession(sessionId), getSessionEvents(sessionId)])
      .then(([sessionData, eventData]) => {
        setSessionInfo(sessionData);
        setEvents(eventData);
        const firstCodeOp = eventData.find(e => e.eventType === 'CODE_CHANGE');
        if (firstCodeOp) {
          const op = (firstCodeOp.payload as unknown) as CodeOperation;
          if (op.language) setPlaybackLanguage(op.language);
        }
        if (sessionData.workspaceId) {
          getWorkspaceMembers(sessionData.workspaceId)
            .then((list) => setMembers(new Map(list.map((m) => [m.userId, m]))))
            .catch(() => {});
        }
      })
      .catch((err) => toast.error(getErrorMessage(err, 'Failed to load playback data')))
      .finally(() => setLoading(false));
  }, [sessionId]);

  // 2. Event Processor
  const processEvent = useCallback((event: SessionEvent) => {
    if (event.eventType === 'CODE_CHANGE') {
      const op = (event.payload as unknown) as CodeOperation;
      editorRef.current?.applyRemoteOperation(op);
    } else if (event.eventType === 'CHAT_MESSAGE') {
      setChatLog((prev) => [...prev, event]);
    }
  }, []);

  // 3. Playback Engine Loop
  useEffect(() => {
    if (isPlaying) {
      const nextTick = () => {
        if (!isPlayingRef.current) return;
        const idx = currentIndexRef.current;
        if (idx >= events.length - 1) {
          setIsPlaying(false);
          return;
        }
        const currentEvent = events[idx];
        const nextEvent = events[idx + 1];
        const t1 = new Date(currentEvent.timestamp).getTime();
        const t2 = new Date(nextEvent.timestamp).getTime();
        let delay = Math.max(0, (t2 - t1) / speedRef.current);
        if (delay > 2000) delay = 2000 / speedRef.current;

        timerRef.current = window.setTimeout(() => {
          if (!isPlayingRef.current) return;
          processEvent(nextEvent);
          currentIndexRef.current += 1;
          setCurrentIndex(currentIndexRef.current);
          nextTick(); 
        }, delay);
      };
      if (currentIndexRef.current === 0 && events.length > 0) processEvent(events[0]);
      nextTick();
    }
    return () => { if (timerRef.current) window.clearTimeout(timerRef.current); };
  }, [isPlaying, events, processEvent]);

  if (loading) {
    return <div className="flex h-screen items-center justify-center bg-[#09090b]"><Spinner size={30} /></div>;
  }

  if (events.length === 0) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#09090b] p-8">
        <div className="max-w-[480px] rounded-xl border border-dashed border-zinc-800 bg-[#18181b] p-10 text-center">
          <h2 className="mb-2 text-lg font-medium text-zinc-200">No Events Found</h2>
          <p className="mb-6 text-[13px] text-zinc-500">This session was recorded but no code was typed.</p>
          <button className="inline-flex items-center gap-2 text-[13px] text-blue-400 hover:text-blue-300" onClick={() => navigate('/dashboard')}>
            <ArrowLeft size={14} /> Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  const progressPercent = events.length > 1 ? (currentIndex / (events.length - 1)) * 100 : 100;

  return (
    <div className="flex h-screen flex-col bg-[#09090b] text-zinc-200">
      

      {/* Topbar */}
      <div className="flex h-12 shrink-0 items-center justify-between border-b border-zinc-800 px-4">
        <div className="flex items-center gap-3">
          <button className="rounded p-1.5 text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100" onClick={() => navigate('/dashboard')}>
            <ArrowLeft size={14} />
          </button>
          <span className="text-[13px] font-semibold text-zinc-100">
            Playback History <span className="mx-2 text-zinc-600">|</span> Session {sessionInfo?.id.substring(0, 8)}
          </span>
        </div>

        
        <div className="flex items-center gap-4">
          <span className="text-[12px] font-medium text-zinc-500">Speed: {speed}x</span>
          <button 
            className="rounded p-1.5 text-zinc-400 hover:bg-zinc-800 hover:text-zinc-100" 
            onClick={() => setSpeed(s => s === 8 ? 1 : s * 2)} 
            title="Toggle Speed"
          >
            <FastForward size={14} />
          </button>
        </div>
      </div>

      {/* Main IDE area */}
      <div className="flex min-h-0 flex-1">
        
        {/* Editor Container with Glass Shield */}
        <div className="relative flex-1 bg-[#18181b] pointer-events-none">
          <EditorPane
            ref={editorRef}
            fileId="playback-file"
            initialContent=""
            initialVersion={0}
            monacoLanguage={playbackLanguage}
            myUserId="readonly-viewer"
            readOnly={false} 
            sendOperation={() => {}} 
            sendCursor={() => {}}    
          />
        </div>

        {/* Audit Log Sidebar */}
        <div className="flex w-[320px] shrink-0 flex-col border-l border-zinc-800 bg-[#09090b]">
          <div className="flex items-center border-b border-zinc-800 px-4 py-3">
            <span className="text-[11px] font-semibold uppercase tracking-widest text-zinc-500">Audit Log</span>
          </div>
          <div className="flex-1 overflow-y-auto p-4 text-[12.5px] leading-relaxed">
            {chatLog.map((log, i) => {
              const name = members.get(log.userId)?.fullname || 'Unknown User';
              const chatPayload = (log.payload as unknown) as ChatMessage;
              return (
                <div key={i} className="mb-3">
                  <span className="font-semibold text-blue-400">{name}: </span>
                  <span className="text-zinc-300">{chatPayload.message}</span>
                </div>
              );
            })}
            {chatLog.length === 0 && <div className="italic text-zinc-600">Event log is empty...</div>}
          </div>
        </div>
      </div>

      {/* VCR Control Deck */}
      <div className="flex h-16 shrink-0 items-center gap-6 border-t border-zinc-800 bg-[#09090b] px-6">
        <button 
          onClick={() => setIsPlaying(!isPlaying)} 
          className="flex w-24 items-center justify-center gap-2 rounded-lg bg-zinc-800 px-4 py-2 text-[13px] font-medium text-white transition-colors hover:bg-zinc-700"
        >
          {isPlaying ? <><Pause size={14} fill="currentColor" /> Pause</> : <><Play size={14} fill="currentColor" /> Play</>}
        </button>

        {/* Progress Bar */}
        <div className="relative h-1.5 flex-1 overflow-hidden rounded-full bg-zinc-800">
          <div 
            className="absolute left-0 top-0 h-full bg-blue-500 transition-all duration-100 ease-linear"
            style={{ width: `${progressPercent}%` }} 
          />
        </div>

        <span className="w-20 text-right font-mono text-[12px] text-zinc-500">
          {currentIndex} / {events.length - 1}
        </span>
      </div>
    </div>
  );
}