# Nexis — Frontend

React 18 + TypeScript (strict) + Vite + Tailwind v4. Real-time collaborative IDE frontend for the
Nexis microservices backend (Auth, Workspace, Execution, Storage, Recording, Collaboration).

## Setup

```bash
npm install
cp .env.example .env   # defaults already match a local backend on the documented ports
npm run dev             # http://localhost:5173
```

`npm run build` runs a full TypeScript check (`tsc --noEmit`) before bundling — treat any red
output from that command as a real bug, not a lint nag.

## ⚠️ Backend bugs found while reviewing your actual source (not yet fixed — needs your changes)

You shared the real `websocket-service` and `execution-service` code. These are concrete,
line-level bugs, not guesses — I traced through them with worked examples before writing this.

**1. `OTEngine.java` — `insert_delete` uses the wrong field, always shifts by zero.**
```java
private static CodeOperation insert_delete(CodeOperation incoming, CodeOperation historical) {
    if (historical.getPosition() <= incoming.getPosition()) {
        int codeLength = historical.getCode().length();   // <-- BUG
        incoming.setPosition(incoming.getPosition() + codeLength);
    }
    return incoming;
}
```
This transforms an incoming INSERT against a historical DELETE. `historical.getCode()` is empty
for a delete — deletes carry their extent in `length`, not `code` (your own comment at the top of
`CodeOperation.java` says this explicitly: *"DELETE: send position, length... the server knows what
to chop out without needing the string itself"*). So `codeLength` is always `0` — a prior delete
never shifts a later insert's position at all — and the sign is backwards besides: a delete before
the incoming position should shift it *left*, not right. Fix:
```java
if (historical.getPosition() <= incoming.getPosition()) {
    incoming.setPosition(incoming.getPosition() - historical.getLength());
}
```

**2. `OTEngine.java` — `delete_insert` has the wrong sign entirely.**
```java
private static CodeOperation delete_insert(CodeOperation incoming, CodeOperation historical) {
    if (historical.getPosition() < incoming.getPosition()) {
        int newPosition = Math.max(historical.getPosition(), incoming.getPosition() - historical.getLength());
        incoming.setPosition(newPosition);   // <-- BUG: subtracting
    }
    return incoming;
}
```
This transforms an incoming DELETE against a historical INSERT. A historical insert *before* the
incoming delete's position pushes everything after it *right* — the incoming delete's position
needs to *increase* by the inserted length to still target the same logical content, not decrease.
Worked example: historical INSERT of 5 chars at position 2; incoming DELETE originally targeting
position 10. Correct new position is `10 + 5 = 15`. Current code computes
`max(2, 10 - 5) = 5` — it'd delete completely unrelated content 10 characters away from where it
should. Fix:
```java
if (historical.getPosition() <= incoming.getPosition()) {
    incoming.setPosition(incoming.getPosition() + historical.getLength());
}
```
(no `Math.max` clamp needed — addition can't go negative.)

**Why these explain what you're seeing, not just theoretically:** both bugs only fire when an
insert and a delete interact — exactly "two people typing and deleting near each other," which is
exactly issue #1/#3 in your reports. `delete_delete` (the one with the `// CLAUDE'S FIX` comment)
and `insert_insert` I traced through by hand too and both check out correctly — it's specifically
the two mixed-type cases that are broken. I did *not* touch your Java files — I don't have your
backend repo, only what you pasted — these are described precisely enough to paste directly into
`OTEngine.java`.

**3. `ExecutionResultPayload.java` vs `ExecutionResult.java` — field name mismatch across the
RabbitMQ boundary.** `execution-service`'s `ExecutionResult` has a field called `statusType`;
`websocket-service`'s `ExecutionResultPayload` (what `ExecutionRecordConsumer` deserializes into)
has a field called `status`. Since Jackson binds by JSON property name, `status` never populates —
it silently stays `null` on every execution result that crosses into websocket-service, which is
why the frontend's `result.status === 'COMPLETED'` check never matches and nothing appends to the
terminal live, even after your own fix routes it through Redis correctly. The REST fallback
(`GET /api/execute/status/{jobId}`) works because it reads straight from Postgres in
execution-service, never crossing this boundary. Cleanest fix, one field + one call site, no
frontend changes needed: in `execution-service`, rename `ExecutionResult.statusType` → `status`,
and update the one caller (`CodeExecutionWorker.java`: `job.setStatus(result.getStatusType())` →
`result.getStatus()`).

## Known gotcha, already fixed

`sockjs-client` (used for the collaboration WebSocket) references the bare Node.js `global`
identifier at module-load time, which doesn't exist in a browser and crashes the entire app before
React even mounts — since routes are imported eagerly, this hits on *every* page load, not just the
IDE page. Fixed with a one-line polyfill as the first thing in `index.html`
(`window.global = window.global || window`), which runs before the module graph loads at all. If
you ever see a blank page with nothing obvious in the console, check that script is still first —
a `vite.config.ts` `define` shim alone does *not* reliably cover this, since Vite's dev-mode
dependency pre-bundling doesn't consistently inherit it (confirmed by inspecting the actual
pre-bundled output, not just assumed).

There's also a top-level `ErrorBoundary` (`src/components/ErrorBoundary.tsx`) wrapping the app, plus
`window.onerror` / `unhandledrejection` listeners in `main.tsx` — so a *future* crash shows a real
message on screen and in the console instead of a silent blank page.

## Fixed: multi-tab testing corrupted sessions

Found while testing real-time collaboration with two tabs in the same browser (exactly the right
way to test it) — but `localStorage` is shared across every tab of the same origin. Logging into a
second tab with a different account silently overwrote the first tab's refresh token; the first
tab's next silent-refresh then picked up the wrong account's token and started operating as the
wrong user without anyone knowing.

**Fixed** by moving the refresh token to `sessionStorage` (`src/lib/refreshTokenStorage.ts`), which
is scoped per-tab. Each tab now keeps its own independent session — log in as a different account
in each tab and they stay independent. Trade-off: closing a tab and opening a *new* one means
logging in again there (a refreshed/reloaded tab is unaffected). Also added a defensive check in
the refresh interceptor: if a refreshed token's user id ever doesn't match the currently-displayed
user, it forces a clean re-login instead of silently continuing under the wrong identity.

Two other bugs surfaced during that same test, both fixed:
- **Dashboard crash** (`can't access property "toLowerCase", ws.visibility is null`) — some
  workspace the second account could see came back with a null `visibility`. Wasn't defensive
  about it before; now falls back to `"unknown"` instead of crashing. Worth knowing this can happen
  with real backend data, not just the happy path.
- **403 on a workspace you're not a member of** now shows a dedicated "Access denied" page instead
  of a jumble of toasts and a subsequent Dashboard crash — a 403 on the primary resource means "you
  don't belong here," which is a different situation from a transient error where retrying makes
  sense.

## Fixed: execution results never arriving in the terminal

Your execution-service log confirmed the job completed and was handed to the WebSocket router, but
it never showed up in the frontend terminal. Given the file-tree endpoint already turned out to use
different field names than documented, I'm no longer trusting the docs' exact `jobId` field name
for the terminal payload either — the match now checks both `jobId` and a plain `id`, and every
incoming terminal message is logged to the console (`[Nexis] Terminal message received: ...`) so if
it's still not arriving, the actual payload shape is one console check away instead of another
guessing round.

## Fixed: `/api/files/undefined/download` (500s)

The raw file objects from the list endpoint don't have a bare `id` key — matching the `fileId`
naming your OTHER storage endpoints use (upload intent returns `{fileId, url}`) rather than the
entity's DB column name. Every file is now normalized to a real `id` at the point it's fetched
(`src/api/files.ts`), checking both `id` and `fileId`, so nothing downstream has to guess. Also
added a hard guard in `fetchFileContent` that throws immediately on an empty id instead of ever
firing a request with `undefined` in the URL again.

## Fixed: OT version tracking (the "wrong line" / "wrong order" bug)

This is the one worth reading carefully. `versionRef.current += 1` was a plain local counter with
zero relationship to your server's actual version number. Your `OperationalTransformService` loop
transforms an incoming op against every historical op whose version is `>=` what the client claims
— so if the client's claimed version is small while the server is really at 85+, that op gets
transformed against far more history than it should. This lines up exactly with your log: a
client-sent `DELETE position: 0` landed server-side as `RETAIN Pos: 85`. That's not a small
adjustment — that's an operation dragged across most of the document's history, which is exactly
the kind of thing that produces "shows up on the wrong line" and "characters in the wrong order."

**Fixed** in `src/components/ide/EditorPane.tsx`: instead of incrementing a local counter, the
client now tracks the last version it's actually seen *confirmed by the server* — from any
operation, its own echo or someone else's — and sends that as the version it's "based on" for the
next op.

**Known residual gap, needs backend support to close fully**: the very first operation in a session
where no other operation has been observed yet still has no way to know the true current server
version (there's no "get current version" endpoint, and the file-content fetch doesn't carry one
either). That first op still goes out versioned at 0. Two ways to close this: a small endpoint to
fetch the current version for a workspace, or including it alongside the file content response.

**Two backend-side things worth a look, shared as actual code separately** — not touched here per
your ask to keep backend fixes out of the frontend:
1. `OTEngine.delete_delete`: the partial-overlap branch shifts `position` correctly but never
   shrinks `length` when historical's deletion partially overlaps incoming's — meaning incoming
   can end up deleting extra characters past what it actually meant to.
2. `OperationalTransformService`'s loop condition (`>=`) — once versions are meaningful (per the
   fix above), this should likely be `>` so an op doesn't get transformed against the very version
   it already claims to be based on.

