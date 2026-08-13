package ca.zubairm.command_quest.commands;

import static ca.zubairm.command_quest.TestSupport.captureOutput;
import static ca.zubairm.command_quest.TestSupport.keystrokes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for CdCommand - the one command that moves between folders instead of
 * creating something inside one.
 *
 * Because it changes the Navigator rather than the Folder, every test here
 * asks navigator.breadcrumb() where the player ended up.
 *
 * A command that fails keeps asking, so any test with a bad line scripts a
 * good one after it to let run() finish.
 */
@DisplayName("CdCommand")
class CdCommandTest {

    /** root/photos/2024, so there is somewhere to go and somewhere to come back from. */
    private static Folder sampleTree() {
        Folder root = new Folder("root");
        root.addSubFolder("photos");
        root.getSubFolder("photos").addSubFolder("2024");
        return root;
    }

    // ---------- moving ----------

    @Test
    @DisplayName("cd <name> steps into a subfolder")
    void movesIntoASubfolder() {
        Navigator navigator = new Navigator(sampleTree());

        captureOutput(() -> new CdCommand().run(navigator, keystrokes("cd photos")));

        assertEquals("root/photos", navigator.breadcrumb());
    }

    @Test
    @DisplayName("cd .. steps back up to the parent")
    void movesUpToTheParent() {
        Navigator navigator = new Navigator(sampleTree());
        navigator.into("photos");

        captureOutput(() -> new CdCommand().run(navigator, keystrokes("cd ..")));

        assertEquals("root", navigator.breadcrumb());
    }

    @Test
    @DisplayName("cd / jumps all the way back to root")
    void jumpsBackToRoot() {
        Navigator navigator = new Navigator(sampleTree());
        navigator.into("photos");
        navigator.into("2024");

        captureOutput(() -> new CdCommand().run(navigator, keystrokes("cd /")));

        assertEquals("root", navigator.breadcrumb());
    }

    @Test
    @DisplayName("reports where the player ended up")
    void announcesTheNewLocation() {
        Navigator navigator = new Navigator(sampleTree());

        String output = captureOutput(() -> new CdCommand().run(navigator, keystrokes("cd photos")));

        assertTrue(output.contains("Now in: root/photos"));
    }

    // ---------- refusing to move ----------

    @Test
    @DisplayName("refuses to go above root and stays put")
    void refusesToGoAboveRoot() {
        Navigator navigator = new Navigator(sampleTree());

        String output = captureOutput(
                () -> new CdCommand().run(navigator, keystrokes("cd ..", "cd photos")));

        assertTrue(output.contains("Already at root."));
        assertEquals("root/photos", navigator.breadcrumb(), "the retry should still work");
    }

    @Test
    @DisplayName("refuses a folder name that is not here")
    void refusesAnUnknownFolder() {
        Navigator navigator = new Navigator(sampleTree());

        String output = captureOutput(
                () -> new CdCommand().run(navigator, keystrokes("cd nope", "cd photos")));

        assertTrue(output.contains("No folder named 'nope' here."));
        assertEquals("root/photos", navigator.breadcrumb(), "the retry should still work");
    }

    /**
     * "2024" exists in the tree, but under photos - not here. cd only ever
     * looks at the children of the folder you are standing in.
     */
    @Test
    @DisplayName("refuses a folder that exists elsewhere in the tree")
    void refusesAFolderFromAnotherLevel() {
        Navigator navigator = new Navigator(sampleTree());

        String output = captureOutput(
                () -> new CdCommand().run(navigator, keystrokes("cd 2024", "cd photos")));

        assertTrue(output.contains("No folder named '2024' here."));
        assertEquals("root/photos", navigator.breadcrumb());
    }

    // ---------- bad input ----------

    @ParameterizedTest(name = "corrects \"{0}\"")
    @ValueSource(strings = {
            "cd",          // no folder name
            "dir photos",  // not the cd keyword
            "photos",      // a name with no command
    })
    @DisplayName("corrects a malformed command and asks again")
    void correctsAMalformedCommand(String badLine) {
        Navigator navigator = new Navigator(sampleTree());

        String output = captureOutput(
                () -> new CdCommand().run(navigator, keystrokes(badLine, "cd photos")));

        assertTrue(output.contains("Not quite - try: cd <name>, cd .., or cd /"));
        assertEquals("root/photos", navigator.breadcrumb(), "the retry should still work");
    }

    // ---------- leaving ----------

    @Test
    @DisplayName("returns to the menu when the player types *")
    void starReturnsToTheMenu() {
        Navigator navigator = new Navigator(sampleTree());

        String output = captureOutput(() -> new CdCommand().run(navigator, keystrokes("*")));

        assertTrue(output.contains("Returning to the main menu..."));
        assertEquals("root", navigator.breadcrumb(), "escaping must not move the player");
    }
}
