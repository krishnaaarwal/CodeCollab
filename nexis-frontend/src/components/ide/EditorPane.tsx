import { forwardRef, useCallback, useImperativeHandle, useRef } from 'react';
import { motion } from 'framer-motion';
import Editor, { type OnMount } from '@monaco-editor/react';
import type { editor as MonacoEditorNS } from 'monaco-editor';
import { applyRemoteOperation as applyRemoteOperationImpl, changesToOpDrafts, type LocalOpDraft } from '@/ot/otEngine';
import { RemoteCursorManager } from './remoteCursors';
import type { CodeOperation, CursorPayload } from '@/types/api';

interface Props {
  fileId: string;
  initialContent: string;
  initialVersion: number;
  monacoLanguage: string;
  myUserId: string;
  readOnly: boolean;
  sendOperation: (op: Omit<CodeOperation, 'userId'>) => void;
  sendCursor: (line: number, characterIndex: number) => void;
  onDirty?: () => void;
  onSaveRequested?: () => void;
}

export interface EditorPaneHandle {
  applyRemoteOperation: (op: CodeOperation) => void;
  updateRemoteCursor: (userId: string, name: string, cursor: CursorPayload) => void;
  removeRemoteCursor: (userId: string) => void;
  getValue: () => string;
}

export const EditorPane = forwardRef<EditorPaneHandle, Props>(function EditorPane(props, ref) {
  const editorInstanceRef = useRef<MonacoEditorNS.IStandaloneCodeEditor | null>(null);
  const cursorManagerRef = useRef<RemoteCursorManager | null>(null);
  const isApplyingRemoteRef = useRef(false);

  const pendingQueueRef = useRef<LocalOpDraft[]>([]);
  const inFlightOpRef = useRef<LocalOpDraft | null>(null);
  const waitingForAckRef = useRef(false);

  const lastKnownServerVersionRef = useRef(props.initialVersion);

  const propsRef = useRef(props);
  propsRef.current = props;

  const flushQueue = useCallback(() => {
    if (waitingForAckRef.current) return;
    const next = pendingQueueRef.current.shift();
    if (!next) return;

    waitingForAckRef.current = true;
    inFlightOpRef.current = next;

    propsRef.current.sendOperation({
      ...next,
      version: lastKnownServerVersionRef.current,
      language: propsRef.current.monacoLanguage,
    });
  }, []);

  useImperativeHandle(
    ref,
    () => ({
      applyRemoteOperation(op) {
        const editor = editorInstanceRef.current;
        if (!editor) return;
        if (typeof op.version === 'number') {
          lastKnownServerVersionRef.current = op.version;
        }
        if (op.userId === propsRef.current.myUserId) {
          waitingForAckRef.current = false;
          inFlightOpRef.current = null;
          flushQueue();
          return;
        }

        isApplyingRemoteRef.current = true;
        try {
          let adjustedPos = op.position;

          const transformPos = (draft: LocalOpDraft) => {
            if (draft.operationType === 'INSERT') {
              if (draft.position < adjustedPos) {
                adjustedPos += draft.code.length;
              } else if (draft.position === adjustedPos && op.userId > propsRef.current.myUserId) {
                adjustedPos += draft.code.length;
              }
            } else if (draft.operationType === 'DELETE') {
              if (draft.position <= adjustedPos) {
                const draftEnd = draft.position + draft.length;
                if (adjustedPos < draftEnd) {
                  adjustedPos = draft.position;
                } else {
                  adjustedPos -= draft.length;
                }
              }
            }
          };

          if (inFlightOpRef.current) transformPos(inFlightOpRef.current);
          pendingQueueRef.current.forEach(transformPos);

          applyRemoteOperationImpl(editor, { ...op, position: adjustedPos });

          const opLength = op.operationType === 'INSERT' ? op.code.length : op.length;

          pendingQueueRef.current.forEach((draft) => {
            if (op.operationType === 'INSERT') {
              if (op.position < draft.position) {
                draft.position += opLength;
              } else if (op.position === draft.position) {
                if (propsRef.current.myUserId > op.userId) {
                  draft.position += opLength;
                }
              } else {
                if (draft.operationType === 'DELETE') {
                  const draftEnd = draft.position + draft.length;
                  if (op.position < draftEnd) {
                    draft.length += opLength;
                  }
                }
              }
            } else if (op.operationType === 'DELETE') {
              if (op.position <= draft.position) {
                const draftEnd = draft.position + (draft.operationType === 'DELETE' ? draft.length : 0);
                if (op.position + opLength > draft.position) {
                  if (draft.operationType === 'DELETE' && draftEnd > op.position + opLength) {
                    draft.length = draftEnd - (op.position + opLength);
                  } else if (draft.operationType === 'DELETE') {
                    draft.length = 0;
                  }
                  draft.position = op.position;
                } else {
                  draft.position -= opLength;
                }
              } else {
                if (draft.operationType === 'DELETE') {
                  const draftEnd = draft.position + draft.length;
                  if (op.position < draftEnd) {
                    const overlapEnd = Math.min(draftEnd, op.position + opLength);
                    const overlap = overlapEnd - op.position;
                    draft.length -= overlap;
                  }
                }
              }
            }
          });
        } catch (err) {
          console.error('[Nexis] Failed to apply remote operation — payload:', op, err);
        } finally {
          isApplyingRemoteRef.current = false;
        }
      },
      updateRemoteCursor(userId, name, cursor) {
        cursorManagerRef.current?.update(userId, name, cursor);
      },
      removeRemoteCursor(userId) {
        cursorManagerRef.current?.remove(userId);
      },
      getValue() {
        return editorInstanceRef.current?.getValue() ?? '';
      },
    }),
    [flushQueue]
  );

  const handleMount: OnMount = useCallback(
    (editor, monaco) => {
      editorInstanceRef.current = editor;
      cursorManagerRef.current = new RemoteCursorManager(editor);
      editor.getModel()?.setEOL(monaco.editor.EndOfLineSequence.LF);

      editor.onDidChangeModelContent((e) => {
        if (isApplyingRemoteRef.current) return;
        pendingQueueRef.current.push(...changesToOpDrafts(e.changes));
        flushQueue();
        propsRef.current.onDirty?.();
      });

      editor.onDidChangeCursorPosition((e) => {
        propsRef.current.sendCursor(e.position.lineNumber, e.position.column - 1);
      });

      editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
        propsRef.current.onSaveRequested?.();
      });
    },
    [flushQueue]
  );

  const isPlaybackGhost = props.myUserId === 'readonly-viewer';

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.1, ease: 'easeOut' }} className="h-full w-full">
      <Editor
        height="100%"
        defaultLanguage={props.monacoLanguage}
        defaultValue={props.initialContent}
        theme="vs-dark"
        onMount={handleMount}
        options={{
          readOnly: props.readOnly,
          fontFamily: "'JetBrains Mono', monospace",
          fontSize: 13,
          lineHeight: 1.5,
          minimap: { enabled: false },
          automaticLayout: true,
          scrollBeyondLastLine: false,
          wordWrap: 'on',
          padding: { top: 12, bottom: 12 },
          glyphMargin: false,
          lineNumbersMinChars: 3,
          renderLineHighlight: 'line',
          autoClosingBrackets: isPlaybackGhost ? 'never' : 'languageDefined',
          autoClosingQuotes: isPlaybackGhost ? 'never' : 'languageDefined',
          autoSurround: isPlaybackGhost ? 'never' : 'languageDefined',
          formatOnType: !isPlaybackGhost,
          formatOnPaste: !isPlaybackGhost,
          quickSuggestions: !isPlaybackGhost,
          suggestOnTriggerCharacters: !isPlaybackGhost,
        }}
      />
    </motion.div>
  );
});