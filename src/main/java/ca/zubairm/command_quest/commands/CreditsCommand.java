package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.Credits;
import ca.zubairm.command_quest.hub.Folder;

/**
 * The hidden credits screen - the reward for typing a word nothing told you
 * about.
 *
 * It implements Command directly rather than extending AbstractCommand for the
 * same reason ViewCommand does: AbstractCommand's algorithm is "validate a
 * name, then create something with it", and this creates nothing. It ignores
 * the folder entirely, which is why the parameter goes unused - the Command
 * contract hands one to every command, and having nothing to do with it is a
 * perfectly good answer.
 *
 * Being hidden is not this class's job. Shell decides what it advertises; all
 * this decides is what the screen says.
 */
public class CreditsCommand implements Command {

    @Override
    public CommandResult execute(Folder folder, String input) {
        StringBuilder sb = new StringBuilder("Command Quest - built by Zubair Muwwakil.\n")
                .append("\nWith thanks to the crew:");

        Credits.members().forEach(member -> sb.append("\n  * ").append(member));

        sb.append("\n\nYou found the hidden command. Well played.");

        return new CommandResult(CommandResult.Outcome.SUCCEEDED, sb.toString(), null);
    }

    /**
     * The console conversation. There is nothing to retry and nothing to get
     * wrong, so unlike the lessons this prints once and returns without ever
     * touching the scanner.
     */
    @Override
    public void run(Folder folder, Scanner scanner) {
        System.out.println("\n" + execute(folder, "credits").output() + "\n");
    }
}
