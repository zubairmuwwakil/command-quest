package ca.zubairm.command_quest.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ViewCommand.execute() - the ls lesson.
 *
 * ls is the odd one out: it is the only command that reports on the folder
 * without changing it, so "succeeded" here means the player typed ls correctly,
 * not that anything was created.
 */
@DisplayName("ViewCommand.execute()")
class ViewCommandTest {

    @Test
    @DisplayName("lists files and subfolders when the player types ls")
    void listsTheFolderContents() {
        Folder folder = new Folder("root");
        folder.addFile("todo.md");
        folder.addSubFolder("photos");

        CommandResult result = new ViewCommand().execute(folder, "ls");

        assertTrue(result.succeeded(), "typing ls correctly passes the lesson");
        assertTrue(result.output().contains("todo.md"));
        assertTrue(result.output().contains("photos"));
    }

    @Test
    @DisplayName("says the folder is empty rather than printing bare headings")
    void reportsAnEmptyFolder() {
        CommandResult result = new ViewCommand().execute(new Folder("root"), "ls");

        assertTrue(result.succeeded());
        assertTrue(result.output().contains("empty"),
                "an empty folder should say so, got:\n" + result.output());
    }

    @Test
    @DisplayName("rejects anything that is not ls instead of silently doing nothing")
    void rejectsAnythingElse() {
        Folder folder = new Folder("root");
        folder.addFile("todo.md");

        CommandResult result = new ViewCommand().execute(folder, "list");

        assertFalse(result.succeeded());
        assertFalse(result.finished(), "the player should get another go");
        assertEquals("ls", result.hint(), "the hint is the command being taught");
        assertFalse(result.output().contains("todo.md"), "nothing should be listed");
    }

    @Test
    @DisplayName("treats * as giving up")
    void starCancels() {
        CommandResult result = new ViewCommand().execute(new Folder("root"), "*");

        assertFalse(result.succeeded());
        assertTrue(result.finished());
        assertEquals("Returning to the main menu...", result.output());
    }

    // ------------------------------------------------------------ hidden entries

    /**
     * A leading dot means hidden, the same as it does in a real shell. This is
     * what keeps the .team folder a secret rather than the first thing a player
     * sees when they type ls in root.
     */
    @Test
    @DisplayName("plain ls hides dot-prefixed entries")
    void hidesDotEntries() {
        Folder folder = new Folder("root");
        folder.addFile("todo.md");
        folder.addFile(".secret.txt");
        folder.addSubFolder(".team");
        folder.addSubFolder("photos");

        CommandResult result = new ViewCommand().execute(folder, "ls");

        assertTrue(result.succeeded());
        assertTrue(result.output().contains("todo.md"), "ordinary files still show");
        assertTrue(result.output().contains("photos"), "ordinary folders still show");
        assertFalse(result.output().contains(".team"),
                "a dot folder must stay hidden; got:\n" + result.output());
        assertFalse(result.output().contains(".secret.txt"),
                "a dot file must stay hidden; got:\n" + result.output());
    }

    @Test
    @DisplayName("ls -a reveals the hidden entries")
    void dashARevealsEverything() {
        Folder folder = new Folder("root");
        folder.addFile("todo.md");
        folder.addSubFolder(".team");

        CommandResult result = new ViewCommand().execute(folder, "ls -a");

        assertTrue(result.succeeded(), "ls -a is a correct command, not a mistake");
        assertTrue(result.output().contains(".team"),
                "-a is what makes hidden entries visible; got:\n" + result.output());
        assertTrue(result.output().contains("todo.md"), "-a shows the ordinary ones too");
    }

    /**
     * A folder holding nothing but hidden entries looks empty, and saying so is
     * better than printing two bare headings with nothing under them.
     */
    @Test
    @DisplayName("a folder of only hidden entries reads as empty")
    void hiddenOnlyFolderReadsAsEmpty() {
        Folder folder = new Folder("root");
        folder.addSubFolder(".team");

        CommandResult result = new ViewCommand().execute(folder, "ls");

        assertTrue(result.succeeded());
        assertTrue(result.output().contains("empty"), result.output());
        assertFalse(result.output().contains(".team"), result.output());
    }
}
