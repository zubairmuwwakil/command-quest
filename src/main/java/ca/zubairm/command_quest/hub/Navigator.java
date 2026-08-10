package ca.zubairm.command_quest.hub;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

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

    // Where you are now = top of the stack.
    public Folder current() {
        return path.peek();
    }

    // cd <name>: step INTO a subfolder of the CURRENT folder -> push it.
    // Taking a name rather than a Folder is what keeps the stack honest: the
    // only thing that can be pushed is a genuine child of where you already
    // are, so the stack can never stop being a real root-to-current path.
    // Returns false if the current folder has no such child.
    public boolean into(String name) {
        Folder sub = path.peek().getSubFolder(name);
        if (sub == null) {
            return false;
        }
        path.push(sub);
        return true;
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
