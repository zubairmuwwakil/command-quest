package ca.zubairm.command_quest.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Folder data model.
 *
 * Folder is the project's core data structure: a List for files and a Map for
 * subfolders. These tests pin down the behaviour of both.
 */
@DisplayName("Folder")
class FolderTest {

    @Test
    @DisplayName("remembers the name it was constructed with")
    void keepsItsName() {
        assertEquals("root", new Folder("root").getName());
    }

    @Test
    @DisplayName("starts with no files")
    void startsWithNoFiles() {
        assertTrue(new Folder("root").getFiles().isEmpty());
    }

    @Test
    @DisplayName("starts with no subfolders")
    void startsWithNoSubFolders() {
        assertTrue(new Folder("root").getSubFolders().isEmpty());
    }

    // ---------- files (backed by a List) ----------

    @Test
    @DisplayName("reports a file it contains")
    void hasFileFindsAnAddedFile() {
        Folder folder = new Folder("root");
        folder.addFile("notes.txt");

        assertTrue(folder.hasFile("notes.txt"));
    }

    @Test
    @DisplayName("does not report a file it never received")
    void hasFileRejectsAnUnknownFile() {
        Folder folder = new Folder("root");
        folder.addFile("notes.txt");

        assertFalse(folder.hasFile("todo.md"));
    }

    @Test
    @DisplayName("matches file names exactly, including case")
    void hasFileIsCaseSensitive() {
        Folder folder = new Folder("root");
        folder.addFile("notes.txt");

        assertFalse(folder.hasFile("NOTES.TXT"));
    }

    @Test
    @DisplayName("keeps files in the order they were added")
    void preservesFileInsertionOrder() {
        Folder folder = new Folder("root");
        folder.addFile("zebra.txt");
        folder.addFile("apple.txt");

        assertEquals(java.util.List.of("zebra.txt", "apple.txt"), folder.getFiles());
    }

    /**
     * Characterisation test: files are stored in a List, which permits
     * duplicates. Nothing in Folder prevents the same name twice - the game
     * avoids it only because AbstractCommand checks hasFile() before calling
     * addFile(). A Set would enforce the rule structurally instead.
     */
    @Test
    @DisplayName("does not itself prevent duplicate file names (List, not Set)")
    void addFileDoesNotDeduplicate() {
        Folder folder = new Folder("root");
        folder.addFile("notes.txt");
        folder.addFile("notes.txt");

        assertEquals(2, folder.getFiles().size());
    }

    // ---------- subfolders (backed by a Map) ----------

    @Test
    @DisplayName("reports a subfolder it contains")
    void hasSubFolderFindsAnAddedFolder() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");

        assertTrue(folder.hasSubFolder("projects"));
    }

    @Test
    @DisplayName("does not report a subfolder it never received")
    void hasSubFolderRejectsAnUnknownFolder() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");

        assertFalse(folder.hasSubFolder("games"));
    }

    @Test
    @DisplayName("stores each subfolder under its own name as the Map key")
    void keysSubFoldersByName() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");

        assertTrue(folder.getSubFolders().containsKey("projects"));
    }

    @Test
    @DisplayName("builds a real Folder object for each subfolder")
    void createsAFolderObjectForEachSubFolder() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");

        Folder child = folder.getSubFolders().get("projects");
        assertNotNull(child);
        assertEquals("projects", child.getName());
    }

    @Test
    @DisplayName("gives each new subfolder its own empty contents")
    void newSubFolderStartsEmpty() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");

        assertTrue(folder.getSubFolders().get("projects").getFiles().isEmpty());
    }

    /**
     * The Map key is the folder name, so adding the same name twice replaces
     * rather than duplicates. This is the behavioural difference from the List
     * used for files, and the reason a Map is the right choice here.
     */
    @Test
    @DisplayName("overwrites rather than duplicates when a folder name repeats")
    void addSubFolderReplacesOnDuplicateName() {
        Folder folder = new Folder("root");
        folder.addSubFolder("projects");
        folder.addSubFolder("projects");

        assertEquals(1, folder.getSubFolders().size());
    }

    @Test
    @DisplayName("keeps files and subfolders in separate namespaces")
    void filesAndFoldersDoNotCollide() {
        Folder folder = new Folder("root");
        folder.addFile("projects");
        folder.addSubFolder("projects");

        assertTrue(folder.hasFile("projects"));
        assertTrue(folder.hasSubFolder("projects"));
    }
}
