/**
 * NOT an OT implementation. The actual operational transform — resolving
 * concurrent edits against document history, deciding the "true" operation
 * — happens server-side (Redisson lock + version history, per the docs).
 * This module only does the two things the client is responsible for:
 *   1. Convert Monaco's local change events into the wire format.
 *   2. Play back whatever the server broadcasts, onto the local model.
 * There is no transform(opA, opB) here, and there shouldn't be — that logic
 * already exists in the backend, and duplicating it client-side would be
 * both redundant and a likely source of the two ends disagreeing.
 */
import type { editor as MonacoEditorNS, IRange } from 'monaco-editor';
import type { CodeOperation, OperationType } from '@/types/api';

export interface LocalOpDraft {
  operationType: OperationType;
  position: number;
  code: string;
  length: number;
}

export function changesToOpDrafts(changes: readonly MonacoEditorNS.IModelContentChange[]): LocalOpDraft[] {
  const drafts: LocalOpDraft[] = [];
  for (const change of changes) {
    if (change.rangeLength > 0) {
      drafts.push({ operationType: 'DELETE', position: change.rangeOffset, length: change.rangeLength, code: '' });
    }
    if (change.text.length > 0) {
      // CRITICAL FIX: Force all OS-level newlines to a single \n character.
      // Otherwise, Windows sending \r\n (length 2) while Monaco stores \n (length 1)
      // permanently corrupts the 1D absolute offsets between the client and server.
      const sanitizedText = change.text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
      drafts.push({
        operationType: 'INSERT',
        position: change.rangeOffset,
        length: sanitizedText.length, // Use the sanitized length!
        code: sanitizedText,
      });
    }
  }
  return drafts;
}
function toRange(model: MonacoEditorNS.ITextModel, start: number, end: number): IRange {
  const s = model.getPositionAt(start);
  const e = model.getPositionAt(end);
  return { startLineNumber: s.lineNumber, startColumn: s.column, endLineNumber: e.lineNumber, endColumn: e.column };
}

/**
 * Applies a remote operation to the model. Relies on Monaco's
 * forceMoveMarkers: true plus its own internal cursor-tracking to handle
 * the local caret position across the edit, rather than this module also
 * computing and setting a new caret position — doing both at once was
 * causing a double-adjustment (the caret would move once from Monaco's own
 * tracking, then again from an explicit setPosition call here).
 */
export function applyRemoteOperation(editor: MonacoEditorNS.IStandaloneCodeEditor, op: CodeOperation): void {
  if (op.operationType === 'RETAIN') return;

  const model = editor.getModel();
  if (!model) return;

  if (typeof op.position !== 'number' || Number.isNaN(op.position)) {
    // eslint-disable-next-line no-console
    console.error('[Nexis] Remote op has a non-numeric position — payload shape likely does not match the CodeOperation contract this client expects:', op);
    return;
  }
  if (op.operationType === 'DELETE' && (typeof op.length !== 'number' || Number.isNaN(op.length))) {
    // eslint-disable-next-line no-console
    console.error('[Nexis] Remote DELETE has a non-numeric length — payload shape likely does not match the CodeOperation contract this client expects:', op);
    return;
  }

  const docLength = model.getValueLength();
  const opEnd = op.operationType === 'DELETE' ? op.position + op.length : op.position;
  if (op.position > docLength || opEnd > docLength) {
    // This client's buffer is shorter than the offset the server is addressing — it has
    // diverged from the server's live document (e.g. it was seeded from a stale saved
    // snapshot rather than the current live state). Monaco's getPositionAt will silently
    // clamp to end-of-document rather than throw, which is exactly how a misplaced insert
    // or a silently-no-op delete would look. Flagging it loudly rather than eating it.
    // eslint-disable-next-line no-console
    console.warn(
      `[Nexis] Remote op targets offset ${opEnd} but the local document is only ${docLength} chars — this client is likely out of sync with the server's live document.`,
      op
    );
  }

  if (op.operationType === 'INSERT') {
    model.applyEdits([{ range: toRange(model, op.position, op.position), text: op.code, forceMoveMarkers: true }]);
  } else {
    model.applyEdits([
      { range: toRange(model, op.position, op.position + op.length), text: '', forceMoveMarkers: true },
    ]);
  }
}
