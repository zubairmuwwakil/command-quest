# Lessons as Guidance, Not Gates — Design

**Date:** 2026-08-09
**Status:** Approved (design), pending implementation plan
**Branch:** `main`

## Context

Command Quest's web front end shows four lesson tabs. Selecting one does two
things: it changes the teaching text on the left, and it decides which command
the player is permitted to type. The second is not a design choice anyone made
deliberately — it falls out of how requests are routed.

`CommandController` holds a dispatch table keyed by lesson:

```java
private final Map<String, BiFunction<Navigator, String, CommandResult>> lessons = Map.of(
        "touch", (nav, input) -> touch.execute(nav.current(), input),
        ...);

BiFunction<Navigator, String, CommandResult> lesson = lessons.get(request.lessonId());
```

The selected tab picks *which single command object ever sees the typed line*.
So with `touch` selected, typing `mkdir homework` is handed to
`TouchCommand.execute()`, fails the keyword check at `AbstractCommand:76`, and
comes back as:

> Not quite - the format was off.
> Try: touch chicken.leg

The message is wrong in the way that matters most for a teaching tool. The
player's format was perfect. They typed a real command correctly and were told
they had made a syntax error. Worse, the correction steers them away from the
thing they just got right.

This is a routing accident with pedagogical consequences. Nothing in the domain
layer causes it: `execute()` returns a `CommandResult` and has never heard of a
lesson. The fix is contained to the web layer and the front end.

## Goals

- Any command the game understands works at any time, regardless of which
  lesson is on screen.
- The lesson panel becomes reference material that reacts to the player rather
  than a gate that constrains them.
- Progress records what the player actually did.
- The unknown-command case gets a real answer, because opening the input means
  players will type things the game has never heard of.
- The console application continues to work, unchanged, from the same domain
  code.

## Non-goals

- **The console application keeps its numbered menu.** `App.java`'s `1/2/3/4`
  prompt has the same shape as the web tabs, but the console is a different
  product with a different interaction model, and changing it means rewriting
  `App`'s main loop plus the `run()` method on every command. Out of scope.
- No new persistence. The server stays stateless; the browser keeps owning the
  tree, the location, and progress.
- No shell features beyond the five commands and `help` — no pipes, no globs,
  no `rm`, no `clear`.

## Architecture

One new class. `Shell` takes over dispatch; `CommandController` goes back to
being an HTTP adapter.

```
CommandController   HTTP only: unwrap the DTO, call Shell, wrap the response
        │
      Shell         parse the first token, dispatch, answer the unknown   ← new
     ╱  │  ╲
Touch  MkDir  View  Cd  Pwd
```

`Shell` lives in `commands/`, alongside `CdCommand`, which already reaches into
`hub` for the `Navigator`. Its surface:

```java
public record Result(String commandId, CommandResult result) {}

public Result execute(Navigator navigator, String input)
public List<String> keywords()
```

`commandId` is the keyword that ran, or `null` when the input matched nothing.

### Why a new class rather than editing the controller in place

The minimum change is one line — key the existing map on `input.split()[0]`
instead of `request.lessonId()`. That much would work.

What makes it insufficient is everything that has to come with it: what to say
when someone types `rm`, whether `mkdr` is close enough to `mkdir` to suggest
it, what `help` prints, and in what order the vocabulary is listed. That is
roughly seventy lines of *game* logic, and left in the controller it can only
be exercised by booting `@WebMvcTest` and asserting on JSON paths.

`CommandController`'s existing comment draws the right line —

> Adapting differently shaped things into one interface for routing is a
> boundary concern, so it lives here rather than in the domain.

— and that reasoning holds for *routing*. It does not extend to deciding what a
beginner should read when they guess wrong. `Shell` is testable in plain JUnit
with no Spring context, and it is where a future console rewrite would plug in.

### The registry must be ordered

`CommandController` currently builds its table with `Map.of()`, which makes no
ordering guarantee. That is invisible today because the front end reads its tab
order from `/api/lessons`, whose `Lesson.catalogue()` uses a `LinkedHashMap`.

Once `Shell` prints its own vocabulary — in the unknown-command reply and in
`help` — the order becomes user-visible, and `Map.of()` would produce a list
that reshuffles between JVM runs. `Shell` uses a `LinkedHashMap` in the same
order as `Lesson.catalogue()`: `touch, mkdir, ls, cd, pwd`.

## Dispatch

`Shell.execute` takes the first whitespace-separated token of the trimmed input
and looks it up. On a hit, the whole original line is passed to that command
exactly as today — commands keep parsing their own arguments, and none of them
change.

The keyword check inside `AbstractCommand:76` (`!tokens[0].equals(keyword)`)
becomes unreachable as a failure, because `Shell` only routes to a command when
that token already matched. **It stays in place**: it is what keeps `execute()`
correct for the console, which calls it directly with arbitrary input.

### `*` stops leaking into the browser

Typing `*` in the web terminal currently produces *"Returning to the main
menu…"*, in a browser that has no main menu. It is a console-ism reaching the
wrong front end through the shared `execute()` path.

