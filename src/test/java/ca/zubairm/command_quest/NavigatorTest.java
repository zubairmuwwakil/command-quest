package ca.zubairm.command_quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for Navigator - the class that answers "where am I?".
 *
 * Folder owns the SHAPE of the tree; Navigator owns a POSITION in it. It holds
 * an ArrayDeque used as a stack whose bottom is always root and whose top is
 * always the current folder, so the stack IS the path from root to here. Every
 * test below leans on that one invariant.
 *
 * Navigator needs no Scanner and prints nothing, which is why these tests are
 * plain method calls with no output capture.
 */
@DisplayName("Navigator")
class NavigatorTest {

    /** root/photos/2024 plus root/notes, so there is somewhere to get lost. */
    private static Folder sampleTree() {
        Folder root = new Folder("root");
        root.addSubFolder("photos");
        root.addSubFolder("notes");
        root.getSubFolder("photos").addSubFolder("2024");
        return root;
    }

    // ---------- starting position ----------

    @Test
    @DisplayName("starts at the root it was given")
    void startsAtRoot() {
        Folder root = sampleTree();

        assertSame(root, new Navigator(root).current());
    }

    @Test
    @DisplayName("a fresh navigator's breadcrumb is just the root name")
    void freshBreadcrumbIsRoot() {
        assertEquals("root", new Navigator(sampleTree()).breadcrumb());
    }

    // ---------- into() ----------

    @Test
    @DisplayName("steps into a child and reports success")
    void intoEntersAChild() {
        Folder root = sampleTree();
        Navigator nav = new Navigator(root);

        assertTrue(nav.into("photos"));
        assertSame(root.getSubFolder("photos"), nav.current());
    }

    @Test
    @DisplayName("refuses an unknown name and does not move")
    void intoRejectsAnUnknownName() {
        Folder root = sampleTree();
        Navigator nav = new Navigator(root);

        assertFalse(nav.into("nope"));
        assertSame(root, nav.current(), "a failed cd must leave you where you were");
    }

    /**
     * The invariant that matters: into() takes a NAME, so the only folder that
     * can ever be pushed is a child of where you already are. "2024" exists in
     * the tree, but not here, so it must be refused.
     */
    @Test
    @DisplayName("refuses a folder that exists elsewhere but is not a child")
    void intoRejectsANonChild() {
        Folder root = sampleTree();
        Navigator nav = new Navigator(root);

        assertFalse(nav.into("2024"), "2024 lives under photos, not under root");
        assertSame(root, nav.current());
    }

    @Test
    @DisplayName("stacks up as you descend, and the breadcrumb follows")
    void breadcrumbGrowsWithDepth() {
        Navigator nav = new Navigator(sampleTree());

        nav.into("photos");
        assertEquals("root/photos", nav.breadcrumb());

        nav.into("2024");
        assertEquals("root/photos/2024", nav.breadcrumb());
    }

    // ---------- up() ----------

    @Test
    @DisplayName("steps back up one level")
    void upReturnsToTheParent() {
        Navigator nav = new Navigator(sampleTree());
        nav.into("photos");
        nav.into("2024");

        assertTrue(nav.up());
        assertEquals("root/photos", nav.breadcrumb());
    }

    @Test
    @DisplayName("refuses to go above root and stays put")
    void upStopsAtRoot() {
        Folder root = sampleTree();
        Navigator nav = new Navigator(root);

        assertFalse(nav.up(), "there is nothing above root");
        assertSame(root, nav.current());
        assertEquals("root", nav.breadcrumb());
    }

    @Test
    @DisplayName("cannot be popped past root however hard you try")
    void repeatedUpNeverEmptiesTheStack() {
        Navigator nav = new Navigator(sampleTree());
        nav.into("photos");

        assertTrue(nav.up());
        assertFalse(nav.up());
        assertFalse(nav.up());
        assertEquals("root", nav.breadcrumb(), "current() must never be null");
    }

    // ---------- toRoot() ----------

    @Test
    @DisplayName("jumps home from any depth")
    void toRootReturnsAllTheWayBack() {
        Folder root = sampleTree();
        Navigator nav = new Navigator(root);
        nav.into("photos");
        nav.into("2024");

        nav.toRoot();

        assertSame(root, nav.current());
        assertEquals("root", nav.breadcrumb());
    }

    @Test
    @DisplayName("is harmless when already at root")
    void toRootAtRootChangesNothing() {
        Folder root = sampleTree();
        Navigator nav = new Navigator(root);

        nav.toRoot();

        assertSame(root, nav.current());
    }

    @Test
    @DisplayName("leaves you able to descend again afterwards")
    void toRootDoesNotBreakTheStack() {
        Navigator nav = new Navigator(sampleTree());
        nav.into("photos");
        nav.toRoot();

        assertTrue(nav.into("notes"));
        assertEquals("root/notes", nav.breadcrumb());
    }

    // ---------- the folders are shared, not copied ----------

    /**
     * Navigator never clones a Folder - it only remembers which ones you walked
     * through. That is why a file created via navigator.current() is visible on
     * the original tree, and so why the whole game works.
     */
    @Test
    @DisplayName("hands back the real folder, not a copy")
    void currentIsTheSameObjectAsInTheTree() {
        Folder root = sampleTree();
        Navigator nav = new Navigator(root);
        nav.into("photos");

        nav.current().addFile("holiday.jpg");

        assertTrue(root.getSubFolder("photos").hasFile("holiday.jpg"));
    }
}
