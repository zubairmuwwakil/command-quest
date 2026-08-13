package ca.zubairm.command_quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The team, and the one place their names are written down.
 *
 * Four easter eggs read from here - the hidden credits command, the .team
 * folder, the reaction when a player names something after a teammate, and the
 * greeting at the login gate. Holding the names in one class means adding a
 * teammate is one edit rather than four, and it keeps the spelling of a real
 * person's name from drifting between copies.
 *
 * LinkedHashMap rather than Map.of for the same reason Shell uses one: the
 * credits screen prints these in order, and an unordered map would shuffle the
 * team between runs.
 */
public final class Credits {

    private Credits() {
        // utility class - never instantiated
    }

    /**
     * The hidden folder the team live in.
     *
     * The leading dot is the whole trick: it is the Unix convention for
     * "hidden", so ls passes over it and only ls -a turns it up.
     */
    public static final String FOLDER = ".team";

    /** Full names, in the order the credits screen reads them out. */
    private static final List<String> MEMBERS = List.of(
            "Victoria Oyedotun",
            "Seun Edagbami-olota",
            "Armando Bazeydio");

    /**
     * First name (lowercase) to the line that fires when a player types it.
     *
     * Keyed on first names only, because mkdir validates against \w+ and a
     * surname like "Edagbami-olota" contains a hyphen - the command would
     * reject the line for bad format before it ever reached a lookup here.
     */
    private static final Map<String, String> SHOUTOUTS = new LinkedHashMap<>();

    static {
        SHOUTOUTS.put("victoria", "Nice - Victoria Oyedotun would've named it the same way.");
        SHOUTOUTS.put("seun", "Seun Edagbami-olota says: ship it.");
        SHOUTOUTS.put("armando", "Armando Bazeydio has entered the chat.");
    }

    public static List<String> members() {
        return MEMBERS;
    }

    /**
     * One file name per teammate, for seeding the hidden folder.
     *
     * Derived from the shoutout keys rather than written out again, so the file
     * a player finds in .team is guaranteed to be a name this class will react
     * to when they type it themselves.
     */
    public static List<String> fileNames() {
        return SHOUTOUTS.keySet().stream().map(first -> first + ".md").toList();
    }

    /**
     * The line for this name, or null when the name belongs to nobody.
     *
     * Null rather than an empty string is deliberate: every caller has to
     * decide whether to print anything at all, and null makes that decision
     * impossible to skip by accident.
     *
     * @param name a folder name or a file name, as the player typed it
     */
    public static String shoutoutFor(String name) {
        if (name == null) {
            return null;
        }

        // "victoria.md" -> "victoria". touch always carries an extension, so
        // without this the file eggs would never match.
        String bare = name.trim().toLowerCase();
        int dot = bare.indexOf('.');
        if (dot >= 0) {
            bare = bare.substring(0, dot);
        }

        return SHOUTOUTS.get(bare);
    }
}
