package ca.zubairm.command_quest;

import java.util.Scanner;

import ca.zubairm.command_quest.commands.Command;
import ca.zubairm.command_quest.commands.MkDirCommand;
import ca.zubairm.command_quest.commands.TouchCommand;
import ca.zubairm.command_quest.commands.ViewCommand;
import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.user.UserManager;
import ca.zubairm.command_quest.user.LoginScreen;
import ca.zubairm.command_quest.user.User;
import ca.zubairm.command_quest.hub.Navigator;

public class App {
	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);

		boolean running = true;

		Folder currentFolder = new Folder("root");
		
		Navigator navigator = new Navigator(currentFolder);

		// Polymorphism 
		
		Command makeFile = new TouchCommand();
		Command makeFolder = new MkDirCommand();
		Command viewFolder = new ViewCommand();

		// work on more concrete examples of polymorphism in code
		// check day 6 in teacher led
		// -> command is an interface said variable is set = to
		// a sub class

		currentFolder.addFile("todo.md");
		currentFolder.addFile("notes.txt");

		System.out.println("""
				Welcome to Command Quest!
				This game is designed to teach you command line prompts
				But for now lets get started!
				""");

	
		// login gate - SRP: the gate logic now lives in LoginScreen
				
		UserManager userManager = new UserManager();
		User user = new LoginScreen(userManager).show(scanner);
		if (user == null) {
			System.out.println("Goodbye!");
			return;
		}
		System.out.println("Welcome, " + user.getUsername() + "!\n");

		while (running) {

			System.out.println("""
					Choose a number to learn how the command works!

					      1) Make a file
					      2) Make a folder
					      3) View current folder
					      4) Change folder
					      9) Logout
					      0) Exit
					      """);
			
		
			System.out.print("Enter choice: ");

			String line = scanner.nextLine();

			try {
				int selection = Integer.parseInt(line.trim());

				switch (selection) {

				// abstraction complete
				// complexity of how files are created are hidden
				case 1:
					
					// 
					makeFile.run(navigator.current(), scanner);
					break;
				case 2:
					makeFolder.run(navigator.current(), scanner);
					break;
					
				case 4:
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

						String[] cdTokens = cmd.split("\\s+");
						if (cdTokens.length < 2 || !cdTokens[0].equals("cd")) {
							System.out.println("\nNot quite - try: cd <name>, cd .., or cd /");
							System.out.print("Enter command: ");
							continue;
						}

						String target = cdTokens[1];
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
					break;
					
				// view of current folder
				case 3:
					viewFolder.run(navigator.current(), scanner);
					
					break;
				case 9:
					System.out.println("Logging out...\n");
					navigator.toRoot();
					user = new LoginScreen(userManager).show(scanner);
					if (user == null) {
						System.out.println("Goodbye!");
						running = false;
						break;
					}
					System.out.println("Welcome, " + user.getUsername() + "!\n");
					break;
				case 0:
					System.out.println("Goodbye!");
					running = false;
					break;
				default:
					System.out.println("Unknown option.");
				}
			} catch (NumberFormatException e) {
				System.out.println("Please enter a number.");
			}
		}

		scanner.close();

	}
}