`*` is not a registry key, so under keyword dispatch it never reaches a command
and falls into unknown-command handling on its own. The console's `run()` loop
still hits the `*` branch because it calls `execute()` directly. The leak closes
with no code deleted and no risk to the console.

## Unknown commands

```
> rm notes.txt
I don't know "rm" yet. Try: touch, mkdir, ls, cd, pwd, or help.

> mkdr homework
I don't know "mkdr". Did you mean "mkdir"?
```

Returned as `Outcome.REJECTED` with `commandId = null`, so it is an ordinary
`200` with `correct=false` — never an HTTP error. A beginner mistyping a command
is not a protocol violation.

`hint` is `null` when a suggestion was made (the message already carries the
correction) and `"help"` otherwise, so the front end's existing
`` print(`Try: ${data.hint}`) `` renders *"Try: help"*.

Blank input is a distinct case. `"".trim().split("\\s+")` yields a single empty
token, and quoting it produces `I don't know "" yet.` — so `Shell` checks for
blank first and returns the vocabulary message with no quoted token:
*"Type a command, or `help` to see what I know."* The front end already refuses
to send empty lines, so this guards `Shell` as a directly-callable class rather
than fixing a reachable bug.

### Near-miss detection

Damerau-Levenshtein distance — the Levenshtein recurrence plus a transposition
case, about three extra lines — against each keyword, taking the closest within
a threshold that depends on the keyword's length:

| Keyword length | Threshold |
|---|---|
| ≤ 3 | 1 |
| ≥ 4 | 2 |

Both halves are load-bearing:

- **Transposition must cost 1**, or `sl` → `ls` scores 2 and gets no
  suggestion. Transposing two adjacent letters is among the most common typos,
  and plain Levenshtein overcharges for it.
- **The threshold must depend on length**, or a flat 2 makes `ls` → `cd`
  (distance 2) suggest a completely unrelated command.

Ties break on registry order, which is deterministic by the previous section.

## `help`

Handled by `Shell` directly, not routed to a command. Returns `SUCCEEDED` with
`commandId = "help"`.

```
Commands I know:
  touch <name.ext>   make a file
  mkdir <name>       make a folder
  ls                 list what is in this folder
  cd <name>          change folder — also cd .. and cd /
  pwd                show where you are
```

`"help"` has no entry in `Lesson.catalogue()`, so the front end credits no
progress and moves no panel for it. That falls out of the design rather than
needing a special case.

The one-line summaries are duplicated from the lesson text rather than read out
of the command objects. This is the same trade-off `Lesson.java` already
documents and accepts: exposing `AbstractCommand`'s private fields through
accessors purely to feed a usage line would push presentation back into the
domain. A shell describing its own vocabulary is the shell's job.

## `pwd`

A new `PwdCommand`, shaped like `CdCommand`: it takes the `Navigator`, and
deliberately does **not** implement `Command`, whose contract is
`execute(Folder, String)`. A `Folder` cannot report where it sits, because
`Folder` holds no parent reference — an invariant the stateless API depends on.

```java
public CommandResult execute(Navigator navigator, String input)
```

Correct input is exactly `pwd`; anything else is `REJECTED` with hint `"pwd"`.
Success returns `navigator.breadcrumb()` — e.g. `root/photos/2024`.

Adds a fifth entry to `Lesson.catalogue()`, which makes progress `/5`. The
front end mostly derives this already (`lessonIds.length || 4`); the hardcoded
fallbacks at `app.js:160` and `index.html:57` become `5`.

## API changes

### `CommandRequest` — `lessonId` is removed

The field, its `@NotBlank`, and the `"Unknown lesson: …"` throw all go. Nothing
dispatches on it any more, and a request field the server ignores is worse than
no field.

### `CommandResponse` — `commandId` is added

The keyword that ran, or `null`. It drives both progress crediting and panel
auto-follow on the client.

### Deploy ordering is a real constraint

The front end (GitHub Pages) and the server (Render) deploy independently, so
the two directions are not symmetric:

| Case | Result |
|---|---|
| Old front end (sends `lessonId`) → new server | **Fine.** Spring Boot's Jackson default is `FAIL_ON_UNKNOWN_PROPERTIES=false`; the extra field is ignored. |
| New front end (omits `lessonId`) → old server | **400.** The old `@NotBlank` rejects it. |

**Render must deploy before GitHub Pages.** The phase order below enforces this.

### Dead code

`CommandController.lessonIds()` is removed. Its comment claims it exists so the
front end knows what it may ask for, but the front end reads `/api/lessons`,
and nothing in `src/` calls it.

## Front end

All in `docs/app.js` unless noted.

