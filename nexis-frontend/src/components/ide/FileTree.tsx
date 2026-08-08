import { useMemo, useState, type FormEvent } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronRight, Edit2, FileCode2, Folder, FolderOpen, Plus, RefreshCw, Trash2, X } from 'lucide-react';
import { uploadFile } from '@/api/storage';
import { listWorkspaceFiles, deleteFile, renameFile } from '@/api/files';
import { getErrorMessage } from '@/api/client';
import { toast } from '@/components/ui/Toast';
import { Spinner } from '@/components/ui/primitives';
import { detectLanguageFromFileName, getBoilerplate } from '@/languages/boilerplates';
import type { WorkspaceFileMeta } from '@/types/api';

// ... (Keep the exact same Props, TreeNode, and boilerplateFor logic) ...
interface Props {
  workspaceId: string;
  files: WorkspaceFileMeta[];
  activeFileId: string | null;
  loading: boolean;
  error: string | null;
  onSelect: (file: WorkspaceFileMeta) => void;
  onCreated: (file: WorkspaceFileMeta) => void;
  onRetry: () => void;
}

type TreeNode = {
  name: string;
  path: string;
  type: 'file' | 'folder';
  meta?: WorkspaceFileMeta;
  children: Record<string, TreeNode>;
};

function TreeItem({ node, activeFileId, workspaceId, onSelect, level = 0 }: any) {
  const [isOpen, setIsOpen] = useState(true);
  const paddingLeft = `${level * 12 + 12}px`;

  // ... (Keep handleDelete and handleRename exactly the same) ...
  async function handleDelete(e: React.MouseEvent, fileId: string) {
    e.stopPropagation();
    if (!confirm('Are you sure you want to delete this file?')) return;
    try {
      await deleteFile(fileId, workspaceId);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to delete file'));
    }
  }

  async function handleRename(e: React.MouseEvent, fileId: string, currentName: string) {
    e.stopPropagation();
    const newName = prompt('Enter new path/name:', currentName);
    if (!newName || newName === currentName) return;
    try {
      await renameFile(fileId, workspaceId, newName);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to rename file'));
    }
  }

  if (node.type === 'folder') {
    return (
      <li>
        <motion.button
          whileHover={{ backgroundColor: 'rgba(255,255,255,0.04)' }}
          className="group flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-[13px] text-zinc-400 transition-colors hover:text-zinc-100"
          onClick={() => setIsOpen(!isOpen)}
          style={{ paddingLeft }}
        >
          <motion.span animate={{ rotate: isOpen ? 90 : 0 }} transition={{ duration: 0.15 }} className="text-zinc-500">
            <ChevronRight size={14} />
          </motion.span>
          {isOpen ? <FolderOpen size={14} className="text-blue-400/80" /> : <Folder size={14} className="text-blue-400/80" />}
          <span className="truncate font-medium">{node.name}</span>
        </motion.button>
        <AnimatePresence initial={false}>
          {isOpen && (
            <motion.ul initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
              {Object.values(node.children).sort((a: any, b: any) => {
                  if (a.type !== b.type) return a.type === 'folder' ? -1 : 1;
                  return a.name.localeCompare(b.name);
                }).map((child: any) => (
                  <TreeItem key={child.path} node={child} activeFileId={activeFileId} workspaceId={workspaceId} onSelect={onSelect} level={level + 1} />
                ))}
            </motion.ul>
          )}
        </AnimatePresence>
      </li>
    );
  }

  const isActive = node.meta?.id === activeFileId;

  return (
    <li className="group relative mt-[1px]">
      <motion.button
        whileHover={{ backgroundColor: isActive ? '' : 'rgba(255,255,255,0.04)' }}
        className={`relative flex w-full items-center gap-2 rounded-md px-2 py-1.5 pr-10 text-left text-[13px] transition-colors ${
          isActive
            ? 'bg-blue-500/10 text-blue-100 font-medium'
            : 'text-zinc-400 hover:text-zinc-100'
        }`}
        onClick={() => onSelect(node.meta!)}
        style={{ paddingLeft }}
      >
        <FileCode2 size={14} className={isActive ? 'text-blue-400' : 'text-zinc-500'} />
        <span className="truncate">{node.name}</span>
      </motion.button>
      
      {/* Action buttons (Rename/Delete) show on hover */}
      <div className="pointer-events-none absolute inset-y-0 right-2 flex items-center gap-1 opacity-0 transition-opacity group-hover:pointer-events-auto group-hover:opacity-100">
        <button type="button" className="rounded p-1 text-zinc-500 hover:bg-zinc-700 hover:text-zinc-200" onClick={(e) => handleRename(e, node.meta!.id, node.meta!.fileName)}>
          <Edit2 size={12} />
        </button>
        <button type="button" className="rounded p-1 text-zinc-500 hover:bg-zinc-700 hover:text-red-400" onClick={(e) => handleDelete(e, node.meta!.id)}>
          <Trash2 size={12} />
        </button>
      </div>
    </li>
  );
}

