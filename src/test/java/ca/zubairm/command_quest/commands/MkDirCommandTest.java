package ca.zubairm.command_quest.commands;

import static ca.zubairm.command_quest.TestSupport.captureOutput;
import static ca.zubairm.command_quest.TestSupport.keystrokes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the parts MkDirCommand supplies to the template: the "mkdir"
 * keyword, the bare-word pattern, and hooks that read and write the folder's
 * SUBFOLDER map.
 */
@DisplayName("MkDirCommand")
class MkDirCommandTest {

    @ParameterizedTest(name = "accepts \"{0}\"")
    @ValueSource(strings = { "projects", "my_folder", "abc123", "UScream4IceCream" })
    @DisplayName("accepts bare word folder names")
    void acceptsWellFormedFolderNames(String folderName) {
        Folder folder = new Folder("root");

        captureOutput(() -> new MkDirCommand().run(folder, keystrokes("mkdir " + folderName)));

        assertTrue(folder.hasSubFolder(folderName));
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @ValueSource(strings = { "has.dot", "has-dash", "has/slash", "has!bang" })
    @DisplayName("rejects folder names containing punctuation")
    void rejectsMalformedFolderNames(String folderName) {
        Folder folder = new Folder("root");

        String output = captureOutput(
                () -> new MkDirCommand().run(folder, keystrokes("mkdir " + folderName, "mkdir ok")));

        assertTrue(output.contains("Not quite - the format was off."),
                "expected \"" + folderName + "\" to be rejected");
        assertFalse(folder.hasSubFolder(folderName));
    }

    /**
     * The two commands' patterns cannot overlap: mkdir forbids the dot that
     * touch requires. That is why a file and a folder can never end up sharing
     * a name even though Folder keeps them in separate collections.
     */
    @Test
    @DisplayName("cannot create a folder named like a file")
    void rejectsFileStyleNames() {
        Folder folder = new Folder("root");

        String output = captureOutput(
                () -> new MkDirCommand().run(folder, keystrokes("mkdir notes.txt", "mkdir notes")));

        assertTrue(output.contains("Not quite - the format was off."));
        assertTrue(folder.hasSubFolder("notes"));
    }

    @Test
    @DisplayName("does not answer to the touch keyword")
    void ignoresTheOtherCommandsKeyword() {
        Folder folder = new Folder("root");

        String output = captureOutput(
                () -> new MkDirCommand().run(folder, keystrokes("touch projects", "mkdir projects")));

        assertTrue(output.contains("Not quite - the format was off."));
    }

    @Test
    @DisplayName("reports an existing folder as a duplicate")
    void reportsDuplicateFolder() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");

        String output = captureOutput(
                () -> new MkDirCommand().run(folder, keystrokes("mkdir projects", "mkdir games")));

        assertTrue(output.contains("folder already exists"),
                "expected the duplicate warning, got:\n" + output);
        assertTrue(folder.hasSubFolder("games"));
    }

    @Test
    @DisplayName("looks for duplicates among folders, not files")
    void existsChecksTheSubFolderMap() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");

        MkDirCommand command = new MkDirCommand();
        assertTrue(command.exists(folder, "projects"));
        assertFalse(command.exists(folder, "games"));
    }

    @Test
    @DisplayName("is not confused by a file of the same name")
    void existsIgnoresFiles() {
        Folder folder = new Folder("root");
        folder.addFile("projects");

        assertFalse(new MkDirCommand().exists(folder, "projects"),
                "a file called projects must not block a FOLDER called projects");
    }

    @Test
    @DisplayName("creates a folder rather than a file")
    void createAddsToTheSubFolderMap() {
        Folder folder = new Folder("root");

        new MkDirCommand().create(folder, "projects");

        assertTrue(folder.hasSubFolder("projects"));
        assertFalse(folder.hasFile("projects"));
    }

    @Test
    @DisplayName("confirms success using its own noun")
    void announcesFolderNotFile() {
        Folder folder = new Folder("root");

        String output = captureOutput(() -> new MkDirCommand().run(folder, keystrokes("mkdir projects")));

        assertTrue(output.contains("Folder Successfully created!"),
                "expected the folder-specific message, got:\n" + output);
    }
}
