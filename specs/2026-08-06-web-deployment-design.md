# Command Quest on the Web — Design

**Date:** 2026-08-06
**Status:** Approved (design), pending implementation plan
**Branch:** `worktree-web-deployment`

## Context

Command Quest is a console application that teaches command-line basics. A
player picks a lesson from a menu, types a real command such as
`touch cat.jpg`, and the program validates the syntax and reports what
happened. It is written in Java 25 and driven by `java.util.Scanner`.

The goal is to publish it as a web application usable from a browser, as a
portfolio piece: a link a recruiter can click that works immediately and is
backed by a repository that reads well.

### Why the current code cannot simply be hosted

`Command.run(Folder, Scanner)` blocks on `scanner.nextLine()` until a human
types. The retry loop in `AbstractCommand.run` holds conversation state on the
call stack — the `done` flag and the program counter *are* the game state.

HTTP inverts this. A request arrives, the server returns a response, and the
thread is released. Nothing survives between requests unless it is stored
somewhere explicit. So the I/O boundary has to change. The domain model does
not.

## Goals

- A playable browser version at a public URL, interactive within ~1 second of
  page load
- The existing OOP domain model preserved and still covered by tests
- The console application continues to work from the same domain code
- Free or near-free hosting
- A repository that reads well: clean history, real tests, useful README

## Revision, 2026-08-08

This spec was written against commit `b9094b3`. `main` has since advanced five
commits to `d9dc5ba`, restructuring the code into `commands/`, `hub/`, and
`user/` packages and adding four features. The sections below are amended
accordingly; the amendments are marked in place.

What changed on `main`:

| Change | Effect on this spec |
|---|---|
| `cd` implemented via `CdCommand` + `Navigator` | No longer a non-goal. Needs a location field on the wire. |
| `case 5` (progress) deleted from the menu | Non-goal wording was stale, not wrong. |
| `ls` extracted into `ViewCommand` | A third `Command` to port, with quirks (see below). |
| OS-selection prompt removed | Simplifies the front end; nothing to port. |
| `user/` package: `User`, `UserManager`, `LoginScreen` | **Accounts are now in scope** (see below). |

The no-parent-reference tree invariant this spec depends on **still holds** —
`Folder` is unchanged and `Navigator` tracks location in a separate structure.
That was the load-bearing assumption and it survived.

`main`'s test sources no longer compile: they sit in the root package with no
imports, while the classes they test moved into subpackages, producing 200
`cannot find symbol` errors. `main` is a graded submission and is not to be
modified. The imports are fixed **on the web branch only**, because the domain
refactor needs a working suite as its safety net.

## Non-goals

Deliberately excluded to keep scope achievable:

- A server-side user database, password storage, or any credential reaching
  the server (see *Accounts* — the login feature is kept, the server-side
  credential store is not)
- Server-side persistence of game state
- Multiplayer, leaderboards, or social features
- A JavaScript framework or front-end build pipeline

## Architecture

Three layers. The domain layer has no web dependencies and no I/O.

```
BROWSER — static files on a CDN (Netlify or GitHub Pages)
  index.html · app.js · style.css
  Profile gate (local) · game state in memory, mirrored to localStorage
  Renders: lesson panel · terminal log · live file tree · breadcrumb
        │
        │  POST /api/command  { lessonId, command, state, path }
        │  ←──────────────────  { output, correct, hint, state, path }
        ▼
SERVER — container on Render (or Fly.io)
  WEB LAYER      CommandController · LessonController
                 request/response DTOs · FolderDto
                 Maps DTO → domain, calls execute(), maps result back
  DOMAIN LAYER   hub.Folder · commands.Command · AbstractCommand
                 TouchCommand · MkDirCommand · ViewCommand
                 CdCommand · CommandResult
                 No Spring. No Scanner. No System.out.
```

The `user/` package is **not** in the server's domain layer. Accounts live
entirely in the browser — see *Accounts*.

The domain layer is the existing code with its I/O removed. Spring depends on
the domain; the domain depends on nothing.

### Rationale

Splitting the front end from the API is a deliberate response to free-tier
cold starts. Containers on free plans sleep after roughly 15 minutes idle and
take tens of seconds to wake. If the server also served the HTML, a visitor
would stare at a blank page for that whole time.

Serving the page from a CDN makes first paint immediate. The page then warms
the API in the background (see *Cold start* below). The cost is CORS
configuration and two deploy targets, which is a fair trade for a link that
always appears to work.