## Recording service: what's actually Phase 2 vs Phase 3

The session start/end **calls** are Phase 2 and already in place. The `IllegalArgumentException:
Workspace session is already actively recording` you'll see in the recording-service log is
expected, not a bug — it fires whenever a second tab/client connects to a workspace that already
has an active session, and my code already treats that failure as non-fatal (caught, ignored,
collaboration keeps working). It does mean a client that hits that error never learns the *real*
session id, so if that's the tab that ends up leaving last, the session may not close cleanly.
Fixing that properly means the backend making `POST /api/sessions/start` idempotent — returning the
existing session instead of erroring — which ties directly into Phase 3's "welcome back" digest
anyway (that feature needs to know the current session's id regardless of which tab asks), so it's
staying scoped there rather than a partial fix now.

## Fixed in this review pass

This zip was handed back over for a full audit against the docs before continuing into Phase 3.
Everything above this line is unchanged from before; these are new:

1. **Recording session was ending itself almost immediately after starting.** The "end when
   presence hits zero" logic lived inside a `useEffect(() => { return () => {...} }, [presence])` —
   i.e. inside that effect's *cleanup*. Cleanup for a dependency-array effect re-runs on **every**
   change to `presence`, not just on unmount, and it runs with the value from *before* the change
   (a stale closure). So the very first presence event after the session started — almost always
   someone **joining**, not leaving — would fire the cleanup with the old (empty) presence value
   and immediately call `endSession`. Moved the zero-check into the effect body (reacting to the
   live value) and kept a separate ref-backed check for the actual-unmount case.
   (`src/pages/workspace/WorkspacePage.tsx`)
2. **Recording session could start with an empty participants list.** `socket.status` can flip to
   `'connected'` before this client's own `JOINED` event has round-tripped back through the
   presence topic, so reading `Array.from(presence)` at that exact moment could send `[]`. Now
   includes the current user directly, since that id is known with certainty regardless of the
   presence race.
3. **Terminal never visually distinguished errors from normal output** — the doc is explicit that
   `FAILED` results should render "in bold red text"; every line was rendering identically. Lines
   now carry a `kind: 'output' | 'error' | 'system'` and render with a red/bold style for errors,
   dim italic for system messages (`src/components/ide/Terminal.tsx`, `globals.css`).
4. **A pending throttled cursor publish could fire after teardown.** `useWorkspaceSocket`'s cursor
   throttle schedules a `setTimeout`; the hook's cleanup deactivated the STOMP client but never
   cleared that timer, so a scheduled publish could still call `.publish()` on an already-deactivated
   client after switching workspaces or unmounting. Cleared in cleanup now.
5. **Dead prop-drilling in the login/signup handoff.** `SignupPage` already navigated to `/login`
   with `{ prefillEmail }` in router state; `LoginPage` never read it. Wired it up, plus moved
   autofocus to whichever field the user actually needs to fill next.
6. Two small housekeeping items: a stray empty directory literally named
   `{styles,types,lib,...}` (a `mkdir -p a/{b,c}` that ran under a shell without brace expansion)
   and two committed `tsconfig.*.tsbuildinfo` build-cache files — both removed, `.gitignore` updated
   so the latter doesn't come back.
7. Added the one documented workspace endpoint that had no wrapper yet (`transferOwnership`, doc
   §3.5) for parity with the rest of `src/api/workspaces.ts` — nothing calls it yet since there's no
   member-management UI, but it's there when that lands.

Everything else — the auth interceptor, the 3-phase MinIO upload, the OT engine, the storage
signature handling — held up against a fresh line-by-line pass against the docs. `npx tsc --noEmit`
and `npm run build` are both clean.

## Second pass — live bug reports against your running backend

You ran this against your actual services and hit three real problems, and pasted in a second
AI's (Gemini's) diagnosis of the OT bugs. Worth being precise about what I could actually verify
versus what I'm inferring, since the two diagnoses disagree on the mechanism:

