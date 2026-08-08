import { useEffect, useRef } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronDown, Play, Square } from 'lucide-react';

export type TerminalLineKind = 'output' | 'error' | 'system';

export interface TerminalLine {
  text: string;
  kind: TerminalLineKind;
}

interface Props {
  lines: TerminalLine[];
  isRunning: boolean;
  canRun: boolean;
  isExpanded: boolean;
  onToggleExpanded: () => void;
  onRun: () => void;
  onKill: () => void;
}

export function Terminal({ lines, isRunning, canRun, isExpanded, onToggleExpanded, onRun, onKill }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' });
  }, [lines]);

  return (
    <motion.div layout transition={{ duration: 0.15, ease: 'easeOut' }} className="flex flex-col border-t border-zinc-800 bg-[#09090b]">
      <div className="flex h-11 items-center justify-between border-b border-zinc-800/50 px-4 bg-[#09090b]">
        <div className="flex items-center gap-3">
          <span className="text-[11px] font-semibold uppercase tracking-widest text-zinc-500">Terminal</span>
          <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium uppercase tracking-wider ${isRunning ? 'bg-blue-500/10 text-blue-400' : 'bg-zinc-800/50 text-zinc-500'}`}>
            {isRunning ? 'Running' : 'Ready'}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            className="rounded p-1.5 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-zinc-200"
            onClick={onToggleExpanded}
            aria-label={isExpanded ? 'Collapse terminal' : 'Expand terminal'}
          >
            <motion.span animate={{ rotate: isExpanded ? 180 : 0 }} transition={{ duration: 0.15, ease: 'easeOut' }}>
              <ChevronDown size={14} />
            </motion.span>
          </button>
          <div className="h-4 w-px bg-zinc-800 mx-1" />
          {isRunning ? (
            <button
              type="button"
              className="flex items-center gap-1.5 rounded bg-red-500/10 px-3 py-1.5 text-[12px] font-medium text-red-500 transition-colors hover:bg-red-500/20"
              onClick={onKill}
            >
              <Square size={12} fill="currentColor" /> Stop
            </button>
          ) : (
            <button
              type="button"
              disabled={!canRun}
              className="flex items-center gap-1.5 rounded bg-blue-600 px-3 py-1.5 text-[12px] font-medium text-white transition-colors hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              onClick={onRun}
            >
              <Play size={12} fill="currentColor" /> Run
            </button>
          )}
        </div>
      </div>
      
      <AnimatePresence initial={false}>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: '16rem', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.15, ease: 'easeOut' }}
            className="overflow-hidden"
          >
            <div className="h-64 overflow-auto bg-[#09090b] px-4 py-3 font-mono text-[13px] leading-relaxed text-zinc-300 selection:bg-blue-500/30">
              {lines.length === 0 && <div className="text-zinc-600 italic">// execution output will appear here</div>}
              {lines.map((line, i) => (
                <div key={i} className={`whitespace-pre-wrap ${line.kind === 'error' ? 'text-red-400 font-medium' : line.kind === 'system' ? 'text-zinc-500 italic' : 'text-zinc-300'}`}>
                  {line.text}
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}