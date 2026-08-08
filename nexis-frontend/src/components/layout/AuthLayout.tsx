import type { ReactNode } from 'react';
import { motion } from 'framer-motion';

type Token = { text: string; cls?: string };
type Line = Token[];

// Modernized syntax highlighting colors for the decorative code block
const DEMO_LINES: Line[] = [
  [{ text: 'class ', cls: 'text-blue-400' }, { text: 'MergeSort', cls: 'text-emerald-400' }, { text: ' {' }],
  [{ text: '' }],
  [{ text: '  // two people, one file, zero conflicts', cls: 'text-zinc-600 italic' }],
  [{ text: '  ' }, { text: 'static void', cls: 'text-blue-400' }, { text: ' sort(int[] arr) {' }],
  [{ text: '    if (arr.length <= ' }, { text: '1', cls: 'text-amber-400' }, { text: ') return;' }],
  [{ text: '    ' }, { text: 'int', cls: 'text-blue-400' }, { text: ' mid = arr.length / ' }, { text: '2', cls: 'text-amber-400' }, { text: ';' }],
  [{ text: '    ' }, { text: 'var', cls: 'text-blue-400' }, { text: ' left  = split(arr, ' }, { text: '0', cls: 'text-amber-400' }, { text: ', mid);' }],
  [{ text: '    ' }, { text: 'var', cls: 'text-blue-400' }, { text: ' right = split(arr, mid, arr.length);' }],
  [{ text: '    merge(left, right, arr);' }],
  [{ text: '  }' }],
  [{ text: '}' }],
];

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen grid grid-cols-1 md:grid-cols-[1.1fr_1fr] bg-[#09090b] text-zinc-200">
      
      {/* Left Pane - Decorative Hero */}
      <div className="hidden md:flex relative flex-col justify-center bg-[#121214] border-r border-zinc-800/80 p-16 overflow-hidden">
        <pre className="font-mono text-[13px] leading-loose text-zinc-300 relative">
          {DEMO_LINES.map((line, i) => (
            <div key={i}>
              {line.map((tok, j) => (
                <span key={j} className={tok.cls}>{tok.text}</span>
              ))}
              {'\n'}
            </div>
          ))}


          {/* Animated collaborative cursors */}
          <motion.div 
            animate={{ y: [0, -4, 0] }} 
            transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
            className="absolute top-[110px] left-[180px] flex items-center gap-1"
          >
            <div className="h-4 w-[2px] bg-teal-400" />
            <span className="rounded bg-teal-400 px-1.5 py-0.5 text-[9px] font-bold text-[#09090b]">Krishna</span>
          </motion.div>
          
          <motion.div 
            animate={{ y: [0, -4, 0] }} 
            transition={{ duration: 4.5, delay: 1.5, repeat: Infinity, ease: "easeInOut" }}
            className="absolute top-[180px] left-[320px] flex items-center gap-1"
          >
            <div className="h-4 w-[2px] bg-pink-400" />
            <span className="rounded bg-pink-400 px-1.5 py-0.5 text-[9px] font-bold text-[#09090b]">Marin</span>
          </motion.div>
        </pre>

        <div className="mt-14 max-w-[400px] text-[13px] leading-relaxed text-zinc-500">
          <strong className="font-medium text-zinc-200">Every keystroke, live.</strong> Nexis transforms concurrent edits on the server so everyone converges on the same file — no locking, no merge conflicts.
        </div>
      </div>

      {/* Right Pane - Form Content */}
      <div className="flex items-center justify-center p-8">
        <div className="w-full max-w-[360px]">
          {children}
        </div>
      </div>
    </div>
  );
}