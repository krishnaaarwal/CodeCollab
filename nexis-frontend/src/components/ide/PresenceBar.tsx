import { colorForUser } from './remoteCursors';
import type { ConnectionStatus } from '@/hooks/useWorkspaceSocket';
import type { WorkspaceMember } from '@/types/api';

interface Props {
  presence: Set<string>;
  members: Map<string, WorkspaceMember>;
  status: ConnectionStatus;
}

const STATUS_LABEL: Record<ConnectionStatus, string> = {
  connecting: 'Connecting...',
  connected: 'Live',
  disconnected: 'Reconnecting...',
};

const PILL_COLORS: Record<string, string> = {
  cyan: 'bg-teal-500',
  magenta: 'bg-pink-500',
  violet: 'bg-violet-500'
};

export function PresenceBar({ presence, members, status }: Props) {
  return (
    <div className="flex items-center gap-4">
      {/* Overlapping avatars */}
      {presence.size > 0 && (
        <div className="flex items-center">
          {Array.from(presence).map((userId) => {
            const member = members.get(userId);
            const label = (member?.fullname || userId).slice(0, 2).toUpperCase();
            const colorClass = PILL_COLORS[colorForUser(userId)] || 'bg-zinc-500';
            
            return (
              <div
                key={userId}
                className={`flex h-6 w-6 -ml-2 items-center justify-center rounded-full border-2 border-[#09090b] ${colorClass} text-[9px] font-bold text-white first:ml-0`}
                title={member?.fullname || userId}
              >
                {label}
              </div>
            );
          })}
        </div>
      )}
      
      {/* Status indicator */}
      <div className="flex items-center gap-1.5 text-[11px] font-medium text-zinc-500">
        <span className="relative flex h-2 w-2">
          {status === 'connected' && <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>}
          <span className={`relative inline-flex h-2 w-2 rounded-full ${
            status === 'connected' ? 'bg-emerald-500' : status === 'connecting' ? 'bg-amber-500' : 'bg-red-500'
          }`}></span>
        </span>
        {STATUS_LABEL[status]}
      </div>
    </div>
  );
}