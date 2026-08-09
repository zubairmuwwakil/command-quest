# Cold-Start Boot Screen — Design

**Date:** 2026-08-09
**Status:** Approved (design), pending implementation plan
**Applies to:** `docs/` (the GitHub Pages front end)

## Context

The front end is served from GitHub Pages and the API from a free Render
container. Render stops a free container after about fifteen minutes idle and
takes roughly thirty seconds to start it again. `DEPLOY.md` describes this as a
known cost and claims the warm-up ping hides it.

It does not hide it. It hides the *delay* from a player who reads before
typing; it does not tell anybody that a delay is happening. A visitor arriving
at a cold URL sees a page that appears finished and inert, and the only way to
learn the server has since woken is to reload by hand.

### What is actually broken

Three defects compound, which is why the symptom reads as "the page is static"
rather than "the server is slow".

**1. The error message cannot be seen.** `#status` is declared inside
`<div id="game" hidden>` (`docs/index.html:65`). At boot, `#game` is still
hidden. `status()` sets `bar.hidden = false`, but `hidden` on an ancestor wins,
so the string *"Cannot reach the server, so lessons could not load. Refresh to
retry."* is written into an element that renders nothing. The one message
telling the player what to do has never once been displayed.

**2. There is a single attempt and no retry.** `boot()` awaits
`GET /api/lessons` exactly once (`docs/app.js:321`). Against a sleeping
container that request either hangs or returns 502. On failure `lessons` stays
`{}` and `lessonIds` stays `[]`, permanently, until a manual reload. This is
the reload the player is forced to discover for themselves.

**3. The gate then misrepresents the state.** `showGate()` runs regardless of
the outcome, so all three buttons appear ready. Choosing *Continue as guest*
calls `startSession`, which calls `selectLesson(lessonIds[0] || 'touch')`,
which hits `if (!lessons[id]) return` (`docs/app.js:164`) and abandons the call
without a word. The player lands in the game shell with no lesson text, no
tabs, and `0/4` progress — a screen that looks broken rather than warming.

### Why a fetch cannot simply be waited on

Render does not reject requests to a sleeping container. It accepts the
connection, holds it open while the container boots, and then proxies it
through. So `fetch` does not settle in thirty seconds and report a problem — it
sits unresolved, and the browser's own network timeout is measured in minutes.

"Is the server awake yet?" therefore cannot be answered by awaiting the
promise. The page has to impose its own per-attempt deadline and retry. This is
also what allows a staged display to advance instead of freezing on one line.

Retrying is cheap: aborting and re-requesting does not restart the container.
The second request queues against the boot already in progress and costs one
round trip, not one cold start.

## Goals

- A visitor never sees a still page without an explanation of what is happening
- The app proceeds on its own when the server answers; no manual reload, ever
- The waiting state animates, so "working" is visibly distinct from "frozen"
- A genuinely dead API is distinguishable from a merely sleeping one
- No build step introduced into `docs/`; no runtime dependencies added
- The new logic is covered by tests, to the standard the rest of the repo sets

## Non-goals

- Removing the cold start. That is a hosting-plan decision, unchanged by this
  work and already discussed honestly at the end of `DEPLOY.md`.
- An uptime pinger. `DEPLOY.md` argues against one on a free tier and that
  argument still stands.
- Server-side changes. `/api/health` and `/api/lessons` are used exactly as
  they are today.

## Design

### Placement: the gate stays usable

The waiting UI sits inside the gate card, beneath the three buttons, and the
buttons stay live throughout.

This follows from a fact about the app rather than a preference: accounts are
entirely local. `store.users()` and `store.save()` read and write
`localStorage`, and `docs/app.js` says so in its header — *"No password or code
is ever sent to the server."* Signing up and logging in work perfectly against
a sleeping API. Disabling them would withhold a gate that functions, and would
spend the player's thirty seconds on nothing.

### The boot panel

Styled as a terminal boot sequence, which suits a game that teaches the command
line and, more usefully, lets each line correspond to a real network call
rather than to decoration.

```
┌───────────────────────────────┐
│      Command Quest            │
│  Learn the CLI by using it.   │
│                               │
│  [ Make an account ]          │
│  [ Log in ]                   │
│  [ Continue as guest ]        │
│                               │
│ ╭─ waking server ─────── 14s ╮│
│ │ ✓ contacting api          ││
│ │ ▸ loading lessons█        ││
│ ╰───────────────────────────╯│
│  Free plan — the server       │
│  sleeps when idle. ~30s.      │
└───────────────────────────────┘
```

