package ca.zubairm.command_quest.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import ca.zubairm.command_quest.Credits;
import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for Shell - the thing that decides which command a typed line reaches.
 *
 * The point of this class is what it does NOT take: there is no lesson
 * parameter anywhere in execute(). Whichever command the first token names is
 * the command that runs, and no caller can narrow that. Everything else here
 * is about being useful when the token names nothing at all.
 */
@DisplayName("Shell.execute()")
class ShellTest {

    /** root/ containing todo.md and a photos/ subfolder. */
    private static Navigator sandbox() {
        Folder root = new Folder("root");
        root.addFile("todo.md");
        root.addSubFolder("photos");
        return new Navigator(root);
    }

    private static Shell.Result run(Navigator navigator, String input) {
        return new Shell().execute(navigator, input);
    }

    // ------------------------------------------------------------ dispatch

    @Test
    @DisplayName("touch reaches TouchCommand")
    void dispatchesTouch() {
        Navigator navigator = sandbox();

        Shell.Result run = run(navigator, "touch cat.jpg");

        assertEquals("touch", run.commandId());
        assertTrue(run.result().succeeded());
        assertTrue(navigator.current().hasFile("cat.jpg"));
    }

    @Test
    @DisplayName("mkdir reaches MkDirCommand")
    void dispatchesMkdir() {
        Navigator navigator = sandbox();

        Shell.Result run = run(navigator, "mkdir homework");

        assertEquals("mkdir", run.commandId());
        assertTrue(run.result().succeeded());
        assertTrue(navigator.current().hasSubFolder("homework"));
    }

    @Test
    @DisplayName("ls reaches ViewCommand")
    void dispatchesLs() {
        Shell.Result run = run(sandbox(), "ls");

        assertEquals("ls", run.commandId());
        assertTrue(run.result().succeeded());
        assertTrue(run.result().output().contains("todo.md"));
    }

    @Test
    @DisplayName("cd reaches CdCommand and actually moves the player")
    void dispatchesCd() {
        Navigator navigator = sandbox();

        Shell.Result run = run(navigator, "cd photos");

        assertEquals("cd", run.commandId());
        assertTrue(run.result().succeeded());
        assertEquals("photos", navigator.current().getName());
    }

    @Test
    @DisplayName("pwd reaches PwdCommand")
    void dispatchesPwd() {
        Shell.Result run = run(sandbox(), "pwd");

        assertEquals("pwd", run.commandId());
        assertTrue(run.result().succeeded());
        assertEquals("root", run.result().output());
    }

    @Test
    @DisplayName("nothing narrows what may be typed - there is no lesson to be on")
    void anyCommandRunsAtAnyTime() {
        Navigator navigator = sandbox();
        Shell shell = new Shell();

        // The same Shell, in one session, running every command in turn. Under
        // the old controller each of these needed its own matching lessonId.
        assertTrue(shell.execute(navigator, "touch a.txt").result().succeeded());
        assertTrue(shell.execute(navigator, "mkdir b").result().succeeded());
        assertTrue(shell.execute(navigator, "ls").result().succeeded());
        assertTrue(shell.execute(navigator, "cd b").result().succeeded());
        assertTrue(shell.execute(navigator, "pwd").result().succeeded());
    }

    @Test
    @DisplayName("the command sees the whole line, not just the keyword")
    void passesTheWholeLineThrough() {
        Navigator navigator = sandbox();

        run(navigator, "touch  spaced.txt");

        assertTrue(navigator.current().hasFile("spaced.txt"));
    }

    @Test
    @DisplayName("a recognised command that fails still reports which command it was")
    void namesTheCommandEvenOnFailure() {
        Shell.Result run = run(sandbox(), "mkdir");

        assertEquals("mkdir", run.commandId(), "the lesson panel needs this to follow the player");
        assertFalse(run.result().succeeded());
    }

    // ------------------------------------------------------------ unknown

    @Test
    @DisplayName("an unknown command is rejected, not routed anywhere")
    void rejectsAnUnknownCommand() {
        Shell.Result run = run(sandbox(), "rm notes.txt");

        assertNull(run.commandId(), "nothing ran");
        assertFalse(run.result().succeeded());
        assertFalse(run.result().finished(), "the player should get another go");
        assertTrue(run.result().output().contains("rm"), "the message names what was typed");
        assertEquals("help", run.result().hint());
    }

    @Test
    @DisplayName("an unknown command lists the whole vocabulary")
    void unknownCommandListsTheVocabulary() {
        String output = run(sandbox(), "xyzzy").result().output();

        for (String known : List.of("touch", "mkdir", "ls", "cd", "pwd", "help")) {
            assertTrue(output.contains(known), "the message should mention " + known);
        }
    }

    // ------------------------------------------------------------ near misses

    @Test
    @DisplayName("a one-letter slip suggests the command meant")
    void suggestsOnASubstitution() {
        String output = run(sandbox(), "mkdr homework").result().output();

        assertTrue(output.contains("Did you mean"), output);
        assertTrue(output.contains("mkdir"), output);
    }