## Domain refactor

### New type

```java
public record CommandResult(String output, boolean success, String hint) {}
```

`output` is what the player sees. `success` marks whether the command was
accepted. `hint` carries the retry example that `AbstractCommand` currently
prints on a malformed command; it is `null` on success.

The domain field is `success`; the JSON field is `correct`. The DTO layer maps
between them deliberately — the domain describes whether an operation
succeeded, the wire format describes whether the player got the lesson right.
They happen to coincide today and need not always.

### Changed interface

```java
public interface Command {
    CommandResult execute(Folder folder, String input);
}
```

`AbstractCommand.execute` keeps the existing validation order — wrong format,
then already-exists, then create — but returns each message instead of
printing it, and no longer loops. Retrying is the browser's job: the player
simply types again.

The `keyword`, `noun`, `namePattern`, `lesson`, and `example` fields and the
`exists` / `create` template methods are unchanged. `TouchCommand` and
`MkDirCommand` need no changes at all beyond compiling against the new
signature.

### Accounts

The login screen is kept on the web, because it is one of the three declared
features of the graded project and because it is intended to grow into a
learning feature — progress tracked per player.

**Accounts run entirely in the browser. No credential is ever sent to the
server.** The login screen, the account list, and the PIN check all execute in
JavaScript against `localStorage`. The API has no concept of a user.

This is not a downgrade of the feature. It is the correct design for what the
feature actually does:

- Its job is to *identify a player so their progress persists*, not to protect
  anything. There is nothing secret in a lesson-completion count.
- `UserManager` stores accounts in a `HashMap`. On a free-tier container that
  sleeps and restarts, a server-side version would forget every account within
  minutes — a player would register and then be told their username does not
  exist. `localStorage` genuinely persists.
- It needs no database, which keeps hosting free and the deploy simple.

**What must not be built:** the current PIN check is
`pin.equals(entry)` against a plaintext `String`, over a 4-digit keyspace, with
no attempt limit. That is entirely reasonable for a console app on one machine,
and it is a clear illustration of encapsulation. Exposed as a public HTTP
endpoint it would be a real liability — not because this game holds anything
valuable, but because people reuse PINs, and a stranger's bank PIN would arrive
in plaintext and be checked without rate limiting. Keeping the check client-side
means no such credential ever crosses the network.

If cross-device progress is ever wanted, that is the point at which a real
database, hashed credentials, and rate limiting become necessary. It is out of
scope here and should be a deliberate, separate project.

The console app on `main` keeps `LoginScreen` exactly as it is. Nothing about
this design requires changing it.

### Navigation and the `cd` command

`CdCommand` deliberately does not implement `Command`; its contract is
`run(Navigator, Scanner)`. The web layer therefore cannot dispatch everything
through one uniform `Map<String, Command>`.

`Navigator` must **never be serialised.** Verified empirically against Jackson
3.1.4: `writeValueAsString(navigator)` returns `{}` — not an error, an empty
object. Jackson discovers properties via the JavaBean `getX()` convention, and
`Navigator`'s methods are `current()`, `up()`, `toRoot()`, `breadcrumb()`. None
match, the `path` field is private with no accessor, so Jackson finds zero
properties and silently drops the entire navigation state.

Location therefore travels as a list of folder **names**, relative to root:

```json
"path": ["photos", "2024"]
```

The server rebuilds a `Navigator` by walking the deserialised tree from root
through those names, runs the command, and returns the new path. An unresolvable
path segment is a 400 — it means the client's state and the tree disagree.

Serialising `Deque<Folder>` directly would be wrong even if Jackson could see
it: each stack entry is a node that also lives inside the root tree, so the
JSON would duplicate whole subtrees and, on the way back, deserialise into
*copies* rather than the aliased nodes `CdCommand` relies on mutating.

### Lesson text is fetched, not returned

`AbstractCommand` prints `lesson` once per `run()` call, outside the input loop,
then loops on input. `execute(Folder, String)` is called **once per submitted
line**, so returning the lesson from `execute` would reprint the whole lesson
after every attempt.

Lesson text moves to its own endpoint, `GET /api/lessons`, returning the static
copy for each lesson id. The front end renders it once in the lesson panel and
leaves it there. `CommandResult.hint` still carries the per-attempt retry
example, which is the part that *should* repeat.

