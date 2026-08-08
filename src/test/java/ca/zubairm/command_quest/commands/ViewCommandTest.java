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
}