    @Test
    @DisplayName("two swapped letters suggest the command meant")
    void suggestsOnATransposition() {
        // Plain Levenshtein scores this 2, which is over the threshold for a
        // two-letter keyword. Damerau scores a transposition 1, which is why
        // this test exists.
        String output = run(sandbox(), "sl").result().output();

        assertTrue(output.contains("Did you mean"), output);
        assertTrue(output.contains("ls"), output);
    }

    @Test
    @DisplayName("help is itself close enough to suggest")
    void suggestsHelp() {
        String output = run(sandbox(), "hlep").result().output();

        assertTrue(output.contains("Did you mean"), output);
        assertTrue(output.contains("help"), output);
    }

    @Test
    @DisplayName("the wrong case is a near miss, not a mystery")
    void suggestsOnACaseSlip() {
        // Dispatch stays case-sensitive, like a real shell. But telling someone
        // with caps lock on that TOUCH is simply unknown teaches them nothing.
        Shell.Result run = run(sandbox(), "TOUCH cat.jpg");

        assertNull(run.commandId(), "it did not run - case still matters");
        assertTrue(run.result().output().contains("Did you mean"), run.result().output());
        assertTrue(run.result().output().contains("touch"), run.result().output());
    }

    @Test
    @DisplayName("a command two letters from a two-letter keyword is not a near miss")
    void doesNotGuessWildly() {
        // "rm" is distance 2 from both "ls" and "cd". A flat threshold of 2
        // would confidently suggest an unrelated command.
        String output = run(sandbox(), "rm").result().output();

        assertFalse(output.contains("Did you mean"), output);
    }

    @Test
    @DisplayName("something nothing like a command gets no guess")
    void offersNoSuggestionForNonsense() {
        String output = run(sandbox(), "xyzzy").result().output();

        assertFalse(output.contains("Did you mean"), output);
    }

    // ------------------------------------------------------------ help

    @Test
    @DisplayName("help lists every command")
    void helpListsEverything() {
        Shell.Result run = run(sandbox(), "help");

        assertEquals("help", run.commandId());
        assertTrue(run.result().succeeded());

        for (String known : List.of("touch", "mkdir", "ls", "cd", "pwd")) {
            assertTrue(run.result().output().contains(known), "help should mention " + known);
        }
    }

    // ------------------------------------------------------------ edges

    @Test
    @DisplayName("* is unknown input, not a console menu escape")
    void starIsNotAMenuEscape() {
        Shell.Result run = run(sandbox(), "*");

        assertNull(run.commandId());
        assertFalse(run.result().output().contains("main menu"),
                "the browser has no main menu to return to");
    }

    @Test
    @DisplayName("blank input asks for a command without quoting an empty string")
    void handlesBlankInput() {
        Shell.Result run = run(sandbox(), "   ");

        assertNull(run.commandId());
        assertFalse(run.result().succeeded());
        assertFalse(run.result().output().contains("\"\""), run.result().output());
        assertTrue(run.result().output().contains("help"));
    }

    @Test
    @DisplayName("the vocabulary is listed in a stable, deliberate order")
    void keywordsAreOrdered() {
        // Map.of() makes no ordering guarantee, so this would fail intermittently
        // across JVM runs if the registry were built with it.
        assertEquals(List.of("touch", "mkdir", "ls", "cd", "pwd"), new Shell().keywords());
    }

    // ------------------------------------------------------------ hidden

    /**
     * The credits command is meant to be found, not advertised. Dispatch has to
     * know it while every list the Shell reads aloud does not - which is why it
     * lives in a second map rather than in the registry.
     */
    @Test
    @DisplayName("credits runs and names the whole team")
    void creditsRuns() {
        Shell.Result run = run(sandbox(), "credits");

        assertEquals("credits", run.commandId());
        assertTrue(run.result().succeeded());

        for (String member : Credits.members()) {
            assertTrue(run.result().output().contains(member),
                    "credits should name " + member + "; got:\n" + run.result().output());
        }
    }

    @Test
    @DisplayName("credits is not listed in help")
    void creditsStaysOutOfHelp() {
        String output = run(sandbox(), "help").result().output();

        assertFalse(output.contains("credits"),
                "a command listed in help is not hidden; got:\n" + output);
    }

    @Test
    @DisplayName("credits is not listed when an unknown command is rejected")
    void creditsStaysOutOfTheVocabulary() {
        String output = run(sandbox(), "xyzzy").result().output();

        assertFalse(output.contains("credits"),
                "the vocabulary list would give it away; got:\n" + output);
    }

    @Test
    @DisplayName("credits is never offered as a near miss")
    void creditsIsNeverSuggested() {
        // One letter short of "credits". The suggester walks the vocabulary, so
        // this is the check that the hidden map is genuinely outside it.
        String output = run(sandbox(), "credit").result().output();

        assertFalse(output.contains("Did you mean"), output);
        assertFalse(output.contains("credits"), output);
    }

    @Test
    @DisplayName("credits is not a lesson tab")
    void creditsIsNotALesson() {
        assertFalse(new Shell().keywords().contains("credits"),
                "keywords() drives the browser's lesson tabs");
    }
}
