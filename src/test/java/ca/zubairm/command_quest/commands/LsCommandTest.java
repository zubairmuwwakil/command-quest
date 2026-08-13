package ca.zubairm.command_quest.commands;

import static ca.zubairm.command_quest.TestSupport.captureOutput;
import static ca.zubairm.command_quest.TestSupport.keystrokes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for LsCommand.
 *
 * ls implements Command directly rather than extending AbstractCommand,
 * because the template there is "parse keyword + name, then create something"
 * and ls creates nothing. That means the retry loop and the corrective
 * messages AbstractCommand hands to mkdir and touch have to be written here -
 * these tests pin down that they exist.
 */
@DisplayName("LsCommand")
class LsCommandTest {

    @Test
    @DisplayName("corrects an unknown command and asks again")
    void teachesAfterAnUnknownCommand() {
        Folder folder = new Folder("root");
        folder.addFile("notes.txt");

        String output = captureOutput(
                () -> new LsCommand().run(new Navigator(folder), keystrokes("dir", "ls")));

        assertTrue(output.contains("Not quite"),
                "an unrecognised command must be corrected, not ignored; got:\n" + output);
        assertTrue(output.contains("- notes.txt"),
                "after correcting, ls must keep listening so the retry works; got:\n" + output);
    }

    /**
     * The retry loop must not trap the player. Both ways out are guarded here
     * because each is now a return from INSIDE the loop rather than the method
     * simply running off its end - drop either one and the scanner runs dry.
     */
    @Test
    @DisplayName("leaves the loop when the player types *")
    void starReturnsToTheMenu() {
        Folder folder = new Folder("root");

        String output = captureOutput(
                () -> new LsCommand().run(new Navigator(folder), keystrokes("*")));

        assertTrue(output.contains("Returning to the main menu..."));
    }

    @Test
    @DisplayName("leaves the loop once ls has listed the folder")
    void listingEndsTheLesson() {
        Folder folder = new Folder("root");
        folder.addFile("notes.txt");
        folder.addSubFolder("projects");

        String output = captureOutput(
                () -> new LsCommand().run(new Navigator(folder), keystrokes("ls")));

        assertTrue(output.contains("- notes.txt"), "files are listed");
        assertTrue(output.contains("- projects"), "subfolders are listed");
    }

    /** ls reports the folder you are standing in, not the root you started in. */
    @Test
    @DisplayName("lists the folder the navigator is currently in")
    void listsTheCurrentFolder() {
        Folder root = new Folder("root");
        root.addFile("top.txt");
        root.addSubFolder("projects");
        root.getSubFolder("projects").addFile("inside.txt");

        Navigator navigator = new Navigator(root);
        navigator.into("projects");

        String output = captureOutput(() -> new LsCommand().run(navigator, keystrokes("ls")));

        assertTrue(output.contains("- inside.txt"), "expected the subfolder's file; got:\n" + output);
        assertFalse(output.contains("- top.txt"), "root's file must not leak in; got:\n" + output);
    }
}