This also means `AbstractCommand`'s five private fields need no accessors, and
the domain layer stays free of presentation concerns.

### ViewCommand needs attention

`ViewCommand` does not fit `execute(Folder, String)` as cleanly as the others:

- it calls `scanner.nextLine()` itself rather than receiving input
- it has no `else` branch, so any input other than `ls` or `*` silently prints
  nothing
- it does not loop, unlike `AbstractCommand`

Ported, it becomes: input `"ls"` returns the listing with `success = true`;
anything else returns `success = false` with a hint, replacing today's silent
no-op. This is a small, deliberate behaviour improvement, and it should be
called out rather than slipped in.

### Folder must remain a tree

`Folder` currently holds `Map<String, Folder> subFolders` and no reference to
its parent. That must stay true.

In a stateless design the folder tree is serialised to JSON on every request.
A `parent` field would make the structure cyclic, and serialisation would
recurse until the stack overflows.

`cd` is now built on `main`, and it confirmed this: `Navigator` tracks location
in its own `Deque<Folder>` rather than adding a parent link to `Folder`.
Location is a property of the session, not of the folder. On the wire that
becomes `List<String>` of names — see *Navigation and the `cd` command*.

### Console application

`App.main` keeps working, rewritten against the same domain:

```java
CommandResult result = makeFile.execute(currentFolder, scanner.nextLine());
System.out.println(result.output());
```

The retry loop that leaves `AbstractCommand` reappears here, where it belongs —
in the code that owns the console conversation.

Keeping both front ends is not nostalgia. Two independent consumers of one
domain is the evidence that the abstraction is real rather than decorative.

## API

Two endpoints plus a health check. No endpoint has any notion of a user.

### `POST /api/command`

Request:

```json
{
  "lessonId": "touch",
  "command": "touch cat.jpg",
  "path": ["photos"],
  "state": {
    "name": "root",
    "files": ["todo.md", "notes.txt"],
    "subFolders": {}
  }
}
```

Response:

```json
{
  "output": "File Successfully created!",
  "correct": true,
  "hint": null,
  "path": ["photos"],
  "state": {
    "name": "root",
    "files": ["todo.md", "notes.txt"],
    "subFolders": {
      "photos": { "name": "photos", "files": ["cat.jpg"], "subFolders": {} }
    }
  }
}
```

`state` is always the **whole tree from root**. `path` locates the current
folder within it — empty means root. The server walks `path` from root to find
the folder to operate on, so `touch` affects the folder the player is actually
standing in.

`path` also comes back on the response because `cd` changes it. For every other
lesson it is returned unchanged.

`lessonId` selects the handler. `"touch"`, `"mkdir"`, and `"ls"` map to
`TouchCommand`, `MkDirCommand`, and `ViewCommand` through an immutable
`Map<String, Command>`. `"cd"` is dispatched separately, because `CdCommand`
does not implement `Command`. An unrecognised `lessonId` is a 400.

A rejected command is not an HTTP error. It is a normal response with
`correct: false` and a populated `hint` — being wrong is how the game teaches.
HTTP status codes describe the transport, not the lesson.

### `GET /api/lessons`

Returns the static lesson text for every lesson, keyed by id:

```json
{ "touch": { "title": "Make a file", "body": "To make a file, type…",
             "example": "touch chicken.leg" } }
```

Fetched once on page load. This exists because `execute()` runs per submitted
line, so lesson text cannot ride on the command response without reprinting
itself after every attempt.

### `GET /api/health`

Returns 200 with a trivial body. Exists so the browser can wake a sleeping
container.

### DTO boundary

`FolderDto` is a separate type from `Folder` rather than serialising the
domain class directly. This keeps Jackson annotations out of the domain layer
and means the wire format can change without touching game logic.

## Front end

A single page, three panels, no build step:

```
┌─ LESSON ─────┐┌─ TERMINAL ───────┐┌─ FILES ───┐
│ Make a file  ││ $ touch cat.jpg  ││ root/     │
│              ││ ✓ Created!       ││  todo.md  │
│ Type touch,  ││                  ││  notes.txt│
│ then a name, ││ $ mkdir photos   ││  cat.jpg ←│
│ then .ext    ││ ✓ Created!       ││  photos/ ←│
│              ││ $ █              ││           │
└──────────────┘└──────────────────┘└───────────┘
```

