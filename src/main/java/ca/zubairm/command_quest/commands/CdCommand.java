package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Navigator;

/**
 * The cd command - moves BETWEEN folders. It needs the Navigator (to change
 * which folder is current), not just a Folder, so it deliberately does NOT
 * implement Command. Navigation is its own contract.
 *
 * Keeping it outside the interface is what stops touch and mkdir from being
 * handed a Navigator they have no business using: a command that receives a
 * Folder cannot move the player, and the compiler enforces that rather than a
 * comment.
 */
public class CdCommand {

	private static final String LESSON = """

			To move around, use the cd command:
			cd <name>  ->  go into a subfolder
			cd ..      ->  go up one level
			cd /       ->  jump back to root

			Type * to return to the main menu.
			""";

	private static final String HINT = "cd <name>, cd .., or cd /";

	/**
	 * Handles one typed line and reports what happened, without printing or
	 * reading anything.
	 */
	public CommandResult execute(Navigator navigator, String input) {
		String typed = input.trim();

		if (typed.equals("*")) {
			return new CommandResult(
					CommandResult.Outcome.CANCELLED,
					"Returning to the main menu...",
					null);
		}

		String[] tokens = typed.split("\\s+");
		if (tokens.length < 2 || !tokens[0].equals("cd")) {
			return rejected("Not quite - try: cd <name>, cd .., or cd /");
		}

		String target = tokens[1];

		if (target.equals("..")) {
			if (!navigator.up()) {
				return rejected("Already at root.");
			}
			return moved(navigator);
		}

		if (target.equals("/")) {
			navigator.toRoot();
			return moved(navigator);
		}

		if (!navigator.current().hasSubFolder(target)) {
			return rejected("No folder named '" + target + "' here.");
		}

		navigator.into(navigator.current().getSubFolders().get(target));
		return moved(navigator);
	}

	private CommandResult moved(Navigator navigator) {
		return new CommandResult(
				CommandResult.Outcome.SUCCEEDED,
				"Now in: " + navigator.breadcrumb(),
				null);
	}

	private CommandResult rejected(String message) {
		return new CommandResult(CommandResult.Outcome.REJECTED, message, HINT);
	}

	/** The console conversation: keep asking until the player moves or gives up. */
	public void run(Navigator navigator, Scanner scanner) {
		System.out.println(LESSON);
		System.out.print("Enter command: ");

		while (true) {
			CommandResult result = execute(navigator, scanner.nextLine());

			switch (result.outcome()) {
				case CANCELLED -> System.out.println(result.output() + "\n");
				case SUCCEEDED -> System.out.println("\n" + result.output() + "\n");
				case REJECTED -> {
					System.out.println("\n" + result.output());
					System.out.print("Enter command: ");
				}
			}

			if (result.finished()) {
				break;
			}
		}
	}
}
