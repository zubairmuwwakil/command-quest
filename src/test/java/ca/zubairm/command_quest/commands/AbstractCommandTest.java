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

    // ---------- the extra-arguments branch ----------

    /**
     * A space starts a new word, so "mkdir nested folder me" is three names in
     * a real shell and it makes three folders. Command Quest teaches one name
     * at a time, so rather than quietly keeping the first word and throwing the
     * rest away - which taught the player that spaces are harmless - the whole
     * line is refused and the reason is explained.
     */
    @Test
    @DisplayName("refuses a name that was typed with spaces in it")
    void rejectsExtraArguments() {
        Folder folder = new Folder("root");
        // Deliberately different names: if the first line were still honoured,
        // "first.txt" would exist and "second.txt" would never be read.
        Scanner input = keystrokes("touch first.txt extra.txt", "touch second.txt");

        captureOutput(() -> new TouchCommand().run(folder, input));

        assertFalse(folder.hasFile("first.txt"), "the spaced line should create nothing at all");
        assertFalse(folder.hasFile("extra.txt"), "least of all the trailing words");
        assertEquals(java.util.List.of("second.txt"), folder.getFiles());
    }

    @Test
    @DisplayName("explains that a space starts a new name")
    void explainsWhySpacesAreTheProblem() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("mkdir nested folder me", "mkdir nestedFolderMe");

        String output = captureOutput(() -> new MkDirCommand().run(folder, input));

        assertTrue(output.contains("makes one folder at a time"),
                "expected the spacing lesson, got:\n" + output);
        assertTrue(output.contains("3 folders"),
                "expected the player's own word count back, got:\n" + output);
    }

    @Test
    @DisplayName("suggests the joined-up name the player probably meant")
    void suggestsJoiningTheWords() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("mkdir nested folder me", "mkdir nestedFolderMe");

        String output = captureOutput(() -> new MkDirCommand().run(folder, input));

        assertTrue(output.contains("mkdir nestedFolderMe"),
                "expected a joined-up suggestion, got:\n" + output);
        assertTrue(folder.hasSubFolder("nestedFolderMe"), "the retry should succeed");
    }

    @Test
    @DisplayName("keeps the suggestion out of the way when it would be invalid")
    void omitsASuggestionThatWouldNotBeAccepted() {
        Folder folder = new Folder("root");
        // "myFile" has no extension, so touch would reject the suggestion too.
        Scanner input = keystrokes("touch my file", "touch myFile.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        assertFalse(output.contains("Did you mean"),
                "should not suggest a name its own pattern forbids, got:\n" + output);
        assertTrue(output.contains("makes one file at a time"));
        assertTrue(output.contains("Here's an example: touch chicken.leg"),
                "should fall back to the worked example, got:\n" + output);
    }

    @Test
    @DisplayName("still names the right command when the keyword is wrong AND spaced")
    void wrongKeywordWithExtraWordsIsAPlainFormatError() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("mkdir a b c", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(folder, input));

        // TouchCommand has no business lecturing about mkdir's spacing.
        assertTrue(output.contains("Not quite - the format was off."),
                "expected the generic format error, got:\n" + output);
        assertTrue(folder.hasFile("hello.txt"));
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

    /**
     * The browser reaches AbstractCommand through execute() and never through
     * run(), so the spacing rule has to be pinned here too - this is the exact
     * path that turned "mkdir nested folder me" into one folder called
     * "nested" on the deployed site.
     */
    @Test
    @DisplayName("execute() refuses a spaced name and explains the space")
    void executeRefusesASpacedName() {
        Folder folder = new Folder("root");

        CommandResult result = new MkDirCommand().execute(folder, "mkdir nested folder me");

        assertFalse(result.succeeded());
        assertFalse(result.finished(), "the player should get another go");
        assertTrue(result.output().contains("makes one folder at a time"),
                "expected the spacing lesson, got:\n" + result.output());
        assertTrue(result.output().contains("Did you mean: mkdir nestedFolderMe"),
                "expected the joined-up suggestion, got:\n" + result.output());
        assertTrue(folder.getSubFolders().isEmpty(), "no folder at all, not even the first word");
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

    // ---------- naming something after a teammate ----------

    /**
     * The shoutout rides in the CommandResult rather than a println, which is
     * what lets the same easter egg reach the terminal and the browser without
     * either one knowing about it.
     */
    @Test
    @DisplayName("naming a folder after a teammate says so")
    void greetsATeammateOnMkdir() {
        Folder folder = new Folder("root");

        CommandResult result = new MkDirCommand().execute(folder, "mkdir victoria");

        assertTrue(result.succeeded());
        assertTrue(folder.hasSubFolder("victoria"), "the folder is still actually made");
        assertTrue(result.output().contains("Victoria Oyedotun"),
                "expected the shoutout; got:\n" + result.output());
        assertTrue(result.output().contains("Folder Successfully created!"),
                "the ordinary confirmation must survive; got:\n" + result.output());
    }

    @Test
    @DisplayName("naming a file after a teammate says so, extension and all")
    void greetsATeammateOnTouch() {
        Folder folder = new Folder("root");

        CommandResult result = new TouchCommand().execute(folder, "touch seun.md");

        assertTrue(result.succeeded());
        assertTrue(folder.hasFile("seun.md"));
        assertTrue(result.output().contains("Seun Edagbami-olota"),
                "the extension must not stop the match; got:\n" + result.output());
    }

    @Test
    @DisplayName("an ordinary name gets the ordinary confirmation and nothing more")
    void saysNothingExtraForOrdinaryNames() {
        Folder folder = new Folder("root");

        CommandResult result = new MkDirCommand().execute(folder, "mkdir homework");

        assertEquals("Folder Successfully created!", result.output(),
                "no easter egg should fire for a name nobody on the team has");
    }
}