**Gemini's specific claims vs. what's actually in this codebase:** Gemini said the frontend
"ignores `op.position` and inserts at the local cursor" and "completely ignores `operationType:
DELETE`". I read `src/ot/otEngine.ts` and `src/components/ide/EditorPane.tsx` fresh, twice, before
touching anything. Neither claim matches the code: `applyRemoteOperation` converts the operation's
absolute `position` (never the local cursor) via `model.getPositionAt()`, and DELETE has its own
branch that builds a real `Range` from `position` to `position + length` and clears it — it isn't
skipped. Gemini was diagnosing from your symptom description and Java-side logs alone, without ever
seeing this code, and produced a plausible-sounding but incorrect story. I'm not saying this to
score a point against another model — it matters because it means the fix isn't "rewrite the apply
logic," which would have been a wasted, risky rewrite of code that was already correct.

**What I actually found and fixed:**

1. **A real, separate bug**: in `EditorPane.tsx`, `isApplyingRemoteRef.current = true` was followed
   by the apply call and then `= false`, with no `try/finally`. If applying a remote op ever threw
   — e.g. from a payload that doesn't match the shape this client expects — that flag got stuck
   `true` forever, which silently disables sending *any further local edits* for that file (the
   content-change handler bails early whenever this flag is true). Wrapped it in `try/finally` and
   added a `console.error` so a failure is loud instead of a silent, permanent stop.
2. **Diagnostics, not a guess-fix, for the position/delete symptoms.** Since I can't attach to your
   running backend from here, I added validation in `otEngine.ts` that logs clearly if a remote
   op's `position`/`length` aren't numbers (the signature of a payload whose field names don't
   actually match the `CodeOperation` TypeScript type it's being cast to) and a `console.warn` if an
   operation targets an offset past the end of the local document. **My leading hypothesis**, given
   the actual code is sound: a client's Monaco buffer is seeded from `GET /api/files/{id}/download`
   (MinIO — the last *saved* snapshot), not from the Collaboration service's live document state.
   Once any edits have happened in a session without a save, a newly-joining or refreshed client is
   working from a shorter/different buffer than the one the server's OT engine is computing absolute
   offsets against — `model.getPositionAt()` doesn't throw on an out-of-range offset, it silently
   clamps to end-of-document, which would look exactly like "insert lands in the wrong place" and,
   for a DELETE whose start and end both clamp to the same point, exactly like "delete does nothing"
   (an empty range replaced with `''` is a no-op). **A cheap test that would confirm or rule this
   out**: have both tabs join a *brand-new empty* workspace and reproduce the insert/delete test
   before either client has any unsaved history to diverge on. If it still happens on a clean
   session, the bounds-check/console output I added will show you the actual payload shape and this
   becomes a five-minute fix instead of a guess. If it *doesn't* happen on a clean session, that
   confirms the baseline-desync theory, and the real fix is a backend endpoint that returns "current
   live document text + current version" for a client to seed from on join/reconnect — there isn't
   one in the docs I was given, and I don't want to invent an endpoint contract for you.
3. **Related gap worth knowing about, not yet hit by your test**: `CodeOperation` (per your docs)
   has no `fileId`, and the topic is `/topic/workspace/{workspaceId}/code` — scoped to the
   *workspace*, not to a file. If two people in the same workspace have *different* files open,
   operations from one will currently get applied to whichever file the other happens to have
   focused, since there's no field to filter on. Only matters once you test multi-file concurrent
   editing; flagging it now so it isn't a surprise later.

**Terminal output not appearing until refresh:** Gemini's generic "you're polling instead of using
the WebSocket" story doesn't match this codebase either — it already subscribes to
`/topic/workspace/{id}/terminal` before any run and never polls. The actual, verifiable bug: the
result handler only updated the terminal if `extractJobId(result) === activeJobIdRef.current`,
and if the real payload's job-id field doesn't match either `jobId` or `id` (both already checked),
that comparison is `undefined !== "<uuid>"` — always false — so the message was silently dropped
every time, and only the refresh path (which fetches by a job id *this client already has locally*,
never needing to parse one out of a push payload) ever showed anything. Changed it so an
unrecognized/missing id field no longer means "hide the message" — it means "show it anyway and log
a warning," since for this UI (one execution in flight at a time) a false positive is far cheaper
than a silent drop. There's already a `console.log` on every incoming terminal message; next run,
check what actually printed and I can tighten `extractJobId` to the real field name in one line.

**Code vanishing on refresh:** Gemini's "hot Redis buffer + checkpoint on save" architecture is the
right shape, but "ask the Collaboration service for the current live buffer on reconnect" needs a
backend endpoint that isn't in the docs I have — I'm not going to fabricate one. What I *can* do
with what's already documented: `uploadFile()` (storage.ts) already implements the full 3-phase
MinIO flow correctly. Wired it up as an actual save path — Ctrl/Cmd+S via Monaco's own command API
(not a window keydown listener, which doesn't reliably fire while the editor has focus), plus a
debounced auto-save (4s after you stop typing, with a 30s hard ceiling so a long uninterrupted
typing session still checkpoints) rather than a fixed interval — agreeing with Gemini's specific
concern that a naive "every 3 seconds regardless" auto-save would hammer Postgres with version rows
for no reason. A save-status indicator ("unsaved changes · ⌘S to save" / "saving…" / "saved" /
"save failed") sits next to the workspace name, and closing/refreshing the tab with unsaved changes
now prompts a confirmation.

**One thing I could not verify and want to be upfront about**: `uploadFile()` was written for
*creating* a file (boilerplate seeding, "new file" in the tree) — it always runs a fresh Phase-1
intent for the given filename. Whether re-uploading to an *existing* filename makes the backend
version the same file, or provisions a second file entity with the same name, isn't something I can
check from here — your docs don't cover re-upload semantics. After your first Ctrl+S, check the
file list: one `Main.java` with updated content is what I'm hoping for; two `Main.java` entries
means this needs a real update-in-place path (passing the existing `fileId` into phase 1, or a
dedicated endpoint) rather than reusing the create flow.

## Third pass — version conflict, manual save, and the delete bug's real source

**`FileVersionConflictException` on every save after the first — root cause confirmed, fixed.**
Your log nailed it: phase 1 correctly returned a new version ("Successfully generated presigned PUT
URL for File ID: ..., Version: 2") but phase 3's commit one line later still said "Version: 1" — the
exact version already committed by the first save, which the backend's idempotency check correctly
rejects. `commitUpload()` was defaulting `versionNum` to a hardcoded `1` regardless of caller — it
had no way to do otherwise, since `UploadIntentResponse` per the docs is just `{fileId, url}`, no
version field. Fixed by tracking `currentVersion` on the file itself (already in `WorkspaceFileMeta`,
confirmed by the SQL in your log) and always saving as `currentVersion + 1`, updating local state
from what the server actually confirmed. Also confirms something I'd flagged as unverified last
round: re-uploading to an existing filename *does* version the same file server-side (it looked the
file up by `workspace_id + file_name`) — it was never a duplicate-file risk, just a wrong version
number.

**Auto-save removed, per request.** `EditorPane` now only saves on Ctrl/Cmd+S — no debounce timer.
The version-conflict bug above would have hit either way (it was going to fire on the *second* save
regardless of what triggered it), but agreed it's better to debug save behavior without a timer
also in the mix.

**Terminal still not updating — added a hard timeout, but re-confirmed the subscription is real.**
Re-read `useWorkspaceSocket.ts` fresh: it does subscribe to `/topic/workspace/{id}/terminal` before
any run is possible (line 71 in that file) — that part was never missing. If you tested this against
the *previous* zip rather than the one with the lenient job-id match from last round, that alone
might explain it. Either way, added a proper backstop: if no terminal message arrives within 20s of
starting a run, it now falls back to `GET /api/execute/status/{jobId}` once and shows whatever that
returns, instead of leaving "Executing..." on screen indefinitely. If it's still silent after this
zip, the `console.log`/`console.warn` on every incoming terminal message will tell us whether
messages are arriving with the wrong shape, or not arriving at all — those are different bugs with
different fixes, and the console output is what distinguishes them.

**The delete bug — this one's already-documented backend behavior, not a new frontend issue.**
"Delete 'apple', still see 'a'" matches, almost exactly, the mechanism already written up above
under "Fixed: OT version tracking": a `DELETE position: 0` op that goes out under a version far
behind the server's real one gets transformed against way more history than it should, and your own
earlier log showed this concretely turning a DELETE into a no-op `RETAIN`. That gap — first
operation in a session has no way to learn the true starting version, since there's no
"get current version" endpoint — is still open, and it's backend-side (the transform loop is server
code, not this repo). Did the two backend fixes mentioned in that section
(`OTEngine.delete_delete`'s partial-overlap length bug, and the `>=` → `>` loop condition) get
applied yet? If not, that's very likely where this is actually coming from, and no amount of
frontend defensiveness fixes a position the server itself computed wrong. I added bounds-checking
and console warnings on the frontend last round specifically so this would be visible rather than
silent — worth checking what printed during your delete test.

**Chat is wired up now.** Group thread + a DM thread per workspace member, typing indicator (group
only, per the docs), unread dots on the toggle button and per-thread. Sends optimistically (shows
your own message immediately) and dedupes against the server's echo by fingerprinting `thread +
text` for a few seconds — this is deliberately more defensive than trusting the "server always
echoes the sender" assumption used elsewhere in this app for code ops/presence, since here the
failure mode of guessing wrong (your own messages never appearing) is much worse than the failure
mode of a rare, harmless double-suppress. Private-message destination is still the one genuinely
undocumented piece (`/app/workspace/{id}/chat/private` publish, `/user/queue/chat` subscribe,
mirroring the `/user/queue/errors` pattern the docs do specify) — flag if the real one differs.

## Fourth pass — private messaging contract, pending-queue fix, and open design questions

You shared your own edits to `useWorkspaceSocket.ts`/`EditorPane.tsx`/`otEngine.ts` along with real
backend source. Synced to your versions and fixed on top:

**Private messages — one destination bug.** Your payload mapping (`senderId→userId`,
`content→message`, etc.) and the publish side (`/app/private` with `{senderId, receiverId,
messageType, content}`) both exactly match `WebsocketController.handlePrivateMessage`. The
subscribe destination didn't: `/topic/user/{id}/private` is a literal topic nothing publishes to.
The backend uses `messagingTemplate.convertAndSendToUser(userId, "/queue/private", ...)` — Spring's
user-destination mechanism, which routes by the STOMP session's Principal (set in
`JwtChannelInterceptor`), not by an id embedded in the path. The client-side counterpart is always
the literal string `/user/queue/private` — no id in it. Fixed.

**Pending-queue position shifting — your fix is right, extended it slightly.** Good catch that
queued (not-yet-sent) local drafts need their stored `position` updated when a remote op lands,
otherwise they get sent with a version number claiming "based on the doc *after* this remote op"
while still holding a position computed *before* it. I added the one gap in the DELETE branch: when
a remote delete partially overlaps a *queued* local delete, your version clamps the position but
doesn't shrink the queued delete's `length` to match — mirrors the same overlap logic already in
`OTEngine.java`'s `delete_delete` (`CLAUDE'S FIX`), just applied to the local queue instead of
server history.

