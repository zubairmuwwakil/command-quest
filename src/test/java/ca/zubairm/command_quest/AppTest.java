package ca.zubairm.command_quest;

import static ca.zubairm.command_quest.TestSupport.runApp;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for App.main().
 *
 * Each test types a whole play-through: an answer to the login gate first,
 * then menu choices, then "0" to quit. The constants below are those menu
 * numbers, so a script reads like the keys a player actually presses.
 *
 * "0" ends whichever prompt is currently reading, so a script needs one for
 * the menu and, if it is still inside the game, one for the gate as well.
 */
@DisplayName("App")
class AppTest {

    // Login gate
    private static final String GUEST = "3";
    private static final String NEW_ACCOUNT = "1";

    // Main menu
    private static final String MAKE_FILE = "1";
    private static final String MAKE_FOLDER = "2";
    private static final String VIEW_FOLDER = "3";
    private static final String CHANGE_FOLDER = "4";
    private static final String LOGOUT = "9";

    /** Quits whichever prompt is reading it - the gate or the menu. */
    private static final String QUIT = "0";

    // ---------- getting in ----------

    @Test
    @DisplayName("greets the player on startup")
    void printsTheWelcomeMessage() {
        String output = runApp(GUEST, QUIT);

        assertTrue(output.contains("Welcome to Command Quest!"));
    }

    @Test
    @DisplayName("lets a guest through to the menu")
    void guestReachesTheMenu() {
        String output = runApp(GUEST, QUIT);

        assertTrue(output.contains("Continuing as Guest."));
        assertTrue(output.contains("Welcome, Guest!"));
    }

    @Test
    @DisplayName("greets a newly created account by name")
    void greetsANewAccountByName() {
        String output = runApp(NEW_ACCOUNT, "zub", "1234", QUIT);

        assertTrue(output.contains("Account created! You're logged in."));
        assertTrue(output.contains("Welcome, zub!"));
    }

    @Test
    @DisplayName("quits from the gate without ever showing the menu")
    void exitsFromTheGate() {
        String output = runApp(QUIT);

        assertTrue(output.contains("Goodbye!"));
        assertFalse(output.contains("Choose a number to learn how the command works!"),
                "choosing Exit at the gate must skip the menu entirely");
    }

    // ---------- the main menu ----------

    @Test
    @DisplayName("reports an unrecognised menu number")
    void rejectsUnknownMenuOption() {
        String output = runApp(GUEST, "42", QUIT);

        assertTrue(output.contains("Unknown option."));
    }

    @Test
    @DisplayName("reports non-numeric menu input")
    void rejectsNonNumericMenuOption() {
        String output = runApp(GUEST, "banana", QUIT);

        assertTrue(output.contains("Please enter a number."));
    }

    @Test
    @DisplayName("keeps running after a bad menu entry")
    void survivesABadMenuEntry() {
        String output = runApp(GUEST, "banana", QUIT);

        assertTrue(output.contains("Goodbye!"), "a bad entry must not end the game");
    }

    // ---------- each menu option ----------

    @Test
    @DisplayName("creates a file through option 1")
    void optionOneCreatesAFile() {
        String output = runApp(GUEST, MAKE_FILE, "touch mine.txt", QUIT);

        assertTrue(output.contains("File Successfully created!"));
    }

    @Test
    @DisplayName("creates a folder through option 2")
    void optionTwoCreatesAFolder() {
        String output = runApp(GUEST, MAKE_FOLDER, "mkdir projects", QUIT);

        assertTrue(output.contains("Folder Successfully created!"));
    }

    @Test
    @DisplayName("lists the folder through option 3")
    void optionThreeListsTheFolder() {
        String output = runApp(GUEST, VIEW_FOLDER, "ls", QUIT);

        assertTrue(output.contains("- todo.md"), "the game seeds todo.md at root");
        assertTrue(output.contains("- notes.txt"), "and notes.txt");
    }

    @Test
    @DisplayName("moves into a subfolder through option 4")
    void optionFourChangesFolder() {
        String output = runApp(GUEST, MAKE_FOLDER, "mkdir projects",
                CHANGE_FOLDER, "cd projects", QUIT);

        assertTrue(output.contains("Now in: root/projects"));
    }

    // ---------- the pieces working together ----------

    @Test
    @DisplayName("shows a newly created file in the listing")
    void createdItemsAppearInTheListing() {
        String output = runApp(GUEST, MAKE_FILE, "touch mine.txt",
                VIEW_FOLDER, "ls", QUIT);

        assertTrue(output.contains("- mine.txt"));
    }

    /**
     * main() passes the Navigator to every command, so a command run after cd
     * acts on the folder cd moved into rather than on root.
     */
    @Test
    @DisplayName("later commands act on the folder cd moved into")
    void commandsFollowTheNavigator() {
        String output = runApp(GUEST, MAKE_FOLDER, "mkdir projects",
                CHANGE_FOLDER, "cd projects",
                MAKE_FILE, "touch inside.txt",
                VIEW_FOLDER, "ls", QUIT);

        assertTrue(output.contains("- inside.txt"), "the new file is in projects");
        assertFalse(output.contains("- todo.md"), "root's files are not visible from inside projects");
    }

    // ---------- logging out ----------

    @Test
    @DisplayName("returns to the login gate on option 9")
    void logoutReturnsToTheGate() {
        String output = runApp(GUEST, LOGOUT, GUEST, QUIT);

        assertTrue(output.contains("Logging out..."));
        assertTrue(output.contains("Goodbye!"), "the second session can still quit");
    }

    /**
     * The folder tree is built once, outside the session loop, so it survives a
     * logout - but main() calls navigator.toRoot() on each login, so the player
     * always restarts at the top of it.
     */
    @Test
    @DisplayName("keeps the tree but restarts at root after logging out")
    void newSessionStartsAtRoot() {
        String output = runApp(GUEST, MAKE_FOLDER, "mkdir projects",
                CHANGE_FOLDER, "cd projects",
                LOGOUT, GUEST,
                VIEW_FOLDER, "ls", QUIT);

        assertTrue(output.contains("- todo.md"), "back at root, so root's files are listed");
        assertTrue(output.contains("- projects"), "and last session's folder is still there");
    }
}