export function FileTree({ workspaceId, files, activeFileId, loading, error, onSelect, onCreated, onRetry }: Props) {
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState('');
  const [busy, setBusy] = useState(false);

  // ... (Keep useMemo virtualTree logic exact same) ...
  const virtualTree = useMemo(() => {
    const root: Record<string, TreeNode> = {};
    for (const file of files) {
      const parts = file.fileName.split('/');
      let currentLevel = root;
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i];
        const isFile = i === parts.length - 1;
        if (!currentLevel[part]) {
          currentLevel[part] = { name: part, path: parts.slice(0, i + 1).join('/'), type: isFile ? 'file' : 'folder', children: {}, meta: isFile ? file : undefined };
        }
        currentLevel = currentLevel[part].children;
      }
    }
    return root;
  }, [files]);

  // ... (Keep handleCreate logic exact same) ...
  function boilerplateFor(fileName: string): string {
    const { executionLanguage } = detectLanguageFromFileName(fileName);
    if (!executionLanguage) return '';
    const { content } = getBoilerplate(executionLanguage);
    if (executionLanguage === 'JAVA') {
      const baseName = fileName.split('/').pop()?.replace(/\.java$/i, '');
      if (baseName && /^[A-Za-z_$][A-Za-z0-9_$]*$/.test(baseName)) {
        return content.replace(/\bpublic class Main\b/, `public class ${baseName}`);
      }
    }
    return content;
  }

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    const name = newName.trim().replace(/^\/+|\/+$/g, '');
    if (!name) return;
    setBusy(true);
    try {
      const content = boilerplateFor(name);
      await uploadFile(workspaceId, name, content);
      const updated = await listWorkspaceFiles(workspaceId);
      const created = updated.find((f) => f.fileName === name);
      if (created) onCreated(created);
      toast.success(`${name} created`);
      setNewName('');
      setCreating(false);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not create file'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex h-full flex-col overflow-hidden bg-transparent p-3">
      <div className="mb-4 flex items-center justify-between px-1">
        <span className="text-[11px] font-semibold uppercase tracking-widest text-zinc-500">Explorer</span>
        <button
          className="rounded p-1 text-zinc-400 transition-colors hover:bg-zinc-800 hover:text-zinc-100"
          onClick={() => setCreating((v) => !v)}
        >
          {creating ? <X size={14} /> : <Plus size={14} />}
        </button>
      </div>

      {creating && (
        <form onSubmit={handleCreate} className="mb-4 space-y-2 rounded-lg border border-zinc-800 bg-zinc-900 p-2 shadow-lg">
          <input
            autoFocus
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="folder/filename.ext"
            className="w-full rounded-md border border-zinc-700 bg-zinc-950 px-2.5 py-1.5 text-[13px] text-zinc-200 placeholder:text-zinc-600 focus:border-blue-500 focus:outline-none"
            disabled={busy}
          />
          <button type="submit" disabled={busy} className="w-full rounded-md bg-blue-600 px-2.5 py-1.5 text-[12px] font-medium text-white hover:bg-blue-500 disabled:opacity-50">
            {busy ? 'Creating...' : 'Create File'}
          </button>
        </form>
      )}

      {loading ? (
        <div className="flex justify-center py-8"><Spinner size={18} /></div>
      ) : error ? (
        <div className="rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-[12px] text-red-300">
          <p className="mb-2">{error}</p>
          <button type="button" className="flex items-center gap-2 text-red-200 hover:text-white" onClick={onRetry}><RefreshCw size={12} /> Retry</button>
        </div>
      ) : files.length === 0 ? (
        <p className="px-1 py-4 text-[12px] text-zinc-500">Workspace is empty.</p>
      ) : (
        <ul className="space-y-0.5 overflow-y-auto pr-1">
          {Object.values(virtualTree).sort((a, b) => {
              if (a.type !== b.type) return a.type === 'folder' ? -1 : 1;
              return a.name.localeCompare(b.name);
            }).map((node) => (
              <TreeItem key={node.path} node={node} activeFileId={activeFileId} workspaceId={workspaceId} onSelect={onSelect} />
            ))}
        </ul>
      )}
    </div>
  );
}