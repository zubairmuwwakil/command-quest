package ca.zubairm.command_quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for Credits - the one place the team's names live.
 *
 * Four separate easter eggs read from this class, so the names are written
 * once here rather than copied into Shell, ViewCommand, AbstractCommand and
 * App. These tests pin down the matching rules those four rely on.
 */
@DisplayName("Credits")
class CreditsTest {

    @Test
    @DisplayName("knows a teammate by their first name")
    void findsATeammate() {
        String shoutout = Credits.shoutoutFor("victoria");

        assertNotNull(shoutout, "victoria is on the team and should have a line");
        assertTrue(shoutout.contains("Victoria Oyedotun"),
                "the line should name her in full; got: " + shoutout);
    }

    /**
     * The trigger has to survive being typed as a filename. "touch seun.md"
     * hands this method "seun.md", not "seun", so the extension comes off
     * before the lookup or the file eggs never fire.
     */
    @Test
    @DisplayName("matches through a file extension")
    void ignoresAFileExtension() {
        assertNotNull(Credits.shoutoutFor("seun.md"));
        assertNotNull(Credits.shoutoutFor("armando.txt"));
    }

    /** mkdir accepts any capitalisation, so the lookup cannot be case-sensitive. */
    @Test
    @DisplayName("matches whatever the capitalisation")
    void ignoresCapitalisation() {
        assertEquals(Credits.shoutoutFor("armando"), Credits.shoutoutFor("ARMANDO"));
        assertNotNull(Credits.shoutoutFor("Victoria"));
    }

    @Test
    @DisplayName("has nothing to say about an ordinary name")
    void staysQuietForEveryoneElse() {
        assertNull(Credits.shoutoutFor("todo.md"));
        assertNull(Credits.shoutoutFor("homework"));
        assertNull(Credits.shoutoutFor(""));
        assertNull(Credits.shoutoutFor(null));
    }

    @Test
    @DisplayName("lists the whole team for the credits screen")
    void listsEveryone() {
        List<String> members = Credits.members();

        assertEquals(
                List.of("Victoria Oyedotun", "Seun Edagbami-olota", "Armando Bazeydio"),
                members);
    }

    /**
     * The .team folder is seeded from this rather than from a second list, so
     * a name cannot be spelled one way in the credits screen and another way on
     * the file inside the folder.
     */
    @Test
    @DisplayName("names a file for each teammate")
    void namesAFilePerTeammate() {
        assertEquals(
                List.of("victoria.md", "seun.md", "armando.md"),
                Credits.fileNames());
    }

    @Test
    @DisplayName("every seeded file name is itself a trigger")
    void seededFilesAreTriggers() {
        for (String fileName : Credits.fileNames()) {
            assertNotNull(Credits.shoutoutFor(fileName),
                    fileName + " sits in .team, so reading its name should fire the egg");
        }
    }
}
