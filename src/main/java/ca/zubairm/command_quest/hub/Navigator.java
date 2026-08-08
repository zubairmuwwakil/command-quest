package ca.zubairm.command_quest.hub;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks WHERE you are in the folder tree using a navigation back-stack -
 * the same pattern browsers and apps use for "back".
 *
 * The stack IS the path from root to your current location:
 *   bottom -> top  =  [ root, photos, 2024 ]
 *   current folder =  the TOP of the stack
 *
 * ArrayDeque used as a stack: push() / pop() / peek() all work on the HEAD,
 * so the head is the "top" (your current folder) and the tail is root.
 */
public class Navigator {

    private Deque<Folder> path = new ArrayDeque<>();

    public Navigator(Folder root) {
        path.push(root);   // root is the bottom of the stack, and starts as current
    }

    /**
     * Rebuilds a Navigator standing at the given path, walking down from root.
     *
     * The web API is stateless: the browser sends the folder tree and a list of
     * folder NAMES like ["photos", "2024"], and the server reconstructs the
     * stack. Names rather than folders, for two reasons.
     *
     * A Navigator cannot be sent directly - Jackson discovers properties by the
     * getX() convention, and none of the methods here match, so it serialises
     * to "{}" and the location is silently lost with no error.
     *
     * And the stack holds references to nodes that also live inside the root
     * tree. Serialising them by value would duplicate whole subtrees and
     * deserialise into copies, so cd would move around a detached clone while
     * touch modified the real tree.
     *
     * @throws IllegalArgumentException if any segment does not exist, rather
     *         than quietly leaving the player somewhere they did not ask for
     */
    public static Navigator at(Folder root, List<String> pathNames) {
        Navigator navigator = new Navigator(root);

        for (String name : pathNames) {
            Folder child = navigator.current().getSubFolders().get(name);
            if (child == null) {
                throw new IllegalArgumentException(
                        "No folder named '" + name + "' under " + navigator.breadcrumb());
            }
            navigator.into(child);
        }

        return navigator;
    }

    /**
     * The current location as a list of names below root - the inverse of at().
     * Root itself is not included, so standing at root gives an empty list.
     */
    public List<String> pathNames() {
        List<String> names = new ArrayList<>();
        Iterator<Folder> it = path.descendingIterator();
        it.next();   // skip root; the client only needs what is below it
        while (it.hasNext()) {
            names.add(it.next().getName());
        }
        return names;
    }

    // Where you are now = top of the stack.
    public Folder current() {
        return path.peek();
    }

    // cd <name>: step INTO a subfolder -> push it, it becomes current.
    public void into(Folder sub) {
        path.push(sub);
    }

    // cd .. : step UP one level -> pop. Guard so we never pop past root.
    // Returns false if already at root (nothing to pop).
    public boolean up() {
        if (path.size() > 1) {
            path.pop();
            return true;
        }
        return false;
    }

    // cd / : jump all the way back to root -> pop until only root remains.
    public void toRoot() {
        while (path.size() > 1) {
            path.pop();
        }
    }

    // The breadcrumb, e.g. "root/photos/2024".
    // The stack iterates top->bottom (current->root), but we want root->current,
    // so we walk it with descendingIterator() (tail->head = root->current).
    public String breadcrumb() {
        StringBuilder sb = new StringBuilder();
        Iterator<Folder> it = path.descendingIterator();
        while (it.hasNext()) {
            sb.append(it.next().getName());
            if (it.hasNext()) {
                sb.append("/");
            }
        }
        return sb.toString();
    }
}
