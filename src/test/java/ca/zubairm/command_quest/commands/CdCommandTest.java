package ca.zubairm.command_quest.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for CdCommand.execute() - the navigation lesson.
 *
 * cd is the one command that takes a Navigator rather than a Folder, because
 * it changes WHERE you are rather than WHAT is in front of you. These tests
 * are consequently also the first coverage Navigator has had.
 */
@DisplayName("CdCommand.execute()")
class CdCommandTest {

    /** root/ with a photos/ subfolder inside it. */
    private static Navigator rootWithPhotos() {
        Folder root = new Folder("root");
        root.addSubFolder("photos");
        return new Navigator(root);
    }

    @Test
    @DisplayName("moves into a subfolder that exists")
    void entersAnExistingSubfolder() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "cd photos");

        assertTrue(result.succeeded());
        assertEquals("photos", navigator.current().getName(), "the navigator actually moved");
        assertTrue(result.output().contains("root/photos"), "the breadcrumb is shown");
    }

    @Test
    @DisplayName("refuses to enter a folder that is not there")
    void refusesAnUnknownFolder() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "cd nowhere");

        assertFalse(result.succeeded());
        assertFalse(result.finished(), "the player should get another go");
        assertTrue(result.output().contains("nowhere"), "the message names the missing folder");
        assertEquals("root", navigator.current().getName(), "the navigator did not move");
    }

    @Test
    @DisplayName("cd .. goes back up a level")
    void goesUpALevel() {
        Navigator navigator = rootWithPhotos();
        new CdCommand().execute(navigator, "cd photos");

        CommandResult result = new CdCommand().execute(navigator, "cd ..");

        assertTrue(result.succeeded());
        assertEquals("root", navigator.current().getName());
    }

    @Test
    @DisplayName("cd .. at root reports there is nowhere to go")
    void refusesToGoAboveRoot() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "cd ..");

        assertFalse(result.succeeded());
        assertTrue(result.output().contains("Already at root"));
        assertEquals("root", navigator.current().getName());
    }

    @Test
    @DisplayName("cd / jumps all the way back to root")
    void jumpsToRoot() {
        Navigator navigator = rootWithPhotos();
        new CdCommand().execute(navigator, "cd photos");

        CommandResult result = new CdCommand().execute(navigator, "cd /");

        assertTrue(result.succeeded());
        assertEquals("root", navigator.current().getName());
    }

    @Test
    @DisplayName("rejects a line that is not a cd command")
    void rejectsAMalformedCommand() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "goto photos");

        assertFalse(result.succeeded());
        assertFalse(result.finished());
        assertEquals("root", navigator.current().getName());
    }

    // ---------- the extra-arguments branch ----------

    /**
     * The target here deliberately EXISTS. "cd nowhere extra" would be refused
     * either way - the old guard read tokens[1] and complained the folder was
     * missing - so it proves nothing. Only a real folder followed by a stray
     * word can tell "the line was refused" apart from "the first word happened
     * to be wrong anyway".
     */
    @Test
    @DisplayName("refuses a target typed with spaces instead of entering the first word")
    void refusesATargetTypedWithSpaces() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "cd photos extra");

        assertFalse(result.succeeded());
        assertFalse(result.finished(), "the player should get another go");
        assertEquals("root", navigator.current().getName(),
                "the spaced line should move the player nowhere at all");
    }

    @Test
    @DisplayName("explains that a space starts a new word")
    void explainsWhySpacesAreTheProblem() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "cd my photos");

        assertTrue(result.output().contains("moves to one place at a time"),
                "expected the spacing lesson, got:\n" + result.output());
        assertTrue(result.output().contains("2 places"),
                "expected the player's own word count back, got:\n" + result.output());
        assertTrue(result.output().contains("my, photos"),
                "expected the words read back as separate targets, got:\n" + result.output());
    }

    /** cd only ever takes one target, and ".." is no exception. */
    @Test
    @DisplayName("refuses extra words after .. as well")
    void refusesExtraWordsAfterDotDot() {
        Navigator navigator = rootWithPhotos();
        new CdCommand().execute(navigator, "cd photos");

        CommandResult result = new CdCommand().execute(navigator, "cd .. ..");

        assertFalse(result.succeeded());
        assertEquals("photos", navigator.current().getName(), "the player did not move");
    }

    /**
     * CdCommand has no business lecturing about another command's spacing - a
     * line that is not a cd command at all gets the plain format error.
     */
    @Test
    @DisplayName("a non-cd line with extra words is still a plain format error")
    void wrongKeywordWithExtraWordsIsAPlainFormatError() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "mkdir a b c");

        assertFalse(result.succeeded());
        assertTrue(result.output().contains("Not quite - try:"),
                "expected the generic format error, got:\n" + result.output());
    }

    @Test
    @DisplayName("treats * as giving up")
    void starCancels() {
        Navigator navigator = rootWithPhotos();

        CommandResult result = new CdCommand().execute(navigator, "*");

        assertFalse(result.succeeded());
        assertTrue(result.finished());
        assertEquals("Returning to the main menu...", result.output());
    }
}