The file tree re-renders on every successful command. That is the point of the
design: it makes the invisible effect of a CLI command visible, which is the
entire premise of the game.

Plain HTML, CSS, and JavaScript — no framework, no bundler, no `node_modules`.
For a Java portfolio the backend is what is being evaluated, and a front end
with no build step cannot break in a build.

Panels stack vertically below 768px.

### State handling

Game state lives in a JavaScript object, written to `localStorage` after each
successful command so a refresh resumes the session. It is sent with every
request and replaced by whatever the server returns.

Which lessons the player has completed is tracked in that same client-side
object, from the `correct` flag on each response, and stored **per profile** —
`localStorage` key `cq.profile.<username>.progress`. This is what makes the
login screen a learning feature: signing in as a different player restores that
player's progress. No progress data reaches the server.

Guest sessions use a reserved profile that is cleared on sign-out, matching
`LoginScreen`'s existing "Continue as Guest" option.

### Cold start

On page load the front end issues `GET /api/health` and ignores the result.
The player then spends twenty to thirty seconds reading the first lesson and
typing. By the time the first real command is submitted, the container is
usually awake.

If a request is still slow, the UI shows a "waking the server" indicator
rather than appearing frozen. The page itself never blocks on the API.

## Error handling

| Condition | Handling |
|---|---|
| Malformed command | `CommandResult` with `success: false` and a hint. HTTP 200. |
| Invalid or missing JSON fields | Bean Validation, mapped by `@ControllerAdvice` to HTTP 400 with a plain message. Never a stack trace. |
| Oversized state | Rejected as 400 past any of: 200 files per folder, 50 sub-folders per folder, depth 10, 100-character names, 64KB request body. |
| API unreachable or asleep | Front end retries with backoff and shows a status indicator. The page stays readable. |
| Unexpected server exception | HTTP 500 with a generic message; details logged server-side only. |

The size caps matter specifically because the design is stateless. The client
supplies the folder tree, so the client is untrusted input. A server that
trusts it can be made to allocate without bound.

## Testing

The 86 tests are the safety net for the refactor, but they do not currently
compile on `main` — they sit in the root package while the classes they test
moved into `commands/` and `hub/`, giving 200 `cannot find symbol` errors.

Step one on the web branch is therefore to make them compile again, by moving
each test into a package mirroring the class it tests. `main` keeps its broken
suite; that is the owner's decision and this branch does not change it.

The tests then change shape but not intent: today they capture `System.out` and
feed a fake `Scanner`; afterwards they assert on a returned `CommandResult`.
The assertions are the evidence that behaviour was preserved across the
refactor, which is precisely why they are repaired first rather than rewritten
afterwards.

Layers:

- **Domain** — pure unit tests, no Spring context. Fast. Carries the bulk of
  the coverage.
- **Web** — `@WebMvcTest` slice tests for status codes, validation failures,
  and malformed JSON. No full application context.
- **Serialisation** — a round-trip test: `Folder` → DTO → JSON → DTO →
  `Folder` preserves structure. This is the test that would catch an
  accidental reintroduction of a parent reference.
- **Path resolution** — `["photos","2024"]` resolves to the right node; an
  unknown segment is rejected rather than silently landing at root.

The last two are the tests that would have caught the `Navigator` serialisation
trap, which produced `{}` with no error at all.

New classes on `main` currently have no tests — `Navigator`, `CdCommand`,
`ViewCommand`, `User`, `UserManager`, `LoginScreen`. The previously measured
97% instruction / 90% branch figure predates them and is stale. `Navigator`
and `CdCommand` are worth covering on this branch because the web port depends
on them; the `user/` classes are console-only and out of the server's scope.

## Deployment

| Component | Host | Notes |
|---|---|---|
| Static front end | Netlify or GitHub Pages | Instant, CDN-backed, free |
| API | Render (Docker) | Free tier sleeps when idle |

The API is containerised with a multi-stage Dockerfile: one stage builds with
the Maven wrapper, a second copies only the resulting jar onto a JRE base
image, so the JDK and the build cache do not ship to production.

CORS is restricted to the deployed front-end origin plus `localhost` for
development — not a wildcard.

## Verified toolchain facts

Confirmed against Maven Central on 2026-08-06 rather than assumed:

