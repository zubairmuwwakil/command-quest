package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;

//interface
public interface Command {

    void run(Folder folder, Scanner scanner);

    /**
     * Handles one typed line and reports what happened, without printing or
     * reading anything.
     *
     * Temporarily a default so run() and execute() can coexist while callers
     * migrate. Once nothing calls run(), run() is deleted and this becomes the
     * only method on the interface.
     */
    default CommandResult execute(Folder folder, String input) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " has not implemented execute() yet");
    }
}
