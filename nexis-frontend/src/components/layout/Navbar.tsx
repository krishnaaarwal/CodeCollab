import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { TerminalSquare, LogOut, Copy, Check, Settings, Moon, Trash2, ChevronDown } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { logoutRequest } from '@/api/auth';
import { refreshTokenStorage } from '@/lib/refreshTokenStorage';
import { toast } from '@/components/ui/Toast';

export function Navbar() {
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  async function handleLogout() {
    const refreshToken = refreshTokenStorage.get();
    try {
      if (refreshToken) await logoutRequest(refreshToken);
    } catch {
      // Intentionally swallow
    } finally {
      refreshTokenStorage.clear();
      useAuthStore.getState().logout();
      navigate('/login', { replace: true });
    }
  }

  function copyUuid() {
    if (!user) return;
    navigator.clipboard.writeText(user.id);
    setCopied(true);
    toast.success('UUID copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="flex h-16 items-center justify-between border-b border-zinc-800/80 bg-[#09090b] px-6">
      <div className="flex items-center gap-2 font-display text-lg font-bold tracking-tight text-zinc-100">
        <TerminalSquare size={22} className="text-blue-500" />
        <span>NEXIS</span>
      </div>
      
      <div className="flex items-center gap-5 relative" ref={dropdownRef}>
        {user && (
          <button 
            onClick={() => setDropdownOpen(!dropdownOpen)}
            className="flex items-center gap-3 rounded-lg p-1.5 transition-colors hover:bg-zinc-800/50"
          >
            <div className="hidden sm:block text-right">
              <div className="text-[13px] font-medium text-zinc-200">{user.fullname}</div>
              <div className="text-[11px] text-zinc-500">{user.email}</div>
            </div>
            <img src={user.avatar} alt="" className="h-9 w-9 rounded-full border border-zinc-700 bg-zinc-800 object-cover" />
            <ChevronDown size={14} className="text-zinc-500" />
          </button>
        )}
        
        {dropdownOpen && user && (
          <div className="absolute right-0 top-14 z-50 w-64 rounded-xl border border-zinc-800 bg-[#18181b] p-2 shadow-2xl">
            <div className="mb-2 flex flex-col gap-1 border-b border-zinc-800/60 p-2 pb-3">
              <span className="text-[10px] font-semibold uppercase tracking-widest text-zinc-500">Your UUID</span>
              <button 
                onClick={copyUuid}
                className="flex items-center justify-between rounded bg-zinc-900/50 px-2 py-1.5 text-[11px] font-mono text-zinc-300 transition-colors hover:bg-zinc-800"
              >
                <span className="truncate">{user.id}</span>
                {copied ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} className="text-zinc-500" />}
              </button>
            </div>
            
            <div className="flex flex-col gap-0.5">
              <button className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-[12px] text-zinc-300 transition-colors hover:bg-zinc-800 hover:text-zinc-100" onClick={() => toast.info('Theme toggle coming soon')}>
                <Moon size={14} /> Theme: Dark
              </button>
              <button className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-[12px] text-zinc-300 transition-colors hover:bg-zinc-800 hover:text-zinc-100" onClick={() => toast.info('About settings coming soon')}>
                <Settings size={14} /> Settings & Profile
              </button>
              <div className="my-1 h-px w-full bg-zinc-800/60" />
              <button onClick={handleLogout} className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-[12px] text-zinc-300 transition-colors hover:bg-zinc-800 hover:text-zinc-100">
                <LogOut size={14} /> Log out
              </button>
              <button className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-[12px] text-red-400 transition-colors hover:bg-red-500/10 hover:text-red-300" onClick={() => toast.error('Account deletion not yet implemented')}>
                <Trash2 size={14} /> Delete Account
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}