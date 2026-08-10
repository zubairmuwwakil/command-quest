package ca.zubairm.command_quest.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for UserManager - the in-memory account store (a HashMap of
 * username -> User). It can add a user, check whether a username is taken, and
 * log a user in only when the username exists AND the PIN matches.
 */
@DisplayName("UserManager")
class UserManagerTest {

    @Test
    @DisplayName("a brand new username is not taken")
    void unknownUsernameIsFree() {
        UserManager users = new UserManager();

        assertFalse(users.usernameExists("bob"));
    }

    @Test
    @DisplayName("remembers a user after they are added")
    void remembersAnAddedUser() {
        UserManager users = new UserManager();
        users.addUser("bob", "1234");

        assertTrue(users.usernameExists("bob"));
    }

    @Test
    @DisplayName("logs in with the correct username and PIN")
    void logsInWithCorrectDetails() {
        UserManager users = new UserManager();
        users.addUser("bob", "1234");

        User user = users.login("bob", "1234");

        assertNotNull(user);
        assertEquals("bob", user.getUsername());
    }

    @Test
    @DisplayName("refuses login with the wrong PIN")
    void refusesWrongPin() {
        UserManager users = new UserManager();
        users.addUser("bob", "1234");

        // wrong PIN -> login returns null
        assertNull(users.login("bob", "0000"));
    }

    @Test
    @DisplayName("refuses login for a username that does not exist")
    void refusesUnknownUser() {
        UserManager users = new UserManager();

        assertNull(users.login("ghost", "1234"));
    }
}
