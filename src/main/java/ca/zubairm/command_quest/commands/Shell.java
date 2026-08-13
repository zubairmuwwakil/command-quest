package ca.zubairm.command_quest.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import ca.zubairm.command_quest.hub.Navigator;

/**
 * Decides which command a typed line reaches, and answers when it reaches none.
 *
 * The web layer used to do this by looking up the lesson the player had
 * selected, which meant the lesson tabs quietly decided what the player was
 * allowed to type: a correct "mkdir homework" typed during the touch lesson was
 * handed to TouchCommand and came back as a format error. Dispatch here is on
 * the first word of the input and nothing else, so there is no parameter a
 * caller could use to narrow it. The lessons became guidance by losing the
 * ability to be anything else.
 *
 * This is deliberately not in the web layer. Routing a line to a command is a
 * boundary concern, but deciding what to tell a beginner who typed "mkdr" is
 * not - it is the game teaching, and it belongs where it can be tested without
 * booting Spring.
 */
public class Shell {

    /**
     * What ran, and what happened.
     *
     * @param commandId the keyword that ran, or null when the input matched
     *                  nothing. The browser uses it to credit progress and to
     *                  move the lesson panel to whatever the player is doing.
     * @param result    the command's own account of the attempt
     */
    public record Result(String commandId, CommandResult result) {}

    private static final String HELP = "help";

    /** One row of the vocabulary: how to type it, what it does, and what runs. */
    private record Entry(String usage, String summary,
                         BiFunction<Navigator, String, CommandResult> action) {}

    private final TouchCommand touch = new TouchCommand();
    private final MkDirCommand mkdir = new MkDirCommand();
    private final ViewCommand ls = new ViewCommand();
    private final CdCommand cd = new CdCommand();
    private final PwdCommand pwd = new PwdCommand();
    private final CreditsCommand credits = new CreditsCommand();

    /**
     * LinkedHashMap, not Map.of, and the difference is user-visible.
     *
     * The old controller used Map.of, which guarantees no order. That was
     * harmless while nothing printed the keys. This registry is now read aloud -
     * in help and in every unknown-command reply - so an unordered map would
     * shuffle the vocabulary between JVM runs. The order here matches
     * Lesson.catalogue(), so the list a player reads in the terminal is the
     * order of the tabs above it.
     */
    private final Map<String, Entry> registry = new LinkedHashMap<>();

    /**
     * Commands that work but are never spoken of.
     *
     * A second map rather than a flag on Entry, because every place that reads
     * the vocabulary aloud - help, the unknown-command reply, the typo
     * suggester, and keywords() for the browser's lesson tabs - walks the
     * registry. Registering a hidden command there and then filtering it out of
     * four separate readers is four chances to forget one. Keeping it outside
     * the registry means dispatch is the only thing that can see it, and the
     * secret holds by construction instead of by vigilance.
     */
    private final Map<String, Entry> hidden = new LinkedHashMap<>();

    public Shell() {
        // touch, mkdir and ls act on the folder in front of you; cd and pwd need
        // the Navigator, because one changes where you are and the other reads
        // it. Adapting both shapes to one signature is this class's job.
        registry.put("touch", new Entry("touch <name.ext>", "make a file",
                (nav, input) -> touch.execute(nav.current(), input)));
        registry.put("mkdir", new Entry("mkdir <name>", "make a folder",
                (nav, input) -> mkdir.execute(nav.current(), input)));
        registry.put("ls", new Entry("ls", "list what is in this folder",
                (nav, input) -> ls.execute(nav.current(), input)));
        registry.put("cd", new Entry("cd <name>", "change folder - also cd .. and cd /",
                (nav, input) -> cd.execute(nav, input)));
        registry.put("pwd", new Entry("pwd", "show where you are",
                (nav, input) -> pwd.execute(nav, input)));

        // Not in the registry, so nothing lists it. Found by typing it.
        hidden.put("credits", new Entry("credits", "the people behind the game",
                (nav, input) -> credits.execute(nav.current(), input)));
    }

