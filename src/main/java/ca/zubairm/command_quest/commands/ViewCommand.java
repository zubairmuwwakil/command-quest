package ca.zubairm.command_quest.commands;

import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;

/**
 * The ls lesson - the only command that reports on a folder without changing
 * it. "Succeeded" here means the player typed ls correctly, not that anything
 * was created.
 *
 * ViewCommand implements Command directly rather than extending
 * AbstractCommand, because AbstractCommand's algorithm is "validate a name,
 * then create something with it" and ls takes no name and creates nothing.
 */
public class ViewCommand implements Command {

	private static final String LESSON = """
			To view your files and folders, you can use the command 'ls' (short for 'list').
			... Yea no thats it lol
			Note it will only show files or folders in the current folder (otherwise known as a directory).
			To go back to the main menu, type *

			""";

	@Override
	public CommandResult execute(Folder folder, String input) {
		String typed = input.trim();

		if (typed.equals("*")) {
			return new CommandResult(
					CommandResult.Outcome.CANCELLED,
					"Returning to the main menu...",
					null);
		}

		String[] tokens = typed.split("\\s+");
		boolean showAll = tokens.length == 2 && tokens[0].equals("ls") && tokens[1].equals("-a");

		if (!showAll && !typed.equals("ls")) {
			// Previously this fell through and printed nothing at all, which
			// left the player staring at a prompt with no idea they were wrong.
			//
			// The message still names 'ls' alone, deliberately. A real shell
			// does not advertise -a either, and a player who is told about it
			// here has been handed the secret rather than left to find it.
			return new CommandResult(
					CommandResult.Outcome.REJECTED,
					"Not quite - the command to list a folder is just 'ls'.",
					"ls");
		}

		return new CommandResult(CommandResult.Outcome.SUCCEEDED, listing(folder, showAll), null);
	}

	/** Renders the folder the way ls would: folders first, then files. */
	private String listing(Folder folder, boolean showAll) {
		List<String> files = visible(folder.getFiles(), showAll);
		List<String> subFolders = visible(folder.getSubFolders().keySet(), showAll);

		// Judged on what is actually going to be printed, not on what the
		// folder holds. A folder containing only hidden entries reads as empty,
		// which is both true from where the player is standing and better than
		// two headings with nothing underneath them.
		if (files.isEmpty() && subFolders.isEmpty()) {
			return "This folder is empty.";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("Files in the current folder:");
		if (files.isEmpty()) {
			sb.append("\n(none)");
		} else {
			files.forEach(name -> sb.append("\n- ").append(name));
		}

		sb.append("\n\nSubfolders in the current folder:");
		if (subFolders.isEmpty()) {
			sb.append("\n(none)");
		} else {
			subFolders.forEach(name -> sb.append("\n- ").append(name));
		}

		return sb.toString();
	}

	/**
	 * The entries ls will admit to, sorted.
	 *
	 * A leading dot means hidden, exactly as it does in a real shell - so the
	 * convention the game teaches is the one the player will meet outside it.
	 */
	private static List<String> visible(Collection<String> names, boolean showAll) {
		return names.stream()
				.filter(name -> showAll || !name.startsWith("."))
				.sorted()
				.toList();
	}

	/**
	 * The console conversation. Unlike AbstractCommand this asks only once,
	 * preserving the original behaviour of the ls lesson.
	 */
	@Override
	public void run(Folder folder, Scanner scanner) {
		System.out.println(LESSON);

		CommandResult result = execute(folder, scanner.nextLine());

		if (result.outcome() == CommandResult.Outcome.CANCELLED) {
			System.out.println(result.output() + "\n");
			return;
		}

		System.out.println(result.output());
		if (result.hint() != null) {
			System.out.println("Try typing: " + result.hint() + "\n");
		}
	}
}
