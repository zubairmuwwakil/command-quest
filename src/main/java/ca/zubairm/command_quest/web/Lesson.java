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
                To make a file, type the touch command, then the name you want \
                the file to have, then the file extension.""",
                "touch chicken.leg"));

        lessons.put("mkdir", new Lesson(
                "Make a folder",
                """
                To make a folder, type the mkdir command - short for "make \
                directory" - then the name you want the folder to have. \
                Folders take no extension.""",
                "mkdir homework"));

        lessons.put("ls", new Lesson(
                "View this folder",
                """
                Type ls - short for "list" - to see what is in the folder you \
                are standing in. It shows only this folder, not the ones inside it.""",
                "ls"));

        lessons.put("cd", new Lesson(
                "Change folder",
                """
                Type cd - short for "change directory" - then where you want to \
                go. Use cd <name> to go into a folder, cd .. to go up one level, \
                and cd / to jump back to root.""",
                "cd photos"));

        return lessons;
    }
}