    public Result execute(Navigator navigator, String input) {
        String typed = input == null ? "" : input.trim();

        if (typed.isEmpty()) {
            return rejected("Type a command, or help to see what I know.", HELP);
        }

        String keyword = typed.split("\\s+")[0];

        if (keyword.equals(HELP)) {
            return new Result(HELP,
                    new CommandResult(CommandResult.Outcome.SUCCEEDED, helpText(), null));
        }

        Entry entry = registry.get(keyword);
        if (entry == null) {
            entry = hidden.get(keyword);
        }
        if (entry == null) {
            return unrecognised(keyword);
        }

        // "*" never gets here - it is not a keyword - so the console's
        // "Returning to the main menu..." can no longer surface in a browser
        // that has no main menu, without the console losing it.
        return new Result(keyword, entry.action().apply(navigator, typed));
    }

    /** The lesson commands, in tab order. Excludes help, which teaches nothing. */
    public List<String> keywords() {
        return List.copyOf(registry.keySet());
    }

    // ---------------------------------------------------------------- unknown

    private Result unrecognised(String keyword) {
        String suggestion = closestTo(keyword);

        if (suggestion != null) {
            // No hint: the message already carries the correction, and
            // "Try: help" underneath a specific suggestion just adds noise.
            return rejected(
                    "I don't know \"" + keyword + "\". Did you mean \"" + suggestion + "\"?",
                    null);
        }

        return rejected(
                "I don't know \"" + keyword + "\" yet. Try: " + vocabularyList() + ".",
                HELP);
    }

    private Result rejected(String message, String hint) {
        return new Result(null, new CommandResult(CommandResult.Outcome.REJECTED, message, hint));
    }

    /** Everything a player may type, help included. */
    private List<String> vocabulary() {
        List<String> all = new ArrayList<>(registry.keySet());
        all.add(HELP);
        return all;
    }

    /** "touch, mkdir, ls, cd, pwd, or help" */
    private String vocabularyList() {
        List<String> all = vocabulary();
        String last = all.remove(all.size() - 1);
        return String.join(", ", all) + ", or " + last;
    }

    private String helpText() {
        StringBuilder sb = new StringBuilder("Commands I know:");
        registry.values().forEach(entry -> sb
                .append("\n  ")
                .append(String.format("%-18s", entry.usage()))
                .append(entry.summary()));
        sb.append("\n\nType any of them whenever you like. "
                + "The lessons are there to help, not to hold you back.");
        return sb.toString();
    }

    // ---------------------------------------------------------------- guessing

    /**
     * The closest command to what was typed, or null if nothing is close enough.
     *
     * Compared without case, so a player with caps lock is told they meant
     * "touch" rather than that TOUCH is a mystery. Dispatch itself stays
     * case-sensitive, like a real shell - the suggestion is what teaches that.
     */
    private String closestTo(String typed) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String candidate : vocabulary()) {
            int distance = distance(typed.toLowerCase(), candidate);

            // Short keywords need a tighter threshold or the guessing turns
            // wild: "rm" is two edits from both "ls" and "cd", and confidently
            // suggesting either would be worse than admitting ignorance.
            int threshold = candidate.length() <= 3 ? 1 : 2;

            if (distance <= threshold && distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }

        return best;
    }

    /**
     * Damerau-Levenshtein distance: insertions, deletions and substitutions
     * cost 1 each, and so does transposing two adjacent characters.
     *
     * That last case is the reason this is not plain Levenshtein. Swapping two
     * letters is among the commonest typos, and plain Levenshtein charges 2 for
     * it - enough to push "sl" out of range of "ls" and leave the player with
     * no suggestion at all.
     */
    private static int distance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

                d[i][j] = Math.min(
                        Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                        d[i - 1][j - 1] + cost);

                if (i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }

        return d[a.length()][b.length()];
    }
}