Markup added to `docs/index.html`, inside `.gate-card`:

```html
<div id="boot" class="boot" hidden>
  <div class="boot-head">
    <span class="boot-title" id="boot-title">waking server</span>
    <span class="boot-elapsed" id="boot-elapsed" aria-hidden="true">0s</span>
  </div>
  <ol class="boot-steps" id="boot-steps" role="status" aria-live="polite">
    <li data-step="contact">contacting api</li>
    <li data-step="lessons">loading lessons</li>
  </ol>
  <p class="boot-note" id="boot-note">
    Free plan — the server sleeps when idle. The first visit takes about
    30 seconds.
  </p>
  <button id="boot-retry" class="btn btn-sm" hidden>Try again</button>
</div>
```

Two decisions are encoded in those attributes.

`aria-live="polite"` is on the step list, not on the panel. Had it wrapped the
whole panel it would include the elapsed counter, and a screen reader would
announce a new number every second for up to ninety seconds. The counter
carries `aria-hidden="true"` for the same reason: it is reassurance for a
sighted user and noise for everyone else. The step transitions are the content
worth announcing.

The panel starts `hidden` and is revealed only if the sequence is still running
after **800 ms**. A warm server completes in well under that, and flashing
*"waking the server"* at somebody whose server was never asleep is worse than
showing nothing. `docs/app.js:267` already uses a 1200 ms delay for the same
reason on the command path.

### Stages

| Step | Work | Marks |
|---|---|---|
| `contacting api` | `GET /api/health` until 200 | `▸` active → `✓` done → `✗` failed |
| `loading lessons` | `GET /api/lessons`, parse JSON | as above |

A step is dim while pending, shows `▸` and a blinking block cursor while
active, `✓` in `--accent` when done, `✗` in `--error` when failed.

### Retry engine

```js
const DEADLINE        = 90_000;                        // total budget
const ATTEMPT_TIMEOUT = 15_000;                        // per request
const BACKOFF         = [1000, 2000, 4000, 6000, 8000]; // last value repeats
```

Each attempt uses `AbortSignal.timeout(ATTEMPT_TIMEOUT)`. On abort or a
non-200, the engine waits the next backoff interval and retries, until the
elapsed time passes `DEADLINE`.

`AbortSignal.timeout()` rather than an `AbortController` paired with
`setTimeout`: it expresses the same intent without the cleanup bookkeeping, and
it is within the browser baseline the project already assumes — `app.js:115`
uses `structuredClone`, which requires Chrome 98 / Safari 15.4, while
`AbortSignal.timeout` arrived in Chrome 103 / Safari 16.

Elapsed time is measured with `performance.now()`, not `Date.now()`.
`Date.now()` tracks the wall clock, so an NTP correction or a laptop resuming
from sleep mid-boot can make the counter jump backwards or leap forward.
`performance.now()` is monotonic, which is the one property a proof-of-life
counter cannot do without.

### Success, and the handoff

When both stages complete, all marks turn `✓`, the panel hides, and the loaded
lessons are handed to `app.js` through a single callback:

```js
function lessonsArrived(loaded) {
  lessons   = loaded;
  lessonIds = Object.keys(loaded);
  if (!state) return;                       // still at the gate — nothing to update
  if (!lessons[state.lessonId]) state.lessonId = lessonIds[0];
  renderLessonTabs();
  selectLesson(state.lessonId);
  enableTerminal();
}
```

That branch is the whole of the "no reload" requirement. A player still at the
gate finds the lessons simply ready when they arrive. A player who has already
clicked through sees the empty lesson panel and tab strip fill in beneath them,
live.

### The in-game waiting state

A player can choose *Continue as guest* three seconds in and reach a game with
no lessons loaded. Today that is a silent blank panel. Instead:

- The lesson panel reads *"Waiting for the server — lessons will appear here."*
- `#term-input` is `disabled`, with placeholder *"waiting for the server…"*

Disabling the input is the substantive half. An enabled box invites a command,
and that command would `POST /api/command` to a container still booting and
hang with no explanation — reproducing the original complaint one screen
further in. Both states clear when `lessonsArrived` fires.

### Failure at ninety seconds

The active step is marked `✗`, the title becomes `server unreachable`, the note
becomes *"The API may be down rather than asleep."*, and `#boot-retry` is
revealed. The button resets the elapsed timer, the step marks and the title,
then restarts the sequence — doing what the manual reload used to do, without
discarding anything the player has entered.

Ninety seconds is roughly three times the typical Render wake. Past that,
continuing to show a waking animation would be asserting something no longer
supported by evidence.

