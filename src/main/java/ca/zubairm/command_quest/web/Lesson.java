package ca.zubairm.command_quest.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The teaching text for one lesson.
 *
 * This is served from its own endpoint rather than returned with each command
 * result. AbstractCommand prints its lesson once per run() and then loops on
 * input, but execute() is called once per submitted line - so returning the
 * lesson with every result would reprint the whole thing after every attempt.
 *
 * @param title   the menu label, e.g. "Make a file"
 * @param body    the explanation shown in the lesson panel
 * @param example a worked example the player can copy
 */
public record Lesson(String title, String body, String example) {

    /**
     * The catalogue, keyed by lessonId.
     *
     * The text is duplicated from the command classes rather than read out of
     * them, deliberately: exposing AbstractCommand's private fields through
     * accessors purely to feed a web page would push presentation concerns
     * back into the domain layer this refactor just cleaned out.
     */
    public static Map<String, Lesson> catalogue() {
        Map<String, Lesson> lessons = new LinkedHashMap<>();

        lessons.put("touch", new Lesson(
                "Make a file",
                """
                To make a file, type the touch command, followed by a filename and \
                extension. A file extension tells the computer what type of file it \
                is, like .txt for text notes or .md for markdown.""",
                "touch chicken.leg"));

        lessons.put("mkdir", new Lesson(
                "Make a folder",
                """
                To make a folder, type the mkdir command — short for "make directory" \
                (in computing, "directory" is just another word for folder) — followed \
                by the folder name. Folders do not need file extensions.""",
                "mkdir homework"));

        lessons.put("ls", new Lesson(
                "View this folder",
                """
                Type ls — short for "list" — to see all the files and folders inside \
                the folder you are currently in. It lists only this folder's contents.""",
                "ls"));

        lessons.put("cd", new Lesson(
                "Change folder",
                """
                Type cd — short for "change directory" — to move between folders. \
                Use cd <name> to enter a folder, cd .. to go back up one folder, \
                and cd / to jump all the way back to the root folder.""",
                "cd photos"));

        lessons.put("pwd", new Lesson(
                "Find where you are",
                """
                Type pwd — short for "print working directory" — to see your exact \
                location path starting from root down to your current folder. \
                It takes no extra name and helps you get your bearings back anytime.""",
                "pwd"));

        return lessons;
    }
}
