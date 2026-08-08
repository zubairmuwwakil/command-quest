package ca.zubairm.command_quest.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Navigator's own tests, including the path handling the web layer depends on.
 *
 * The stateless API sends location as a list of folder NAMES rather than the
 * Navigator itself. That is not a stylistic choice: serialising a Navigator
 * produces "{}" - it has no getX() methods, so Jackson finds no properties and
 * silently drops the whole navigation state without raising anything.
 */
@DisplayName("Navigator")
class NavigatorTest {

    private static Folder tree() {
        Folder root = new Folder("root");
        root.addSubFolder("photos");
        root.getSubFolders().get("photos").addSubFolder("2024");
        return root;
    }

    @Test
    @DisplayName("starts at root")
    void startsAtRoot() {
        Navigator navigator = new Navigator(tree());

        assertEquals("root", navigator.current().getName());
        assertEquals("root", navigator.breadcrumb());
    }

    @Test
    @DisplayName("breadcrumb reads root first, not current first")
    void breadcrumbReadsRootFirst() {
        Folder root = tree();
        Navigator navigator = new Navigator(root);
        navigator.into(root.getSubFolders().get("photos"));

        assertEquals("root/photos", navigator.breadcrumb());
    }

    @Test
    @DisplayName("cannot pop past root")
    void cannotGoAboveRoot() {
        Navigator navigator = new Navigator(tree());

        assertFalse(navigator.up(), "up() reports it did not move");
        assertEquals("root", navigator.current().getName());
    }

    @Test
    @DisplayName("toRoot unwinds any depth")
    void toRootUnwindsEverything() {
        Folder root = tree();
        Navigator navigator = new Navigator(root);
        Folder photos = root.getSubFolders().get("photos");
        navigator.into(photos);
        navigator.into(photos.getSubFolders().get("2024"));

        navigator.toRoot();

        assertEquals("root", navigator.current().getName());
    }

    // ---------- rebuilding a Navigator from a wire path ----------

    @Test
    @DisplayName("at() walks a path of names down from root")
    void rebuildsFromAPath() {
        Navigator navigator = Navigator.at(tree(), List.of("photos", "2024"));

        assertEquals("2024", navigator.current().getName());
        assertEquals("root/photos/2024", navigator.breadcrumb());
    }

    @Test
    @DisplayName("at() with an empty path stays at root")
    void emptyPathIsRoot() {
        assertEquals("root", Navigator.at(tree(), List.of()).current().getName());
    }

    @Test
    @DisplayName("at() rebuilds a real stack, so up() still works afterwards")
    void rebuiltNavigatorCanStillGoUp() {
        Navigator navigator = Navigator.at(tree(), List.of("photos", "2024"));

        assertTrue(navigator.up());
        assertEquals("photos", navigator.current().getName());
        assertTrue(navigator.up());
        assertEquals("root", navigator.current().getName());
    }

    @Test
    @DisplayName("at() refuses a path that does not exist rather than landing somewhere wrong")
    void rejectsAnUnknownPath() {
        assertThrows(IllegalArgumentException.class,
                () -> Navigator.at(tree(), List.of("photos", "nope")));
    }
}
