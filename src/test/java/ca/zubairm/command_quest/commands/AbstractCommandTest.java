package ca.zubairm.command_quest.commands;

import static ca.zubairm.command_quest.TestSupport.captureOutput;
import static ca.zubairm.command_quest.TestSupport.keystrokes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for AbstractCommand.run() - the most complex method in the project.
 *
 * run() is a Template Method: it owns the fixed algorithm (read a line, allow
 * an escape, validate the format, reject duplicates, otherwise create) and
 * delegates the two varying steps to the abstract exists() and create() hooks.
 *
 * Every branch of that algorithm is exercised below. The tests drive the real
 * TouchCommand and MkDirCommand subclasses rather than stand-ins, so they test
 * production behaviour rather than a test double's behaviour.
 */
@DisplayName("AbstractCommand.run()")
class AbstractCommandTest {

    // ---------- the success branch ----------

    @Test
    @DisplayName("creates the file when the command is correct first time")
    void createsOnValidCommand() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch hello.txt");

        captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(folder.hasFile("hello.txt"));
    }

    @Test
    @DisplayName("returns as soon as the command succeeds")
    void stopsReadingAfterSuccess() {
        Folder folder = new Folder("root");
        // The second line must never be consumed - run() should have returned.
        Scanner input = keystrokes("touch first.txt", "touch second.txt");

        captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(folder.hasFile("first.txt"));
        assertFalse(folder.hasFile("second.txt"));
    }

    @Test
    @DisplayName("confirms success with the noun capitalised")
    void announcesSuccessWithCapitalisedNoun() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        // exercises the private capitalize() helper: "file" -> "File"
        assertTrue(output.contains("File Successfully created!"),
                "expected capitalised success message, got:\n" + output);
    }

    @Test
    @DisplayName("shows the lesson text before asking for input")
    void printsTheLessonFirst() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("To make a file, type the touch command."),
                "expected the lesson text, got:\n" + output);
    }

    // ---------- the bad-format branch ----------

    @Test
    @DisplayName("rejects a wrong keyword and keeps asking")
    void rejectsWrongKeyword() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("mkdir hello.txt", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Not quite - the format was off."));
        assertTrue(folder.hasFile("hello.txt"), "should still succeed on the retry");
    }

    @Test
    @DisplayName("rejects a name that does not match the required pattern")
    void rejectsNameWithoutExtension() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch noextension", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Not quite - the format was off."));
        assertFalse(folder.hasFile("noextension"));
    }

    @Test
    @DisplayName("rejects a keyword with no name after it")
    void rejectsCommandWithNoArgument() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Not quite - the format was off."));
        assertEquals(1, folder.getFiles().size());
    }

    @Test
    @DisplayName("rejects a blank line")
    void rejectsBlankLine() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Not quite - the format was off."));
    }

    @Test
    @DisplayName("rejects a whitespace-only line")
    void rejectsWhitespaceOnlyLine() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("     ", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Not quite - the format was off."));
    }

    @Test
    @DisplayName("offers a worked example when the format is wrong")
    void showsAnExampleAfterAFormatError() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("nonsense", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Here's another example: touch chicken.leg"),
                "expected the example text, got:\n" + output);
    }

    @Test
    @DisplayName("tolerates surrounding whitespace around a valid command")
    void trimsSurroundingWhitespace() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("   touch hello.txt   ");

        captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(folder.hasFile("hello.txt"));
    }

    @Test
    @DisplayName("keeps retrying through several bad attempts")
    void retriesUntilTheUserGetsItRight() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("wrong", "still wrong", "touch", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertEquals(3, output.split("Not quite - the format was off.", -1).length - 1,
                "expected exactly three rejections before success");
        assertTrue(folder.hasFile("hello.txt"));
    }

    // ---------- the duplicate branch ----------

    @Test
    @DisplayName("refuses to create a file that already exists")
    void reportsDuplicateFile() {
        Folder folder = new Folder("root");
        folder.addFile("hello.txt");
        Scanner input = keystrokes("touch hello.txt", "touch other.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("file already exists"),
                "expected the duplicate warning, got:\n" + output);
    }

    @Test
    @DisplayName("praises the format even when the name is taken")
    void duplicateStillConfirmsTheFormatWasRight() {
        Folder folder = new Folder("root");
        folder.addFile("hello.txt");
        Scanner input = keystrokes("touch hello.txt", "touch other.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Your command format was correct, congrats!"));
    }

    @Test
    @DisplayName("does not add a second copy of an existing file")
    void duplicateDoesNotCreateAnything() {
        Folder folder = new Folder("root");
        folder.addFile("hello.txt");
        Scanner input = keystrokes("touch hello.txt", "touch other.txt");

        captureOutput(() -> new TouchCommand().run(folder, input));

        assertEquals(java.util.List.of("hello.txt", "other.txt"), folder.getFiles());
    }

    // ---------- the "*" escape branch ----------

    @Test
    @DisplayName("returns to the menu when the user types *")
    void starReturnsToTheMenu() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("*");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Returning to the main menu..."));
    }

    @Test
    @DisplayName("creates nothing when the user escapes with *")
    void starCreatesNothing() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("*");

        captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(folder.getFiles().isEmpty());
    }

    @Test
    @DisplayName("accepts * after a failed attempt")
    void starWorksAfterAFormatError() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("garbage", "*");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Returning to the main menu..."));
        assertTrue(folder.getFiles().isEmpty());
    }

    @Test
    @DisplayName("accepts * with surrounding whitespace")
    void starIsTrimmedBeforeComparison() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("   *   ");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(output.contains("Returning to the main menu..."));
    }

    // ---------- known validation gap (characterisation test) ----------

    /**
     * The guard is "tokens.length < 2", which never rejects too MANY tokens, so
     * trailing arguments are silently discarded. A real shell would treat
     * "touch a.txt b.txt" as a request for two files.
     *
     * This test documents the behaviour as it stands today. Tightening the
     * guard to "tokens.length != 2" is the fix; this test is what should be
     * updated to assert rejection once that change is made.
     */
    @Test
    @DisplayName("KNOWN GAP: silently ignores extra arguments")
    void extraArgumentsAreCurrentlyIgnored() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch hello.txt extra junk");

        captureOutput(() -> new TouchCommand().run(folder, input));

        assertTrue(folder.hasFile("hello.txt"));
        assertFalse(folder.hasFile("extra"), "the extra arguments are dropped, not created");
        assertEquals(1, folder.getFiles().size());
    }

    // ---------- the template method is genuinely open for extension ----------

    /**
     * A brand-new command can be added without touching AbstractCommand at all
     * - the Open/Closed Principle in action. This subclass exists only to prove
     * that the extension point works, and it exercises the same run() algorithm
     * with a different keyword, noun, and name pattern.
     */
    private static final class ShoutCommand extends AbstractCommand {
        ShoutCommand() {
            super("shout", "message", "[A-Z]+", "Type: shout HELLO", "shout HELLO");
        }

        @Override
        protected boolean exists(Folder folder, String name) {
            return folder.hasFile(name);
        }

        @Override
        protected void create(Folder folder, String name) {
            folder.addFile(name);
        }
    }

    @Test
    @DisplayName("supports a new command subclass with no change to AbstractCommand")
    void anyNewSubclassInheritsTheWholeAlgorithm() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("shout lowercase", "shout HELLO");

        String output = captureOutput(() -> new ShoutCommand().run(folder, input));

        assertTrue(output.contains("Not quite - the format was off."), "its own pattern is enforced");
        assertTrue(folder.hasFile("HELLO"));
        assertTrue(output.contains("Message Successfully created!"), "its own noun is capitalised");
    }

    @Test
    @DisplayName("dispatches polymorphically through the Command interface")
    void runsThroughTheInterfaceReference() {
        Folder folder = new Folder("root");
        // Declared as Command; the correct run() is chosen at runtime.
        Command[] commands = { new TouchCommand(), new MkDirCommand() };

        captureOutput(() -> commands[0].run(folder, keystrokes("touch hello.txt")));
        captureOutput(() -> commands[1].run(folder, keystrokes("mkdir projects")));

        assertTrue(folder.hasFile("hello.txt"));
        assertTrue(folder.hasSubFolder("projects"));
    }

    // ---------- execute(): the same algorithm as a pure function ----------
    //
    // run() is a conversation - it prints, reads, and loops until satisfied.
    // execute() takes one line and returns what happened, printing nothing.
    // These tests pin the returned value; the run() tests above stay until
    // every caller has moved across.

    @Test
    @DisplayName("execute() creates the item and reports success")
    void executeCreatesTheItem() {
        Folder folder = new Folder("root");

        CommandResult result = new TouchCommand().execute(folder, "touch cat.jpg");

        assertTrue(result.succeeded(), "a well-formed command on a free name creates");
        assertEquals("File Successfully created!", result.output());
        assertTrue(folder.hasFile("cat.jpg"), "the folder really changed");
    }

    @Test
    @DisplayName("execute() rejects a malformed command and hands back the example")
    void executeRejectsAMalformedCommand() {
        Folder folder = new Folder("root");

        CommandResult result = new TouchCommand().execute(folder, "touch nodot");

        assertFalse(result.succeeded());
        assertFalse(result.finished(), "a rejection leaves the player retrying");
        assertEquals("Not quite - the format was off.", result.output());
        assertEquals("touch chicken.leg", result.hint(), "the hint is the command's own example");
        assertTrue(folder.getFiles().isEmpty(), "nothing was created");
    }

    @Test
    @DisplayName("execute() refuses a name that is already taken")
    void executeRefusesADuplicateName() {
        Folder folder = new Folder("root");
        folder.addFile("cat.jpg");

        CommandResult result = new TouchCommand().execute(folder, "touch cat.jpg");

        assertFalse(result.succeeded());
        assertFalse(result.finished(), "the player should get another go");
        assertTrue(result.output().contains("Your command format was correct"),
                "the format praise is kept - the syntax lesson was learned");
        assertTrue(result.output().contains("file already exists"));
        assertEquals(1, folder.getFiles().size(), "no duplicate was added");
    }

    @Test
    @DisplayName("execute() treats * as giving up, not as a bad command")
    void executeTreatsStarAsCancelling() {
        Folder folder = new Folder("root");

        CommandResult result = new TouchCommand().execute(folder, "*");

        assertFalse(result.succeeded(), "nothing was made");
        assertTrue(result.finished(), "cancelling ends the exchange, unlike a rejection");
        assertEquals("Returning to the main menu...", result.output());
        assertTrue(folder.getFiles().isEmpty());
    }
}
