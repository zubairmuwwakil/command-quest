package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;

public class ViewCommand implements Command {
	@Override
	public void run(Folder folder, Scanner scanner) {

		System.out.println("""
				To view your files and folders, you can use the command 'ls' (short for 'list').
				... Yea no thats it lol
				Note it will only show files or folders in the current folder (otherwise known as a directory).
				To go back to the main menu, type *

				""");
		String input = scanner.nextLine().trim();
		if (input.equals("*")) {
			System.out.println("Returning to the main menu...\n");
			return;
		}
		if (input.equals("ls")) {

			System.out.println("Files in the current folder:");
			for (String fileName : folder.getFiles()) {
				System.out.println("- " + fileName);
			}

			System.out.println("\nSubfolders in the current folder:");
			for (String subFolderName : folder.getSubFolders().keySet()) {
				System.out.println("- " + subFolderName);
			}

		}

		
	}
}