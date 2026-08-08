package ca.zubairm.command_quest;

import java.util.Scanner;

import ca.zubairm.command_quest.commands.Command;
import ca.zubairm.command_quest.commands.MkDirCommand;
import ca.zubairm.command_quest.commands.TouchCommand;
import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.user.UserManager;
import ca.zubairm.command_quest.user.LoginScreen;
import ca.zubairm.command_quest.user.User;
import ca.zubairm.command_quest.hub.Navigator;

public class App {
	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);

		boolean running = true;

		int operatingSystem = -1;

		Folder currentFolder = new Folder("root");
		
		Navigator navigator = new Navigator(currentFolder);

		// Polymorphism 
		
		Command makeFile = new TouchCommand();
		Command makeFolder = new MkDirCommand();

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
					System.out.println("coming soon");
					break;
					
				// view of current folder
				case 3:
					System.out.println("Current folder: " + navigator.current().getFiles());
					System.out.println("Files: " + navigator.current().getSubFolders());
					
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
