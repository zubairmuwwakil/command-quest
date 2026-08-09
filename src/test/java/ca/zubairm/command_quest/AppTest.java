package ca.zubairm.command_quest;

import static ca.zubairm.command_quest.TestSupport.runApp;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for App.main() - the login gate plus the menu loop that ties
 * the whole game together.
 *
 * runApp(...) feeds the given lines as if the user typed each one and pressed
 * Enter, then returns everything the game printed. Each test just checks the
 * output contains the message we expect.
 *
 * The flow is:
 *   login gate  -> 1 Make Account | 2 Login | 3 Continue as Guest | 0 Exit
 *   main menu   -> 1 file | 2 folder | 3 view | 4 cd | 9 logout | 0 exit
 * Most scripts log in as a guest ("3") and then leave the menu ("0").
 */
@DisplayName("App")
class AppTest {

    // ---------- startup and the login gate ----------

    @Test
    @DisplayName("greets the player on startup")
    void printsTheWelcomeMessage() {
        String output = runApp("3", "0");   // guest in, then exit
        assertTrue(output.contains("Welcome to Command Quest!"));
    }

    @Test
    @DisplayName("lets a guest straight in")
    void guestCanContinue() {
        String output = runApp("3", "0");
        assertTrue(output.contains("Welcome, Guest!"));
    }

    @Test
    @DisplayName("creates a new account from a username and 4-digit PIN")
    void registersANewUser() {
        // 1 = Make Account, then the username, then the PIN, then exit the menu.
        String output = runApp("1", "bob", "1234", "0");
        assertTrue(output.contains("Account created! You're logged in."));
        assertTrue(output.contains("Welcome, bob!"));
    }

    @Test
    @DisplayName("rejects a PIN that is not 4 digits")
    void rejectsABadPin() {
        // "12" is too short - the gate refuses it, then we exit.
        String output = runApp("1", "bob", "12", "0");
        assertTrue(output.contains("PIN must be exactly 4 digits."));
    }

    @Test
    @DisplayName("denies login when the account does not exist")
    void deniesAWrongLogin() {
        // 2 = Login, for a user that was never created -> access denied.
        String output = runApp("2", "ghost", "0000", "0");
        assertTrue(output.contains("Access denied"));
    }

    @Test
    @DisplayName("says goodbye when Exit is chosen at the gate")
    void exitsFromTheGate() {
        String output = runApp("0");
        assertTrue(output.contains("Goodbye!"));
    }

    // ---------- the main menu ----------

    @Test
    @DisplayName("says goodbye and stops on menu option 0")
    void exitsOnZero() {
        String output = runApp("3", "0");
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    @DisplayName("reports an unknown menu number")
    void rejectsUnknownMenuOption() {
        String output = runApp("3", "42", "0");
        assertTrue(output.contains("Unknown option."));
    }

    @Test
    @DisplayName("reports non-numeric menu input instead of crashing")
    void rejectsNonNumericMenuInput() {
        String output = runApp("3", "banana", "0");
        assertTrue(output.contains("Please enter a number."));
    }

    @Test
    @DisplayName("logs out and returns to the login gate")
    void logoutReturnsToTheGate() {
        // guest -> logout (9) -> guest again -> exit
        String output = runApp("3", "9", "3", "0");
        assertTrue(output.contains("Logging out..."));
    }

    // ---------- the features, driven through the menu ----------

    @Test
    @DisplayName("creates a file through menu option 1")
    void makesAFile() {
        // guest -> option 1 -> type the touch command -> exit
        String output = runApp("3", "1", "touch mine.txt", "0");
        assertTrue(output.contains("File Successfully created!"));
    }

    @Test
    @DisplayName("creates a folder through menu option 2")
    void makesAFolder() {
        String output = runApp("3", "2", "mkdir projects", "0");
        assertTrue(output.contains("Folder Successfully created!"));
    }

    @Test
    @DisplayName("lists the current folder through menu option 3")
    void viewsTheFolder() {
        // option 3 -> type "ls" to list -> exit. The game starts with two files.
        String output = runApp("3", "3", "ls", "0");
        assertTrue(output.contains("notes.txt"));
        assertTrue(output.contains("todo.md"));
    }

    @Test
    @DisplayName("changes into a folder through menu option 4")
    void changesFolder() {
        // make a folder, then cd into it - the breadcrumb should update.
        String output = runApp("3", "2", "mkdir projects", "4", "cd projects", "0");
        assertTrue(output.contains("Now in: root/projects"));
    }
}