**`forceMoveMarkers`-only caret handling** — removing the explicit `setPosition` call to avoid
double-adjustment is a reasonable simplification; Monaco's own cursor tracking does generally
follow model edits without help. I'm not fully certain this covers every case as well as explicit
handling would have, so if you notice a *different* new symptom — your own cursor jumping to an odd
place right after a remote edit lands nearby — this is the first place to look.

**Blank file on open (#2)** — added a distinct error state instead of a silent blank editor
(`contentLoadError`, shown inline with a Try Again button, plus the raw error now hits
`console.error`). This doesn't fix a bug so much as make it possible to tell which bug you're
actually hitting: a genuine fetch failure vs. a file that's blank because nothing was ever saved to
it (see below). Check the console/Network tab next time this happens — if `/api/files/{id}/download`
itself is erroring, that's a real backend bug; if it returns 200 with empty content, the file
genuinely has no saved bytes.

**New files now seed with boilerplate.** The FileTree "add file" flow was creating files with `''`
content — only workspace-creation was seeding boilerplate. Now both do. Java is special-cased: the
boilerplate's `public class Main` gets retargeted to match whatever filename you actually typed,
since Java requires the class name and filename to match or it won't compile.

**Design questions for #3, #6, #7, #8 — my recommendations, not yet built:**

- **#3 (new joiner sees blank instead of in-progress work):** don't build a "force everyone to
  save" workaround — that's fighting the symptom. The real fix is the same "residual gap" already
  documented above: a backend way to seed a joining client from the Collaboration service's *live*
  Redis document state, not the last MinIO save. Given `OperationalTransformService` already keeps
  the full text implicitly reconstructable from `nexis:workspace:{id}:history` (an ordered op list,
  capped at the last 100), the smallest addition would be a REST endpoint like
  `GET /api/workspaces/{id}/live-document` on the Collaboration service that replays that history
  into a single string + returns the current version — a client joining mid-session calls this
  instead of (or in addition to) the Storage service's `/download`. This also directly closes the
  "residual gap" version-seeding problem from `EditorPane.tsx` above, for free, once it exists.
- **#6 (invite via private message):** your own `MessageType` enum already has `INVITE` sitting
  right next to `DIRECT_MESSAGE` and `ERROR` — this looks like it was designed for exactly this and
  just never wired up. Suggested shape: non-owner calls a new endpoint (e.g.
  `POST /api/workspaces/{id}/invite?targetUserId={uuid}`), the Workspace service stores a pending
  invite row and fires a private message with `messageType: INVITE` and enough info in `content`
  (workspace id + name, at minimum, maybe as a small JSON blob) for the frontend to render an
  Accept/Decline action. Owner-initiated add can keep using the existing
  `POST /api/workspaces/{id}/members?memberId=` directly, no invite needed. Once you've built the
  invite endpoint (or confirm this shape), I'll wire up the dashboard's private-message section and
  the accept/decline UI against it.
- **#7 (delete/rename files):** Storage service doesn't document either. Needed:
  `DELETE /api/files/{fileId}` and something like `PUT /api/files/{fileId}/rename` (body:
  `{newFileName}`) or a `PATCH`. Both are straightforward CRUD additions to the existing `files`
  entity — happy to wire up the frontend the moment either exists, or to take a guess at the exact
  contract if you'd rather I just build against an assumption and you adjust the backend to match.
- **#8 (Time Machine / welcome-back digest):** `GET /api/sessions/{id}/events` is already wrapped
  in `src/api/sessions.ts` and unused. This is a big enough feature (a new route, a read-only Monaco
  instance driven by `executeEdits()` on a playback timer, plus the digest aggregation) that it
  deserves its own focused pass rather than being squeezed in here — say the word and I'll start on
  it next.

## Phase 1 — Foundation & Auth

- Project scaffold, "Phosphor" design system (amber-terminal theme, see `src/styles/globals.css`)
- Auth: signup, login, OAuth2 (Google/GitHub), forgot/reset password
- **Stay-logged-in flow**: access token lives only in memory (`src/store/authStore.ts`); the mutex
  refresh interceptor in `src/api/client.ts` transparently refreshes on 401/410; `AuthProvider`
  silently re-authenticates from the refresh token on every page load/reload
- Dashboard: workspace list + create, with language-aware boilerplate seeding through the real
  3-phase MinIO upload flow

## Phase 2 — The actual IDE

- **Live collaborative editor**: Monaco wired to the collaboration WebSocket. Local edits queue
  through the documented "pending buffer" (one unconfirmed op in flight at a time — see
  `src/components/ide/EditorPane.tsx`); remote ops are played back onto the model with caret
  preservation. `src/ot/otEngine.ts` is explicit about the boundary: **the transform itself is your
  backend's job** — this module only converts Monaco's edits to the wire format and plays back
  whatever the server broadcasts.
- **Live cursors**: throttled to 100ms per the docs, rendered as colored carets + name labels
  (`src/components/ide/remoteCursors.ts`); colors are assigned deterministically from user id so
  they're stable across a session without a backend-assigned color.
- **Presence bar** + connection status, resolved to real names via the workspace member roster.
- **File tree**: real list + open-on-click + add-new-file, built on the assumed list/content
  endpoints (see Integration notes below).
- **Execution terminal**: Run sends the editor's *live* buffer (not a stale snapshot) to
  `/api/execute/run`, then goes silent and waits for the WebSocket result — no polling, per the
  documented flow. Stale/foreign job results are filtered by jobId. Refreshing mid-run rehydrates
  from `GET /api/execute/status/{jobId}` via a job id cached in localStorage, matching the doc's
  described fallback path.
- **Recording session lifecycle**: a session starts once the socket connects and ends (best-effort)
  when this client believes it's the last one leaving — see Integration notes, since there's no
  endpoint to confirm that server-side.
- **Save**: Ctrl/Cmd+S only (via Monaco's own command API), checkpointing through the existing
  3-phase MinIO upload flow, now correctly versioned — see "Third pass" above. No auto-save, per
  request.
- Monaco loads from its default CDN (via `@monaco-editor/react`), not bundled locally — keeps the
  JS bundle small (~355KB) and needs zero worker configuration. Trade-off: needs internet at
  runtime. Switching to a fully offline bundle is a contained follow-up (worker config +
  `loader.config({ monaco })`) if you want it, not a rebuild.
- **Chat**: workspace-wide group thread + a DM thread per member, typing indicator, unread
  tracking. See "Third pass" above for the one still-undocumented piece (the private-message
  destination).

## Not yet built (Phase 3)

- Time Machine playback and the Recording-service "welcome back" digest. `GET /api/sessions/{id}/events`
  is already wrapped in `src/api/sessions.ts` and unused — see "Fourth pass" above for the plan.
- File delete/rename in the tree — blocked on two small backend additions, see "Fourth pass" above.
- Workspace invites (owner adds directly; non-owner request goes out as a private message) — blocked
  on one backend endpoint, see "Fourth pass" above. The dashboard private-message section this
  implies isn't built yet either.
- A backend endpoint to seed a joining client from the Collaboration service's live document state
  instead of the last MinIO save — see "Fourth pass" above; this is what would fully close both the
  blank-file-for-a-new-joiner issue and the OT "residual gap" noted in `EditorPane.tsx`.

## Integration notes — please confirm or correct

Your docs are thorough but two different generations of them are mixed together, and a few things
the frontend needs aren't documented at all. Everything below is a place where a wrong guess would
actually break a feature, not just a style choice.

**Resolved conflicts (going with the more detailed/current-looking doc):**
- Signup is `POST /api/auth/signup` and does **not** auto-login (matches your detailed doc). The
  original planning doc's `AuthController` sample uses `/register` and logs the user in
  immediately — if your backend actually does that, tell me and I'll flip Signup to auto-login.
- Live code sync: your Collaboration doc gives **two different specs** for the same feature —
  `/topic/.../code` + `/app/.../code` with a single `{position, length, code}` op (fully documented,
  has a working example hook — **this is what Phase 2 builds against**), vs.
  `/topic/.../operations` + `/app/.../operate` with a Quill-style `{retain, insert, delete}` delta
  array. The transport lives in `useWorkspaceSocket.ts` and the op shape in `src/ot/otEngine.ts`,
  so swapping is contained if the second spec is actually current.
- Same split for the connection itself, across three doc sections: `ws://localhost:8082/ws` (direct
  to the Collaboration service — matches the spec Phase 2 builds against), `ws://localhost:8082/ws/ide`
  (same port, different path, paired with the delta-array spec above), and
  `ws://localhost:8080/ws` (through the gateway, in the Execution doc). Went with the first —
  `src/lib/config.ts`'s `WS_URL` defaults to `http://localhost:8082/ws` (an `http://` URL is
  correct here, not `ws://` — SockJS does its own upgrade). This bypasses the gateway by design;
  `JwtChannelInterceptor` must be validating the JWT at the Collaboration service itself, not at
  the gateway. If WS actually needs to go through 8080, it's a one-line `.env` change.

**Gaps I filled in — these are guesses, not confirmed contract:**
- No endpoint to list a workspace's files or fetch a file's content. Built against
  `GET /api/files/workspace/{id}` (list) and `GET /api/files/{id}/download` returning `{ url }` —
  a presigned GET URL, mirroring the upload flow's own architecture — rather than proxied bytes.
  (`src/api/files.ts`)
- No private-chat destination documented — Phase 3 will add `/user/queue/chat` alongside the
  existing `/user/queue/errors` pattern, unless you have a real one.
- `POST /api/execute/run` takes one `code` string, not a file reference — so Run executes whichever
  file is open in the editor. Cross-file imports (`Main.java` importing `Helper.java`) won't work
  until the endpoint accepts a file id or a file list.
- No "get the active session for this workspace" endpoint, and no "everyone left" signal from the
  server. Session end is currently a client-side heuristic (fires when this client's local presence
  set hits zero) — the server's own Redis presence tracking would be a more reliable source of
  truth if you want to expose it.

**Not a gap, just a design call:**
- Workspace creation has no `language` field, so no backend change is needed for language-aware
  boilerplate — the frontend uses the language choice once, client-side, to decide which starter
  file to push through the existing upload flow.

## Folder structure

```
src/
  api/            axios client (refresh mutex), one file per backend service
  components/
    ide/          FileTree, EditorPane, PresenceBar, Terminal, remote cursor decorations
    layout/       Navbar, AuthLayout (the split-screen auth shell)
    ui/           Button/Input/Select/Modal/Toast — no external UI kit
  hooks/          useWorkspaceSocket — STOMP connection + subscriptions for one workspace
  languages/      per-language boilerplate content + extension → language detection
  ot/             client-side op conversion + remote-op playback (NOT the transform algorithm)
  pages/          route-level components, grouped by feature
  providers/      AuthProvider (silent relogin on load)
  routes/         ProtectedRoute guard
  store/          zustand stores
  styles/         globals.css — the whole design system lives here
  types/          api.ts — single source of truth for backend DTO shapes
```
