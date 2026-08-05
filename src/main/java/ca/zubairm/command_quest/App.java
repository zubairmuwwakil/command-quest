package ca.zubairm.command_quest;

import java.util.Scanner;


public class App {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        boolean running = true;

        int operatingSystem = -1;

        Folder currentFolder = new Folder("root");
        currentFolder.addFile("todo.md");
        currentFolder.addFile("notes.txt");

        System.out.println("""
        		Welcome to Command Quest!
        		This game is designed to teach you command line prompts
        		But for now lets get started!
        		""");

        // CHANGED BY REVIEW: was || (always true -> infinite loop). && makes it
        // stop once you pick a valid OS: loop while it's NOT 1 AND NOT 2 AND NOT 3.
        while (operatingSystem != 1 && operatingSystem != 2 && operatingSystem != 3) {
	        System.out.println("""
	        		1) MacOS
	        		2) Windows
	        		3) Linux

	        		""");

	        System.out.print("Enter choice: ");

	        // GET USERS OS SO COMMAND LINE DIRECTIONS ARE CORRECT
	        String choice = scanner.nextLine();

	        try {
	            operatingSystem = Integer.parseInt(choice.trim());
	        }
			catch (NumberFormatException e) {
			                System.out.println("\nPlease enter a valid number.");
			}
        }

        System.out.println("\nThanks! :) I hope you enjoy your journey into Command Quest ~\n");


        while (running) {

            System.out.println("""
            		Choose a number to learn how the command works!

                    1) Make a file
                    2) Make a folder
                    3) View current folder
                    4) Change folder
                    5) My progress
                    0) Exit
                    """);
            System.out.print("Enter choice: ");

            String line = scanner.nextLine();

            try {
                int selection = Integer.parseInt(line.trim());

                switch (selection) {
                case 1:
                	System.out.println("""
                			to make a file type the touch command
                			then type name of the file you want it to be called
                			then type the file extension
                			Here's an example: touch itsAMeMario.jpg

                			Easy right?

                			Now you try.

                			Enter command:
                			""");
                	line = scanner.nextLine();

                	//read user input
                	/*
                	 * maybe next 3 cases?
                	 * case 1 its invalid because of incorrect command check that touch exists
                	 * and it goes a word then . then another word
                	 * if case 1 tell user to try again because of incorrect format show a different example
                	 * Here's another example touch chicken.leg
                	 *
                	 * Enter command:
                	 * case 2 is inverse of case 1
                	 *
                	 * File Successfully created then break switch to re enter whiel loop
                	 *
                	 * case 3 invalid because file already exists
                	 *
                	 * tell user command fromat was correct congrats ! :) only problem is the file already exists :(
                	 * Try another file name
                	 *
                	 *
                	 */

                	// --- implementation of your 3-outcome plan ---
                	String[] tokens = line.trim().split("\\s+");

                	// OUTCOME 1: wrong format. Needs "touch <name>.<ext>" (a word, a dot, a word).
                	if (tokens.length < 2 || !tokens[0].equals("touch") || !tokens[1].matches("\\w+\\.\\w+")) {
                		System.out.println("Not quite - the format was off.");
                		System.out.println("Here's another example: touch chicken.leg");
                	}
                	// OUTCOME 3: format correct, but that file already exists.
                	else if (currentFolder.hasFile(tokens[1])) {
                		System.out.println("Your command format was correct, congrats! :)");
                		System.out.println("The only problem is that file already exists :( Try another file name.");
                	}
                	// OUTCOME 2: format correct and the name is free -> create it.
                	else {
                		currentFolder.addFile(tokens[1]);
                		System.out.println("File Successfully created!");
                	}
                	break;

                case 2:
                case 4:
                case 5:
                    System.out.println("coming soon");
                    break;
                case 3:

                    if (currentFolder.getFiles().isEmpty()) {
                        System.out.println("(empty)");
                    } else {
                        currentFolder.getFiles().stream()
                                .sorted()
                                .forEach(name -> System.out.println(name));
                    }
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
