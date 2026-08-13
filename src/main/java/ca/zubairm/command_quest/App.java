package ca.zubairm.command_quest;

import java.util.Scanner;

import ca.zubairm.command_quest.commands.Command;
import ca.zubairm.command_quest.commands.CreditsCommand;
import ca.zubairm.command_quest.commands.MkDirCommand;
import ca.zubairm.command_quest.commands.TouchCommand;
import ca.zubairm.command_quest.commands.ViewCommand;
import ca.zubairm.command_quest.commands.CdCommand;
import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;
import ca.zubairm.command_quest.user.UserManager;
import ca.zubairm.command_quest.user.LoginScreen;
import ca.zubairm.command_quest.user.User;

public class App {
	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);

		// the sandbox tree + where we are in it
		Folder currentFolder = new Folder("root");
		Navigator navigator = new Navigator(currentFolder);
		currentFolder.addFile("todo.md");
		currentFolder.addFile("notes.txt");

		// The team, hidden the way a real shell hides things: a leading dot, so
		// only "ls -a" lists it. Seeded from Credits so the names are written
		// down in exactly one place.
		currentFolder.addSubFolder(Credits.FOLDER);
		Folder team = currentFolder.getSubFolders().get(Credits.FOLDER);
		Credits.fileNames().forEach(team::addFile);

		// Polymorphism - command is an interface, each variable is set to a subclass
		// (check day 6 in teacher led)
		Command makeFile = new TouchCommand();
		Command makeFolder = new MkDirCommand();
		Command viewFolder = new ViewCommand();
		Command credits = new CreditsCommand();   // hidden - the menu never mentions it
		CdCommand cd = new CdCommand();          // navigation: its own contract, not a Command

		// accounts
		UserManager userManager = new UserManager();
		LoginScreen loginScreen = new LoginScreen(userManager);

		System.out.println("""
				Welcome to Command Quest!
				This game is designed to teach you command line prompts
				But for now lets get started!
				""");

		// Outer loop = one login session. Logout drops back here; Exit ends the app.
		boolean appRunning = true;
		while (appRunning) {

			// login gate - SRP: the gate logic lives in LoginScreen
			User user = loginScreen.show(scanner);
			if (user == null) {              // Exit chosen at the gate
				System.out.println("Goodbye!");
				break;
			}
			System.out.println("Welcome, " + user.getUsername() + "!\n");

			// One check here rather than four inside LoginScreen: making an
			// account, logging in, resetting a PIN and continuing as Guest all
			// arrive at this same line.
			String greeting = Credits.shoutoutFor(user.getUsername());
			if (greeting != null) {
				System.out.println(greeting + "\n");
			}
			navigator.toRoot();              // each session starts at root

			boolean loggedIn = true;
			while (loggedIn) {

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
					// abstraction complete - how files/folders are made is hidden in the commands
					case 1:
						makeFile.run(navigator.current(), scanner);
						break;
					case 2:
						makeFolder.run(navigator.current(), scanner);
						break;
					case 3:
						viewFolder.run(navigator.current(), scanner);
						break;
					case 4:
						cd.run(navigator, scanner);
						break;
					// Deliberately absent from the menu printed above. The web
					// build hides its credits in Shell's second map; the console
					// has a numbered menu, so it hides a number instead.
					case 7:
						credits.run(navigator.current(), scanner);
						break;
					case 9:
						System.out.println("Logging out...\n");
						loggedIn = false;    // drop back to the login gate
						break;
					case 0:
						System.out.println("Goodbye!");
						loggedIn = false;
						appRunning = false;
						break;
					default:
						System.out.println("Unknown option.");
					}
				} catch (NumberFormatException e) {
					System.out.println("Please enter a number.");
				}
			}
		}

		scanner.close();
	}
}
