package ca.zubairm.command_quest.commands;

import static ca.zubairm.command_quest.TestSupport.captureOutput;
import static ca.zubairm.command_quest.TestSupport.keystrokes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for AbstractCommand.run() - the shared loop behind mkdir and touch.
 *
 * run() is a Template Method: it owns the fixed algorithm (read a line, allow
 * an escape, check the format, reject duplicates, otherwise create) and leaves
 * the two varying steps to the abstract exists() and create() hooks. Each
 * section below covers one branch of that algorithm.
 *
 * The tests drive the real TouchCommand and MkDirCommand rather than stand-ins,
 * so they check production behaviour.
 */
@DisplayName("AbstractCommand.run()")
class AbstractCommandTest {

    // ---------- success ----------

    @Test
    @DisplayName("creates the file when the command is correct")
    void createsOnValidCommand() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch hello.txt");

        captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(folder.hasFile("hello.txt"));
    }

    @Test
    @DisplayName("stops reading as soon as the command succeeds")
    void stopsReadingAfterSuccess() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch first.txt", "touch second.txt");

        captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(folder.hasFile("first.txt"));
        assertFalse(folder.hasFile("second.txt"), "the second line should never be read");
    }

    @Test
    @DisplayName("confirms success with the noun capitalised")
    void announcesSuccessWithCapitalisedNoun() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("File Successfully created!"));
    }

    @Test
    @DisplayName("shows the lesson before asking for input")
    void printsTheLessonFirst() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("To make a file, type the touch command."));
    }

    @Test
    @DisplayName("accepts a valid command with spaces around it")
    void trimsSurroundingWhitespace() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("   touch hello.txt   ");

        captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(folder.hasFile("hello.txt"));
    }

    // ---------- bad format ----------

    /**
     * Every one of these lines is wrong in a different way, and all of them get
     * the same correction. The second scripted line is always a valid command so
     * run() has a way to finish.
     */
    @ParameterizedTest(name = "rejects \"{0}\"")
    @ValueSource(strings = {
            "mkdir hello.txt", // the other command's keyword
            "mkdir a b c",     // the other command's keyword, with extra words
            "touch noextension", // right keyword, name has no extension
            "touch",           // keyword with no name at all
            "nonsense",        // not a command
            "",                // an empty line
            "     ",           // spaces only
    })
    @DisplayName("rejects a badly formatted line and asks again")
    void rejectsBadFormat(String badLine) {
        Folder folder = new Folder("root");
        Scanner input = keystrokes(badLine, "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("Not quite - the format was off."));
        assertTrue(folder.hasFile("hello.txt"), "the retry should still succeed");
    }

    @Test
    @DisplayName("offers a worked example after a format error")
    void showsAnExampleAfterAFormatError() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("nonsense", "touch hello.txt");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("Here's another example: touch chicken.leg"));
    }

    @Test
    @DisplayName("keeps asking through several bad attempts")
    void retriesUntilTheUserGetsItRight() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("wrong", "still wrong", "touch", "touch hello.txt");

        captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(folder.hasFile("hello.txt"), "the fourth line should finally succeed");
    }

    // ---------- the name is already taken ----------

    @Test
    @DisplayName("refuses to create a file that already exists")
    void reportsDuplicateFile() {
        Folder folder = new Folder("root");
        folder.addFile("hello.txt");
        Scanner input = keystrokes("touch hello.txt", "touch other.txt");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("file already exists"));
        assertTrue(output.contains("Your command format was correct, congrats!"),
                "a taken name still means the format was right");
    }

    @Test
    @DisplayName("does not add a second copy of an existing file")
    void duplicateDoesNotCreateAnything() {
        Folder folder = new Folder("root");
        folder.addFile("hello.txt");
        Scanner input = keystrokes("touch hello.txt", "touch other.txt");

        captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertEquals(List.of("hello.txt", "other.txt"), folder.getFiles());
    }

    // ---------- the "*" escape ----------

    @Test
    @DisplayName("returns to the menu when the player types *")
    void starReturnsToTheMenu() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("*");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("Returning to the main menu..."));
        assertTrue(folder.getFiles().isEmpty(), "escaping must create nothing");
    }

    @Test
    @DisplayName("accepts * after a failed attempt")
    void starWorksAfterAFormatError() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("garbage", "*");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("Returning to the main menu..."));
    }

    @Test
    @DisplayName("accepts * with spaces around it")
    void starIsTrimmedBeforeComparison() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("   *   ");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("Returning to the main menu..."));
    }

    // ---------- a name typed with spaces in it ----------

    /**
     * A space starts a new word, so "mkdir nested folder me" asks a real shell
     * for three folders. Command Quest teaches one name at a time, so rather
     * than quietly keeping the first word - which would teach the player that
     * spaces are harmless - the whole line is refused and the reason explained.
     */
    @Test
    @DisplayName("creates nothing at all from a name typed with spaces")
    void rejectsExtraArguments() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("touch first.txt extra.txt", "touch second.txt");

        captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertEquals(List.of("second.txt"), folder.getFiles(),
                "only the retry should have created anything");
    }

    @Test
    @DisplayName("explains that a space starts a new name")
    void explainsWhySpacesAreTheProblem() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("mkdir nested folder me", "mkdir nestedFolderMe");

        String output = captureOutput(() -> new MkDirCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("makes one folder at a time"));
        assertTrue(output.contains("3 folders"), "it counts the player's own words back");
    }

    @Test
    @DisplayName("suggests the joined-up name the player probably meant")
    void suggestsJoiningTheWords() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("mkdir nested folder me", "mkdir nestedFolderMe");

        String output = captureOutput(() -> new MkDirCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("Did you mean: mkdir nestedFolderMe"));
    }

    @Test
    @DisplayName("skips the suggestion when its own pattern would reject it")
    void omitsASuggestionThatWouldNotBeAccepted() {
        Folder folder = new Folder("root");
        // "myFile" has no extension, so touch would reject the suggestion too.
        Scanner input = keystrokes("touch my file", "touch myFile.txt");

        String output = captureOutput(() -> new TouchCommand().run(new Navigator(folder), input));

        assertFalse(output.contains("Did you mean"), "it must not suggest a name it forbids");
        assertTrue(output.contains("Here's an example: touch chicken.leg"), "it falls back to the example");
    }

    // ---------- the template is open for extension ----------

    /**
     * A brand-new command can be added without touching AbstractCommand at all -
     * the Open/Closed Principle in action. This subclass exists only to prove the
     * extension point works.
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
    @DisplayName("a new subclass inherits the whole algorithm")
    void anyNewSubclassInheritsTheWholeAlgorithm() {
        Folder folder = new Folder("root");
        Scanner input = keystrokes("shout lowercase", "shout HELLO");

        String output = captureOutput(() -> new ShoutCommand().run(new Navigator(folder), input));

        assertTrue(output.contains("Not quite - the format was off."), "its own pattern is enforced");
        assertTrue(output.contains("Message Successfully created!"), "its own noun is capitalised");
        assertTrue(folder.hasFile("HELLO"));
    }

    @Test
    @DisplayName("dispatches polymorphically through the Command interface")
    void runsThroughTheInterfaceReference() {
        Folder folder = new Folder("root");
        // Declared as Command; the correct run() is chosen at runtime.
        Command touch = new TouchCommand();
        Command mkdir = new MkDirCommand();

        captureOutput(() -> touch.run(new Navigator(folder), keystrokes("touch hello.txt")));
        captureOutput(() -> mkdir.run(new Navigator(folder), keystrokes("mkdir projects")));

        assertTrue(folder.hasFile("hello.txt"));
        assertTrue(folder.hasSubFolder("projects"));
    }
}