The deadline covers the sequence as a whole, not each stage separately, and the
gate buttons stay live in the failed state exactly as they do while waiting. A
player can still create an account against a dead API; they simply cannot play
yet, and the panel now says so.

## Structure

`docs/boot.js`, a new classic script loaded before `docs/app.js`.

`app.js` is already 332 lines carrying the gate, lessons, terminal, file tree
and API layer; its own header states that it "owns the whole game state". A
network-liveness state machine with timers, deadlines and abort signals is a
separate concern, and it is the part worth testing in isolation. A second
`<script>` tag keeps the site buildless and matches the existing `window.CQ_API`
global convention at `app.js:19`.

Internally the file separates pure logic from browser wiring, with the sequence
engine receiving its collaborators as parameters:

```js
async function runSequence({ fetchFn, now, sleep, onStage, deadline, attemptTimeout })
```

and a guard at the foot of the IIFE:

```js
// Node loads this file for its tests, where there is no document to wire to.
if (typeof module === 'object' && module.exports) {
  module.exports = { formatElapsed, backoffFor, runSequence };
  return;
}
// ---- browser wiring below ----
```

Injecting `now` and `sleep` is what makes the ninety-second deadline testable
in under a millisecond: a fake clock jumps to 91,000 and a fake `sleep`
resolves immediately. The assertion that matters most — that the engine stops
rather than retrying forever — would otherwise cost CI ninety seconds of real
waiting, which is the reliable way to ensure nobody runs the suite.

## Styling

A `.boot` block in `docs/style.css` built from the tokens already defined at
`:root`: `--panel-2` for the background, `--line` for the border, `--mono` for
the step list, `--accent` for `✓`, `--error` for `✗`, `--ink-dim` for pending
steps and the note.

The cursor blinks via `@keyframes`, wrapped in
`@media (prefers-reduced-motion: reduce)` so it holds steady for anyone who has
asked their system to stop things moving.

## Testing

`test/boot.test.js`, using `node:test` and `node:assert/strict` from the
standard library. No `package.json`, no `node_modules`, no runner dependency;
`docs/` remains as buildless as it is now.

| Test | Catches |
|---|---|
| `formatElapsed` at 0s, 59s, 60s, 1m 52s | the `60s` versus `1m 0s` boundary slip |
| `backoffFor` past the end of the array | `undefined` → `setTimeout(fn, NaN)` → a hot retry loop |
| success path | both stages reported done, lessons resolved |
| retry then succeed | recovers, and sleeps for the intended intervals |
| deadline exceeded | stops and reports failure; does not retry forever |
| stage ordering | `onStage` fires contact-active, contact-done, lessons-active, lessons-done, in order |

Behaviour not covered by the suite, verified by hand and recorded in
`DEPLOY.md`:

| Path | How |
|---|---|
| Cold start | Leave the service idle 15+ minutes, then load the Pages URL |
| Warm start | Reload at once — the panel must not flash |
| Failure and retry | Set `window.CQ_API = 'https://example.invalid'` before `boot.js`, reload |
| Late arrival | Throttle to Slow 3G, choose *Continue as guest* immediately |

### CI

Two steps appended to the existing `build-test` job:

```yaml
      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: "22"

      - name: Test the front end
        run: node --test test/
```

The existing skip guard reads `target/surefire-reports`, so it cannot see a
`test.skip()` in `boot.test.js` — precisely the hole that step exists to close.
The Node runner prints a `# skipped N` summary line, so the guard is extended
to assert that count is zero as well, under `set -o pipefail` so a failure
inside the pipeline is not swallowed.

## Files touched

| File | Change |
|---|---|
| `docs/boot.js` | New. Wake sequence, retry engine, panel rendering. |
| `docs/index.html` | Boot panel markup; `<script src="./boot.js">` before `app.js`. |
| `docs/app.js` | `lessonsArrived`, in-game waiting state, delete the dead `status()` call at line 325. |
| `docs/style.css` | `.boot` block and the reduced-motion guard. |
| `test/boot.test.js` | New. Six tests. |
| `.github/workflows/ci.yml` | Node setup, front-end test run, extended skip guard. |
| `DEPLOY.md` | Rewrite the cold-start section; it currently overstates what the warm-up ping achieves. |

The `status()` call at `app.js:325` is deleted rather than relocated. The other
two calls, both inside `send()`, run while `#game` is visible and are correct
as they stand; only the boot-time call sits in the dead zone, and the boot
panel now covers that case properly.