| Change | Detail |
|---|---|
| Drop `lessonId` from the POST body | It no longer exists on the wire. |
| Credit progress by `data.commandId` | Replaces `state.lessonId` at `app.js:298`. Only credits ids present in `lessons`, so `help` is naturally excluded. |
| Auto-follow the panel | On any response with a non-null `commandId` that has a lesson, call `selectLesson(commandId)` — on failure as well as success. |
| Command history | `history[]` plus an index; `ArrowUp`/`ArrowDown` on the input recall previous lines. |
| Progress denominator | The `\|\| 4` fallback at `app.js:160` becomes `\|\| 5`; `0/4` → `0/5` at `index.html:57`. |
| Opening hint | *"Pick a lesson, then type the command"* is now false. Becomes *"Type a command, or `help` if you get stuck. The lessons on the left are there whenever you want them."* |
| Tab `aria-selected` | Still tracks the visible lesson. It now means "what you are reading", not "what you are allowed to type" — which is what the attribute meant all along. |

**Auto-follow fires even after a manual tab click.** Click `cd` to read it, type
`touch x.txt`, and the panel moves to `touch`. Accepted deliberately: the rule
stays one sentence long, there is no hidden per-session mode, and the panel
always shows the command actually in play. The alternative — suppressing
auto-follow once a tab has been clicked — gives two players different behaviour
with nothing on screen explaining why.

Failure is the case that earns this feature. Typing `mkdir` with no argument
while reading the `touch` lesson is the exact moment the `mkdir` lesson is worth
showing.

## What deliberately does not change

- Every command class. `TouchCommand`, `MkDirCommand`, `ViewCommand` and
  `CdCommand` are untouched; `AbstractCommand` is untouched.
- `App.java`, `LoginScreen`, `UserManager`, `User`.
- `Folder`, `Navigator`, `FolderDto`, `CommandResult`.
- The stateless request model, the account/gate flow, and the cold-start warm-up.

## Testing

### New — `ShellTest`, plain JUnit, no Spring

| Case | Expectation |
|---|---|
| Every keyword dispatches to its command | Five cases, each succeeding on correct input |
| A command runs with no lesson context whatsoever | `Shell` has no lesson parameter to pass — this is structural, and the test documents it |
| Unknown command | `REJECTED`, `commandId` null, message names the vocabulary, `hint` is `"help"` |
| `mkdr` | Suggests `mkdir` |
| `sl` | Suggests `ls` — the transposition case |
| `xyzzy` | No suggestion; falls back to the vocabulary list |
| `ls` against `cd` | No suggestion — the false-positive guard |
| `help` | `SUCCEEDED`, `commandId` `"help"`, lists all five |
| `*` | Treated as unknown, never *"Returning to the main menu"* |
| Empty and whitespace-only input | Rejected without throwing |
| `keywords()` order | Matches `Lesson.catalogue()` order |

### New — `PwdCommandTest`

Reports root as `root`; reports a nested location as `root/photos`; rejects
`pwd extra` and bare `pw`.

### Updated — `CommandControllerTest`

- `body()` drops its `lessonId` parameter across all call sites.
- *"an unknown lesson is a 400"* inverts into **"an unknown command is a 200
  with a helpful message"** — the old test asserted the bug.
- *"a missing command is rejected by validation"* keeps its meaning but its
  fixture loses `lessonId`.
- **New regression test, the one that names this change:** posting
  `mkdir homework` succeeds and returns `commandId: "mkdir"`, with no lesson
  field in the request at all. Under the old code this was unreachable.
- New: the response carries `commandId` on success.

### Unchanged

`AppTest`, `UserManagerTest`, `FolderTest`, `NavigatorTest`, `FolderDtoTest`
and the four command tests should all pass untouched. If any of them fails,
this design has overreached and that is the signal to stop.

CI already fails the build on skipped tests (`19cdb09`), so nothing here can be
quietly disabled later.

## Phases

Ordered so the deploy hazard cannot fire.

1. **`Shell` + `ShellTest`.** Server-side only, nothing wired up, no behaviour
   change. The suite stays green throughout.
2. **`PwdCommand` + `PwdCommandTest` + the `pwd` lesson entry.** Still not
   reachable from the UI.
3. **Wire `CommandController` to `Shell`.** Remove `lessonId`, add `commandId`,
   delete `lessonIds()`, update `CommandControllerTest`. *The server now accepts
   both old and new request shapes.*
4. **Deploy the server.** Render, from `main`.
5. **Front end.** `app.js` and `index.html`. Verify locally against the
   deployed server before publishing.
6. **Deploy the front end.** GitHub Pages.

Steps 3–4 must land before 5–6. Phases 1 and 2 are independent and could be
done in either order.

## Risks

| Risk | Mitigation |
|---|---|
| Front end published before the server, breaking the live site | Phase ordering. Phase 3 leaves the server accepting both shapes, so there is no window where either half is broken. |
| Auto-follow feels like the panel is fighting the player | Accepted, and reversible in one line if it does. The failure case is where it earns its place. |
| Suggestion threshold produces a confusing wrong guess | Length-dependent threshold plus the explicit `ls`/`cd` false-positive test. |
| Vocabulary order reshuffles between runs | `LinkedHashMap` in `Shell`, asserted by `keywords()` order test. |
| `pwd` is scope creep on a bug fix | It is small, self-contained, and phase 2 is independently revertible. |

## Open questions

None. All four scope questions and both design questions were settled before
this document was written.
