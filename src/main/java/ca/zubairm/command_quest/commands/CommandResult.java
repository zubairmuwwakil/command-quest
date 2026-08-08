package ca.zubairm.command_quest.commands;

/**
 * What happened when a player typed one line at a command.
 *
 * run() answered this question by printing. Returning it instead is what lets
 * the same command serve a console and a browser: the caller decides how to
 * display it, and the command stops caring.
 *
 * @param outcome what the command did with the input
 * @param output  the message to show the player
 * @param hint    a worked example to try next, or null when there is nothing
 *                to retry
 */
public record CommandResult(Outcome outcome, String output, String hint) {

    /**
     * Three things can happen, and a boolean cannot express three things.
     *
     * The console loop needs REJECTED separated from the other two so it knows
     * whether to keep prompting; the browser needs CREATED separated from the
     * other two so it knows whether to advance the lesson.
     */
    public enum Outcome {
        /** The command was valid and the folder changed. */
        CREATED,
        /** The command was understood but not accepted; the player should retry. */
        REJECTED,
        /** The player typed "*" to give up on this lesson. */
        CANCELLED
    }

    /** True when the folder actually changed - the signal that a lesson is passed. */
    public boolean created() {
        return outcome == Outcome.CREATED;
    }

    /** True when there is no point prompting again, whether by success or by giving up. */
    public boolean finished() {
        return outcome != Outcome.REJECTED;
    }
}
