package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Navigator;

/**
 * The cd command - moves BETWEEN folders rather than creating things inside
 * one. It takes the Navigator like every Command does, but where touch and
 * mkdir call current() to reach a folder, cd calls up(), into(), and toRoot()
 * to change which folder current() will return.
 */
public class CdCommand implements Command {

	@Override
	public void run(Navigator navigator, Scanner scanner) {
		System.out.println("""

				To move around, use the cd command:
				cd <name>  ->  go into a subfolder
				cd ..      ->  go up one level
				cd /       ->  jump back to root

				Type * to return to the main menu.
				""");
		System.out.print("Enter command: ");

		while (true) {
			String cmd = scanner.nextLine().trim();
			if (cmd.equals("*")) {
				System.out.println("Returning to the main menu...\n");
				break;
			}

			String[] tokens = cmd.split("\\s+");
			if (tokens.length < 2 || !tokens[0].equals("cd")) {
				System.out.println("\nNot quite - try: cd <name>, cd .., or cd /");
				System.out.print("Enter command: ");
				continue;
			}

			String target = tokens[1];
			boolean moved = false;

			if (target.equals("..")) {
				if (navigator.up()) moved = true;
				else System.out.println("\nAlready at root.");
			} else if (target.equals("/")) {
				navigator.toRoot();
				moved = true;
			} else if (navigator.current().hasSubFolder(target)) {
				navigator.into(navigator.current().getSubFolders().get(target));
				moved = true;
			} else {
				System.out.println("\nNo folder named '" + target + "' here.");
			}

			if (moved) {
				System.out.println("\nNow in: " + navigator.breadcrumb() + "\n");
				break;
			}
			System.out.print("Enter command: ");
		}
	}
}
