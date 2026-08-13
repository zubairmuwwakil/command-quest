package ca.zubairm.command_quest.commands;

import static ca.zubairm.command_quest.TestSupport.captureOutput;
import static ca.zubairm.command_quest.TestSupport.keystrokes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the parts TouchCommand supplies to the template: the "touch"
 * keyword, the "name.extension" pattern, and hooks that read and write the
 * folder's FILE list.
 */
@DisplayName("TouchCommand")
class TouchCommandTest {

    @ParameterizedTest(name = "accepts \"{0}\"")
    @ValueSource(strings = { "hello.txt", "a.b", "my_notes.md", "file123.jpg", "123.456" })
    @DisplayName("accepts any name.extension pair")
    void acceptsWellFormedFileNames(String fileName) {
        Folder folder = new Folder("root");

        captureOutput(() -> new TouchCommand().run(new Navigator(folder), keystrokes("touch " + fileName)));

        assertTrue(folder.hasFile(fileName));
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @ValueSource(strings = { "noextension", "a.b.c", "has-dash.txt", ".txt", "trailingdot." })
    @DisplayName("rejects names that are not exactly name.extension")
    void rejectsMalformedFileNames(String fileName) {
        Folder folder = new Folder("root");
        // second line is a valid command so run() can terminate
        String output = captureOutput(
                () -> new TouchCommand().run(new Navigator(folder), keystrokes("touch " + fileName, "touch ok.txt")));

        assertTrue(output.contains("Not quite - the format was off."),
                "expected \"" + fileName + "\" to be rejected");
        assertFalse(folder.hasFile(fileName));
    }

    @Test
    @DisplayName("does not answer to the mkdir keyword")
    void ignoresTheOtherCommandsKeyword() {
        Folder folder = new Folder("root");

        String output = captureOutput(
                () -> new TouchCommand().run(new Navigator(folder), keystrokes("mkdir hello.txt", "touch hello.txt")));

        assertTrue(output.contains("Not quite - the format was off."));
    }

    @Test
    @DisplayName("looks for duplicates among files, not folders")
    void existsChecksTheFileList() {
        Folder folder = new Folder("root");
        folder.addFile("hello.txt");

        TouchCommand command = new TouchCommand();
        assertTrue(command.exists(folder, "hello.txt"));
        assertFalse(command.exists(folder, "missing.txt"));
    }

    @Test
    @DisplayName("is not confused by a subfolder of the same name")
    void existsIgnoresSubFolders() {
        Folder folder = new Folder("root");
        folder.addSubFolder("hello.txt");

        assertFalse(new TouchCommand().exists(folder, "hello.txt"),
                "a folder called hello.txt must not block a FILE called hello.txt");
    }

    @Test
    @DisplayName("creates a file rather than a folder")
    void createAddsToTheFileList() {
        Folder folder = new Folder("root");

        new TouchCommand().create(folder, "hello.txt");

        assertTrue(folder.hasFile("hello.txt"));
        assertFalse(folder.hasSubFolder("hello.txt"));
    }
}
