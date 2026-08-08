package ca.zubairm.command_quest;

import static ca.zubairm.command_quest.TestSupport.runApp;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * End-to-end tests for App.main() - the menu loop that ties everything
 * together.
 *
 * Each test scripts a full session: an operating-system choice, some menu
 * selections, and finally "0" to quit.
 *
 * DISABLED on the web-deploy branch: these tests describe the app as it was at
 * b9094b3, and main has since changed the flow underneath them.
 *
 *   - The operating-system prompt was deleted in bf2cf86, so the six tests for
 *     it assert on a feature that no longer exists.
 *   - Menu option 4 is now cd and option 5 was removed, so "coming soon" is
 *     never printed.
 *   - Listing moved into ViewCommand, which prints "Files in the current
 *     folder:" and no longer suffixes folders with "/", so every listing
 *     assertion targets replaced output. It also reads its own input line, so
 *     the scripts need an extra "ls".
 *   - App now opens with a login gate, so every script needs a leading "3"
 *     (Continue as Guest) before it reaches the menu.
 *
 * These are not failures of the code - the code moved on and the tests did not
 * follow. Reviving them means rewriting them against the current flow, which is
 * a deliberate decision rather than a mechanical fix. The 66 domain tests in
 * commands/ and hub/ are unaffected and remain the safety net for the
 * execute(Folder, String) refactor.
 */
@Disabled("Describes the pre-bf2cf86 console flow; see the class comment above")
@DisplayName("App")
class AppTest {

    // ---------- startup and the operating system prompt ----------

    @Test
    @DisplayName("greets the player on startup")
    void printsTheWelcomeMessage() {
        String output = runApp("1", "0");

        assertTrue(output.contains("Welcome to Command Quest!"));
    }

    @ParameterizedTest(name = "accepts operating system choice {0}")
    @ValueSource(strings = { "1", "2", "3" })
    @DisplayName("accepts each of the three offered operating systems")
    void acceptsEveryValidOperatingSystem(String choice) {
        String output = runApp(choice, "0");

        assertTrue(output.contains("Thanks! :) I hope you enjoy your journey"));
    }

    @Test
    @DisplayName("re-prompts when the operating system is not a number")
    void rejectsNonNumericOperatingSystem() {
        String output = runApp("banana", "1", "0");

        assertTrue(output.contains("Please enter a valid number."));
        assertTrue(output.contains("Thanks! :) I hope you enjoy your journey"), "should continue after the retry");
    }

    @Test
    @DisplayName("re-prompts when the operating system is out of range")
    void rejectsOutOfRangeOperatingSystem() {
        String output = runApp("9", "1", "0");

        // 9 parses fine, so no error text - the loop simply asks again.
        assertFalse(output.contains("Please enter a valid number."));
        assertTrue(output.contains("Thanks! :) I hope you enjoy your journey"));
    }

    @Test
    @DisplayName("tolerates whitespace around the operating system choice")
    void trimsTheOperatingSystemChoice() {
        String output = runApp("  1  ", "0");

        assertTrue(output.contains("Thanks! :) I hope you enjoy your journey"));
    }

    // ---------- the main menu ----------

    @Test
    @DisplayName("says goodbye and stops on option 0")
    void exitsOnZero() {
        String output = runApp("1", "0");

        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    @DisplayName("reports an unrecognised menu number")
    void rejectsUnknownMenuOption() {
        String output = runApp("1", "42", "0");

        assertTrue(output.contains("Unknown option."));
    }

    @Test
    @DisplayName("reports non-numeric menu input")
    void rejectsNonNumericMenuOption() {
        String output = runApp("1", "banana", "0");

        assertTrue(output.contains("Please enter a number."));
    }

    @Test
    @DisplayName("keeps running after a bad menu entry")
    void survivesABadMenuEntry() {
        String output = runApp("1", "banana", "99", "0");

        assertTrue(output.contains("Please enter a number."));
        assertTrue(output.contains("Unknown option."));
        assertTrue(output.contains("Goodbye!"), "should still reach a clean exit");
    }

    @ParameterizedTest(name = "option {0} is not built yet")
    @ValueSource(strings = { "4", "5" })
    @DisplayName("marks the unfinished menu options as coming soon")
    void unimplementedOptionsSayComingSoon(String option) {
        String output = runApp("1", option, "0");

        assertTrue(output.contains("coming soon"));
    }

    // ---------- option 3: viewing the current folder ----------

    @Test
    @DisplayName("lists the two files the game starts with")
    void listsTheSeededFiles() {
        String output = runApp("1", "3", "0");

        assertTrue(output.contains("Here are your current files and folders!"));
        assertTrue(output.contains("notes.txt"));
        assertTrue(output.contains("todo.md"));
    }

    @Test
    @DisplayName("lists files in alphabetical order")
    void sortsTheListing() {
        String output = runApp("1", "3", "0");

        assertTrue(output.indexOf("notes.txt") < output.indexOf("todo.md"),
                "notes.txt should sort before todo.md");
    }

    // ---------- options 1 and 2 wired through the menu ----------

    @Test
    @DisplayName("creates a file through menu option 1")
    void optionOneCreatesAFile() {
        String output = runApp("1", "1", "touch mine.txt", "3", "0");

        assertTrue(output.contains("File Successfully created!"));
        assertTrue(output.contains("mine.txt"), "the new file should appear in the listing");
    }

    @Test
    @DisplayName("creates a folder through menu option 2")
    void optionTwoCreatesAFolder() {
        String output = runApp("1", "2", "mkdir projects", "3", "0");

        assertTrue(output.contains("Folder Successfully created!"));
        assertTrue(output.contains("projects/"), "folders are listed with a trailing slash");
    }

    @Test
    @DisplayName("shows folders before files in the listing")
    void listsFoldersBeforeFiles() {
        String output = runApp("1", "2", "mkdir zzz", "3", "0");

        assertTrue(output.indexOf("zzz/") < output.indexOf("notes.txt"),
                "the zzz/ folder should be printed above the files despite sorting last");
    }

    @Test
    @DisplayName("keeps created items across several menu turns")
    void remembersItemsBetweenMenuChoices() {
        String output = runApp("1", "1", "touch alpha.txt", "2", "mkdir beta", "3", "0");

        assertTrue(output.contains("alpha.txt"));
        assertTrue(output.contains("beta/"));
    }

    @Test
    @DisplayName("lets the player escape a command with * and carry on")
    void starReturnsToTheMenuFromTheApp() {
        String output = runApp("1", "1", "*", "3", "0");

        assertTrue(output.contains("Returning to the main menu..."));
        assertTrue(output.contains("Here are your current files and folders!"), "the menu should still work");
        assertTrue(output.contains("Goodbye!"));
    }
}
