# Command Quest

A terminal game that teaches real command-line skills by making you type real
commands. Instead of reading about `touch`, `mkdir`, `ls`, and `cd`, the player
runs them against a simulated file system that lives entirely in memory — so a
typo costs nothing and a wrong command produces a lesson instead of an error.

Written in Java as an individual project: a console app, a login gate, and a
navigable folder tree, backed by a 112-test JUnit 5 suite.

---

## Quick start

Requires **JDK 25** (the project uses text blocks and targets release 25) and no
manual Maven install — the wrapper is committed.

```bash
./mvnw -q clean compile exec:java -Dexec.mainClass=ca.zubairm.command_quest.App
```

Or compile once and run it straight from the JDK — no plugin download needed:

```bash
./mvnw -q clean compile && java -cp target/classes ca.zubairm.command_quest.App
```

Run the test suite and generate the coverage report:

```bash
./mvnw clean test
```

The coverage report lands at `target/site/jacoco/index.html`.

---

## How it plays

1. **The gate** — create an account (username + 4-digit PIN), log in, or continue
   as Guest.
2. **The menu** — pick a number: make a file, make a folder, list the current
   folder, or change folder.
3. **The lesson** — each option explains its command, then waits for you to type
   it. Wrong format? It tells you *why* it was wrong and shows another example.
   Type `*` to return to the menu.

The folder tree persists across logout, but every session restarts at `root`.

---

## Project structure

```
ca.zubairm.command_quest
├── App.java              menu loop, session loop, wiring
├── commands/             what the player can do
│   ├── Command           the interface every command implements
│   ├── AbstractCommand   shared "prompt → validate → create" algorithm
│   ├── TouchCommand      creates files
│   ├── MkDirCommand      creates folders
│   ├── LsCommand         lists the current folder
│   └── CdCommand         moves between folders
├── hub/                  where things live
│   ├── Folder            a folder's files and subfolders
│   └── Navigator         where you currently are in the tree
└── user/                 who is playing
    ├── User              a username and a PIN
    ├── UserManager       the account store
    └── LoginScreen       the login UI
```

The split that matters: **`Folder` holds contents, `Navigator` holds position.**
A command asks the `Navigator` for the current folder and acts on it, which is
why `cd` can change where a later `touch` puts its file without either command
knowing about the other.

---

## OOP and SOLID

| Principle | Where | How |
| --- | --- | --- |
| **Abstraction** | `Command` | `App` calls `run()` without knowing how a file gets made. |
| **Encapsulation** | `Folder`, `User` | Fields are private. `User` exposes `checkPin()` — never a `getPin()`, so the PIN can be verified but not read. |
| **Inheritance** | `TouchCommand`, `MkDirCommand` | Both extend `AbstractCommand` and inherit the whole prompt-and-validate loop. |
| **Polymorphism** | `App.main()` | Four commands are held as `Command` variables; `run()` resolves to the right class at runtime. |

| SOLID | Applied as |
| --- | --- |
| **Single Responsibility** | `LoginScreen` handles the UI, `UserManager` handles the accounts, `User` is just identity. Same split in `hub/`: `Folder` = contents, `Navigator` = position. |
| **Open/Closed** | `AbstractCommand.run()` is closed for modification. A new create-style command is a new subclass supplying `exists()` and `create()` — the algorithm is never touched. |
| **Liskov Substitution** | `CdCommand` implements `Command` directly rather than extending `AbstractCommand` (it moves instead of creating), yet `App` stores and calls it exactly like the others. |
| **Dependency Inversion** | `App` depends on the `Command` interface, not on `TouchCommand`. `LoginScreen` receives its `UserManager` through the constructor. |

`AbstractCommand` is a **template method**: `run()` fixes the sequence — teach,
read input, reject bad format, reject duplicates, create — and defers only the
two steps that actually differ to its subclasses.

---

## Input validation

| # | Validation | Where |
| --- | --- | --- |
| 1 | Menu input must parse as a number; unknown numbers are rejected | `App.main()` |
| 2 | PIN must match exactly four digits (`\d{4}`) | `LoginScreen.show()` |
| 3 | Usernames must be unique; wrong username or PIN is refused | `LoginScreen`, `UserManager.login()` |
| 4 | Command format — right keyword, right token count, name matching a regex (`\w+\.\w+` for files, `\w+` for folders) | `AbstractCommand.run()` |
| 5 | Duplicate file/folder names are blocked before creation | `AbstractCommand.run()` via `exists()` |
| 6 | Extra words trigger a targeted lesson: it counts the names, explains that spaces separate words, and suggests a valid camelCase join | `AbstractCommand.spacingLesson()` |
| 7 | `cd` into a nonexistent folder fails; `cd ..` at root refuses to go higher | `CdCommand.run()`, `Navigator` |
| 8 | `ls` accepts nothing but `ls` on its own | `LsCommand.run()` |

Validation is written to *teach*: a rejected command explains the mistake and
re-prompts rather than throwing the player back to the menu.

---

## Data structures

| Structure | Where | Why this one |
| --- | --- | --- |
| `ArrayList<String>` | `Folder.files` | Files are listed in the order created, and `ls` only ever walks them start to finish. |
| `HashMap<String, Folder>` | `Folder.subFolders` | `cd <name>` is a lookup by name — O(1) instead of scanning a list. Unique keys also make duplicate folder names impossible by construction. |
| `HashMap<String, User>` | `UserManager.users` | Login is a username lookup, so the username is the natural key. |
| `ArrayDeque<Folder>` | `Navigator.path` | Used as a stack: `cd <name>` pushes, `cd ..` pops. The stack *is* the path from root to here, so the breadcrumb is just the stack read backwards, and `up()` guarding on `size() > 1` is what makes going above root impossible. |

---

## Testing

**112 JUnit 5 tests, all passing, 94.0% instruction coverage** (JaCoCo).

The three most complex methods are fully branch-covered:

| Method | Cyclomatic complexity | Branch coverage |
| --- | --- | --- |
| `App.main()` | 10 | 10/10 |
| `CdCommand.run()` | 9 | 9/9 |
| `AbstractCommand.run()` | 9 | 9/9 |

A console game is normally awkward to test because it reads from `System.in` and
writes to `System.out`. `TestSupport` solves both: `keystrokes(...)` builds a
`Scanner` over scripted lines as if they were typed, and `captureOutput(...)`
redirects `System.out` into a buffer so a test can assert on what the player saw.

That difference is itself a lesson in dependency injection. `AbstractCommand.run()`
*accepts* a `Scanner`, so a test hands it one directly. `App.main()` *creates* its
own over `System.in`, so `runApp(...)` has to swap out standard input underneath
it — the extra ceremony is the practical cost of not injecting the dependency.

**Known gap:** `LoginScreen.show()` (complexity 8) sits at 55.4% instruction and
3/8 branches. Its account-creation and failed-login paths are exercised through
`AppTest` end to end, but it has no dedicated unit test of its own.

---

## Roadmap

- Direct unit tests for `LoginScreen.show()` to close the coverage gap
- Persist accounts and the folder tree between runs
- More commands: `rm`, `pwd`, `cat`