| Item | Value | Note |
|---|---|---|
| Latest Spring Boot | 4.1.0 (2026-06) | Spring Framework 7.0.8 |
| Spring Boot Java baseline | 17 | A floor, not a ceiling; overridable |
| Managed JUnit | 6.0.3 | Project currently pins 5.11.0 |
| Managed Jackson | 3.1.4 | Packages renamed to `tools.jackson.*` |
| Latest JaCoCo | 0.8.15 | Required for Java 25 |
| Java 25 class file version | 69 | JaCoCo 0.8.12 could not read it |
| Maven | 3.9.16, wrapper 3.3.4 | Script-only, no wrapper jar committed |

## Risks

**JUnit 5 → 6.** Adopting `spring-boot-starter-parent` upgrades the managed
JUnit version, revalidating all 86 tests against a new major release. Mitigate
by upgrading JUnit as its own step, with the suite green before Spring Boot is
introduced, so a failure has one obvious cause.

**Jackson 3 package rename.** Imports are `tools.jackson.*`, not
`com.fasterxml.jackson.*`. Most online examples show the old packages.

**Java 25 with Spring Boot 4.1.** Boot 4.1 postdates Java 25, so support is
expected, but it is unverified here. The first task of Phase 2 is an empirical
check: stand up a trivial endpoint on Java 25 and run it. If anything
misbehaves, drop `maven.compiler.release` to 21, which costs this project
nothing — no Java 22–25 language feature is in use.

**Free-tier sleep.** Mitigated by static hosting and the warm-up ping, not
eliminated. If it proves unacceptable, options are a paid tier (~$7/month) or
an external uptime pinger.

**Jackson 3 constructor binding.** `Folder` has a single-argument constructor
and no no-arg constructor. Jackson 3's default `ConstructorDetector` heuristic
does deserialise it correctly, but only when the module is compiled with
`-parameters` — which `spring-boot-starter-parent` enables by default. If the
parent is ever dropped or the compiler config hand-rolled, deserialisation
breaks in a way that looks unrelated to the change. The round-trip test guards
this.

**Divergence from `main`.** This branch is based on `b9094b3`; `main` has since
moved to `d9dc5ba`. The restructure and the four new features must be merged in
before Phase 2, or the port will be built against code that no longer exists.

## Phases

Each phase ends with a green test suite and a working application.

| Phase | Work | Status |
|---|---|---|
| 0 | Repo hygiene: `.gitignore`, untrack `target/`, Maven wrapper | ✅ `ac9f72f`, `b9094b3` |
| 1 | Merge `main`; move tests into mirrored packages | ✅ `762c7e9`, `fa72939` — 66 green |
| 2 | Upgrade JUnit 5.11 → 6.1.3 on its own | ✅ `71ba3a5` — no source changes needed |
| 3 | Domain refactor: `CommandResult`, `execute()` | ✅ `a248eec`, `9f14289` — 81 green, `App` unchanged |
| 4 | Spring Boot: controllers, DTOs, path resolution, validation, CORS | ✅ `3e92d1b` — 103 green, verified by `curl` |
| 5 | Front end: profile gate, three panels, live file tree | ✅ `0f4e760` — verified in a real browser |
| 6 | Dockerfile and deploy configuration | ✅ `886aba9` — **image unbuilt, no Docker daemon available** |
| 7 | README and DEPLOY guide | ✅ `886aba9` |
| — | **Deploy to Render and GitHub Pages** | ⬜ **Needs the owner's accounts** — see `DEPLOY.md` |

Java 25 with Spring Boot 4.1 was the one risk held open in this spec. It is
resolved: the project compiles and runs on Java 25 and the fallback to Java 21
is not needed.

Two findings during implementation that the spec had not anticipated:

- `spring-boot-maven-plugin` must be *declared*, not merely inherited. Without
  it `package` produced a 38 KB jar with no `Main-Class`, which would have
  built a container that crash-looped on start.
- Spring Boot 4 split the test slices into per-technology modules, so
  `@WebMvcTest` needs a separate `spring-boot-webmvc-test` dependency and lives
  in `org.springframework.boot.webmvc.test.autoconfigure`.

Phases 1, 2, and 3 are separated on purpose. Merging a restructure, changing
the test framework, and changing the code under test are three independent
sources of failure; combined, a red suite tells you nothing about which one
broke it.

## Open question

Front-end hosting is Netlify or GitHub Pages. GitHub Pages needs no new
account and matches the existing repository; Netlify gives simpler custom
domains and deploy previews. Deferred to Phase 5 — it changes nothing earlier.
