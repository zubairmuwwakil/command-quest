package ca.zubairm.command_quest.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for PwdCommand.execute() - "where am I?".
 *
 * Like cd, this takes a Navigator rather than a Folder, and for the same
 * reason inverted: a Folder holds no parent reference, so it cannot report
 * where it sits. Only the Navigator knows the path.
 */
@DisplayName("PwdCommand.execute()")
class PwdCommandTest {

    /** root/ with a photos/ subfolder inside it. */
    private static Navigator rootWithPhotos() {
        Folder root = new Folder("root");
        root.addSubFolder("photos");
        return new Navigator(root);
    }

    @Test
    @DisplayName("reports root when the player has not moved")
    void reportsRoot() {
        CommandResult result = new PwdCommand().execute(rootWithPhotos(), "pwd");

        assertTrue(result.succeeded());
        assertEquals("root", result.output());
    }

    @Test
    @DisplayName("reports the full path once the player has moved")
    void reportsANestedPath() {
        Navigator navigator = rootWithPhotos();
        new CdCommand().execute(navigator, "cd photos");

        CommandResult result = new PwdCommand().execute(navigator, "pwd");

        assertTrue(result.succeeded());
        assertEquals("root/photos", result.output());
    }

    @Test
    @DisplayName("takes no arguments")
    void rejectsArguments() {
        CommandResult result = new PwdCommand().execute(rootWithPhotos(), "pwd here");

        assertFalse(result.succeeded());
        assertFalse(result.finished(), "the player should get another go");
        assertEquals("pwd", result.hint());
    }

    @Test
    @DisplayName("rejects a line that is not pwd")
    void rejectsAMisspelling() {
        CommandResult result = new PwdCommand().execute(rootWithPhotos(), "pw");

        assertFalse(result.succeeded());
        assertFalse(result.finished());
    }

    @Test
    @DisplayName("tolerates surrounding whitespace")
    void trimsInput() {
        CommandResult result = new PwdCommand().execute(rootWithPhotos(), "  pwd  ");

        assertTrue(result.succeeded());
        assertEquals("root", result.output());
    }
}
