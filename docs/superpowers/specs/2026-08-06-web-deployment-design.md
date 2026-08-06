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

## Non-goals

Deliberately excluded to keep scope achievable:

- User accounts, login, or a database
- Server-side persistence of any kind
- The unimplemented `cd` and progress features from the console menu
  (`case 4` and `case 5`, currently "coming soon"). The architecture leaves
  room for them; this project does not build them.
- Multiplayer, leaderboards, or social features
- A JavaScript framework or front-end build pipeline

## Architecture

Three layers. The domain layer has no web dependencies and no I/O.

```
BROWSER — static files on a CDN (Netlify or GitHub Pages)
  index.html · app.js · style.css
  Holds game state in memory, mirrored to localStorage
  Renders: lesson panel · terminal log · live file tree
        │
        │  POST /api/command  { command, state, lessonId }
        │  ←──────────────────  { output, state, correct }
        ▼
SERVER — container on Render (or Fly.io)
  WEB LAYER      CommandController · request/response DTOs · FolderDto
                 Maps DTO → domain, calls execute(), maps result back
  DOMAIN LAYER   Folder · Command · AbstractCommand · TouchCommand
                 MkDirCommand · CommandResult
                 No Spring. No Scanner. No System.out.
```

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

### Folder must remain a tree

`Folder` currently holds `Map<String, Folder> subFolders` and no reference to
its parent. That must stay true.

In a stateless design the folder tree is serialised to JSON on every request.
A `parent` field would make the structure cyclic, and serialisation would
recurse until the stack overflows.

When `cd` is eventually built, the current location is tracked as a separate
`List<String>` path — `["root", "photos"]` — and `cd ..` removes the last
element. Location is a property of the session, not of the folder.

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

One endpoint plus a health check.

### `POST /api/command`

Request:

```json
{
  "lessonId": "touch",
  "command": "touch cat.jpg",
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
  "state": {
    "name": "root",
    "files": ["todo.md", "notes.txt", "cat.jpg"],
    "subFolders": {}
  }
}
```

`lessonId` selects which `Command` implementation handles the input —
`"touch"` maps to `TouchCommand`, `"mkdir"` to `MkDirCommand`. The controller
holds an immutable `Map<String, Command>` for this lookup. An unrecognised
`lessonId` is a 400.

A rejected command is not an HTTP error. It is a normal response with
`correct: false` and a populated `hint` — being wrong is how the game teaches.
HTTP status codes describe the transport, not the lesson.

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
object, from the `correct` flag on each response. This is a counter in the
browser, not the "My progress" screen listed under non-goals — no progress
data reaches the server and nothing is persisted beyond `localStorage`.

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

The existing 86 tests are the safety net for the refactor. They change shape
but not intent: today they capture `System.out` and feed a fake `Scanner`;
afterwards they assert on a returned `CommandResult`. This is a simplification
— the tests get shorter and more direct.

Layers:

- **Domain** — pure unit tests, no Spring context. Fast. Carries the bulk of
  the coverage.
- **Web** — `@WebMvcTest` slice tests for status codes, validation failures,
  and malformed JSON. No full application context.
- **Serialisation** — one round-trip test: `Folder` → DTO → JSON → DTO →
  `Folder` preserves structure. This is the test that would catch an
  accidental reintroduction of a parent reference.

JaCoCo is already configured. Current baseline is 97% instruction and 90%
branch coverage; the refactor should not lower it.

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

## Phases

Each phase ends with a green test suite and a working application.

| Phase | Work | Done when |
|---|---|---|
| 0 | Repo hygiene: `.gitignore`, untrack `target/`, Maven wrapper | ✅ Complete (commits `ac9f72f`, `b9094b3`) |
| 1 | Upgrade JUnit 5.11 → 6.x on its own | 86 tests green |
| 2 | Domain refactor: `CommandResult`, `execute()`, rewrite `App` | Tests green, console app still runs |
| 3 | Spring Boot: controller, DTOs, validation, CORS | Endpoint answers `curl` locally |
| 4 | Front end: three panels, live file tree | Playable against local API |
| 5 | Dockerfile, deploy API and static site, wire CORS | Public URL works |
| 6 | README with screenshot, live link, coverage note | Repo reads well |

Phase 1 is separated from Phase 2 on purpose. Changing the test framework and
the code under test simultaneously makes a failure ambiguous.

## Open question

Front-end hosting is Netlify or GitHub Pages. GitHub Pages needs no new
account and matches the existing repository; Netlify gives simpler custom
domains and deploy previews. Deferred to Phase 5 — it changes nothing earlier.
