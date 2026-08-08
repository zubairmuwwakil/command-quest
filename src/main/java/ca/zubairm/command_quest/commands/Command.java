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
     * This is the method the web layer calls. It is a pure function of the
     * folder and the typed line, which is what lets a browser and a terminal
     * share one set of rules.
     */
    CommandResult execute(Folder folder, String input);
}
