import type { editor as MonacoEditorNS } from 'monaco-editor';
import type { CursorPayload } from '@/types/api';

const PALETTE = ['cyan', 'magenta', 'violet'];

export function colorForUser(userId: string): string {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) hash = (hash * 31 + userId.charCodeAt(i)) >>> 0;
  return PALETTE[hash % PALETTE.length];
}

interface Entry {
  widget: MonacoEditorNS.IContentWidget;
  position: { lineNumber: number; column: number };
}


export class RemoteCursorManager {
  private editor: MonacoEditorNS.IStandaloneCodeEditor;
  private entries = new Map<string, Entry>();
  private decorationsByUser = new Map<string, string>();

  constructor(editor: MonacoEditorNS.IStandaloneCodeEditor) {
    this.editor = editor;
  }

  update(userId: string, name: string, cursor: CursorPayload): void {
    const model = this.editor.getModel();
    if (!model) return;

    const line = Math.min(Math.max(1, cursor.line), model.getLineCount());
    const maxCol = model.getLineMaxColumn(line);
    const column = Math.min(Math.max(1, cursor.characterIndex + 1), maxCol);
    const color = colorForUser(userId);
    const position = { lineNumber: line, column };

    const range = { startLineNumber: line, startColumn: column, endLineNumber: line, endColumn: column };
    const previousDecoration = this.decorationsByUser.get(userId);
    const newIds = this.editor.deltaDecorations(
      previousDecoration ? [previousDecoration] : [],
      [{ range, options: { className: `remote-caret remote-caret-${color}`, stickiness: 1 } }]
    );
    this.decorationsByUser.set(userId, newIds[0]);

    const existing = this.entries.get(userId);
    if (existing) {
      existing.position = position;
      this.editor.layoutContentWidget(existing.widget);
      return;
    }

    const domNode = document.createElement('div');
    domNode.className = `remote-caret-label remote-caret-label-${color}`;
    domNode.textContent = name;

    const entry: Entry = {
      position,
      widget: {
        getId: () => `remote-cursor-${userId}`,
        getDomNode: () => domNode,
        getPosition: () => ({ position: entry.position, preference: [1, 2] }),
      },
    };
    this.entries.set(userId, entry);
    this.editor.addContentWidget(entry.widget);
  }

  remove(userId: string): void {
    const entry = this.entries.get(userId);
    if (entry) {
      this.editor.removeContentWidget(entry.widget);
      this.entries.delete(userId);
    }
    const decorationId = this.decorationsByUser.get(userId);
    if (decorationId) {
      this.editor.deltaDecorations([decorationId], []);
      this.decorationsByUser.delete(userId);
    }
  }

  disposeAll(): void {
    for (const userId of Array.from(this.entries.keys())) this.remove(userId);
  }
}
