import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { X, Send, Hash } from 'lucide-react';
import type { ChatMessage, PrivateChatMessage, WorkspaceMember } from '@/types/api';

export type ChatThread = { kind: 'group' } | { kind: 'dm'; userId: string };

export function threadKey(t: ChatThread): string {
  return t.kind === 'group' ? 'group' : `dm:${t.userId}`;
}

interface Props {
  members: Map<string, WorkspaceMember>;
  currentUserId: string;
  groupMessages: ChatMessage[];
  privateThreads: Map<string, PrivateChatMessage[]>;
  typingUserIds: Set<string>;
  unreadThreads: Set<string>;
  activeThread: ChatThread;
  onSelectThread: (thread: ChatThread) => void;
  onSendGroup: (message: string) => void;
  onSendPrivate: (recipientId: string, message: string) => void;
  onTyping: (isTyping: boolean) => void;
  onClose: () => void;
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export function ChatPanel({ members, currentUserId, groupMessages, privateThreads, typingUserIds, unreadThreads, activeThread, onSelectThread, onSendGroup, onSendPrivate, onTyping, onClose }: Props) {
  const [draft, setDraft] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const typingTimeoutRef = useRef<number | null>(null);

  const roster = useMemo(() => Array.from(members.values()).filter((m) => m.userId !== currentUserId), [members, currentUserId]);
  const activeMessages: Array<ChatMessage | PrivateChatMessage> = activeThread.kind === 'group' ? groupMessages : privateThreads.get(activeThread.userId) ?? [];

  useEffect(() => { bottomRef.current?.scrollIntoView({ block: 'end' }); }, [activeMessages.length, activeThread]);

  // ... (Keep handleDraftChange, handleSend, handleKeyDown exact same) ...
  function handleDraftChange(value: string) {
    setDraft(value);
    if (activeThread.kind !== 'group') return;
    onTyping(true);
    if (typingTimeoutRef.current) window.clearTimeout(typingTimeoutRef.current);
    typingTimeoutRef.current = window.setTimeout(() => onTyping(false), 2000);
  }

  function handleSend(e?: FormEvent) {
    e?.preventDefault();
    const text = draft.trim();
    if (!text) return;
    if (activeThread.kind === 'group') {
      onSendGroup(text);
      if (typingTimeoutRef.current) window.clearTimeout(typingTimeoutRef.current);
      onTyping(false);
    } else {
      onSendPrivate(activeThread.userId, text);
    }
    setDraft('');
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  const othersTyping = Array.from(typingUserIds).filter((id) => id !== currentUserId);

  return (
    <div className="flex h-screen flex-col border-l border-zinc-800 bg-[#09090b]">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-zinc-800 px-4 py-3">
        <span className="text-[11px] font-semibold uppercase tracking-widest text-zinc-500">Team Chat</span>
        <button className="rounded p-1 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-zinc-200" onClick={onClose}>
          <X size={14} />
        </button>
      </div>

      {/* Threads list */}
      <div className="flex max-h-[160px] flex-col gap-1 overflow-y-auto border-b border-zinc-800 p-2">
        <button
          className={`flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-[13px] transition-colors ${activeThread.kind === 'group' ? 'bg-zinc-800 text-blue-400 font-medium' : 'text-zinc-400 hover:bg-zinc-900 hover:text-zinc-200'}`}
          onClick={() => onSelectThread({ kind: 'group' })}
        >
          <Hash size={14} /> Workspace
          {unreadThreads.has('group') && activeThread.kind !== 'group' && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-blue-500" />}
        </button>
        
        {roster.map((m) => (
          <button
            key={m.userId}
            className={`flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-[13px] transition-colors ${(activeThread.kind === 'dm' && activeThread.userId === m.userId) ? 'bg-zinc-800 text-blue-400 font-medium' : 'text-zinc-400 hover:bg-zinc-900 hover:text-zinc-200'}`}
            onClick={() => onSelectThread({ kind: 'dm', userId: m.userId })}
          >
            <div className="flex h-5 w-5 items-center justify-center rounded-full bg-zinc-700 text-[10px] font-bold text-zinc-300">
              {(m.fullname || m.email || '?')[0].toUpperCase()}
            </div>
            <span className="truncate">{m.fullname || m.email}</span>
            {unreadThreads.has(`dm:${m.userId}`) && !(activeThread.kind === 'dm' && activeThread.userId === m.userId) && (
              <span className="ml-auto h-1.5 w-1.5 rounded-full bg-blue-500" />
            )}
          </button>
        ))}
        {roster.length === 0 && <p className="p-3 text-center text-[12px] text-zinc-600">No one else here yet.</p>}
      </div>

      {/* Messages Area */}
      <div className="flex flex-1 flex-col gap-4 overflow-y-auto p-4">
        {activeMessages.length === 0 && (
          <div className="flex h-full items-center justify-center text-[12px] text-zinc-600">
            {activeThread.kind === 'group' ? 'No messages yet — say hi.' : 'Start of your conversation.'}
          </div>
        )}
        
        {activeMessages.map((msg, i) => {
          const mine = msg.userId === currentUserId;
          const name = mine ? 'You' : members.get(msg.userId)?.fullname || 'Unknown';
          return (
            <div key={i} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
              <div className={`flex max-w-[85%] flex-col gap-1 rounded-xl px-3.5 py-2 text-[13px] shadow-sm ${mine ? 'bg-blue-600 text-white rounded-br-sm' : 'bg-zinc-800 text-zinc-200 rounded-bl-sm'}`}>
                {!mine && <span className="text-[10px] font-semibold text-blue-300">{name}</span>}
                <span className="whitespace-pre-wrap break-words">{msg.message}</span>
                <span className={`self-end text-[9px] ${mine ? 'text-blue-200' : 'text-zinc-500'}`}>{formatTime(msg.time)}</span>
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {/* Typing Indicator */}
      <div className="min-h-[20px] px-4 text-[11px] italic text-zinc-500">
        {activeThread.kind === 'group' && othersTyping.length > 0 && (
          <span>
            {othersTyping.map((id) => members.get(id)?.fullname?.split(' ')[0] || 'Someone').join(', ')}
            {othersTyping.length === 1 ? ' is' : ' are'} typing...
          </span>
        )}
      </div>

      {/* Composer */}
      <form className="flex items-end gap-2 border-t border-zinc-800 p-3" onSubmit={handleSend}>
        <textarea
          value={draft}
          onChange={(e) => handleDraftChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={activeThread.kind === 'group' ? 'Message workspace...' : 'Private message...'}
          className="max-h-[120px] min-h-[40px] flex-1 resize-none rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2 text-[13px] text-zinc-100 placeholder:text-zinc-500 focus:border-blue-500 focus:outline-none"
          rows={1}
        />
        <button
          type="submit"
          disabled={!draft.trim()}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-600 text-white transition-colors hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Send size={16} />
        </button>
      </form>
    </div>
  );
}