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
                case 1: {
                	System.out.println("""
                			
                			
                			To make a file type the touch command
                			Next type name of the file you want it to be called
                			Finally type the file extension

                			Here's an example: touch itsAMeMario.jpg

                			Easy right?

                			Now you try.

                			To go back to main menu type * else proceed with your file creation!

                			Enter command:
                			""");

                	// Retry loop: keep asking until a file is made (or the user types *).
                	boolean fileMade = false;
                	while (!fileMade) {
                		line = scanner.nextLine();

                		// ESCAPE: typing * returns to the main menu.
                		if (line.trim().equals("*")) {
                			System.out.println("Returning to the main menu...\n");
                			break;
                		}

                		String[] tokens = line.trim().split("\\s+");

                		// OUTCOME 1: wrong format -> show example and re-prompt.
                		if (tokens.length < 2 || !tokens[0].equals("touch") || !tokens[1].matches("\\w+\\.\\w+")) {
                			System.out.println("\nNot quite - the format was off.");
                			System.out.println("Here's another example: touch chicken.leg\n");
                			System.out.print("Enter command: ");
                		}
                		// OUTCOME 3: format correct, but that file already exists -> re-prompt.
                		else if (currentFolder.hasFile(tokens[1])) {
                			System.out.println("\nYour command format was correct, congrats! :)");
                			System.out.println("The only problem is that file already exists :( Try another file name.");
                			System.out.print("Enter command: ");
                		}
                		// OUTCOME 2: format correct and name is free -> create it and stop looping.
                		else {
                			currentFolder.addFile(tokens[1]);
                			System.out.println("\nFile Successfully created!");
                			fileMade = true;
                		}
                	}
                	break;
                }
                case 2: {
                	System.out.println("""
                			
                			To make a folder type the mkdir command
                			Next type the name of the folder (what you want it to be called)
                			Finally... jk thats it you're done !!

                			Here's an example: mkdir UScream4IceCream

                			Easy right?

                			Now you try.

                			To go back to main menu type * else proceed with your folder creation!
                			""");
                	System.out.print("Enter command: ");

                	// Retry loop: keep asking until a file is made (or the user types *).
                	boolean fileMade = false;
                	while (!fileMade) {
                		line = scanner.nextLine();

                		// ESCAPE: typing * returns to the main menu.
                		if (line.trim().equals("*")) {
                			System.out.println("Returning to the main menu...\n");
                			break;
                		}

                		String[] tokens = line.trim().split("\\s+");

                		// OUTCOME 1: wrong format -> show example and re-prompt.
                		if (tokens.length < 2 || !tokens[0].equals("mkdir") || !tokens[1].matches("\\w+")) {
                			System.out.println("\nNot quite - the format was off.");
                			System.out.println("Here's another example: mkdir chickenCoop\n");
                			System.out.print("Enter command: ");
                		}
                		// OUTCOME 3: format correct, but that file already exists -> re-prompt.
                		else if (currentFolder.hasSubFolder(tokens[1])) {
                			System.out.println("\nYour command format was correct, congrats! :)");
                			System.out.println("The only problem is that folder already exists :( Try another folder name.");
                			System.out.print("Enter command: ");
                		}
                		// OUTCOME 2: format correct and name is free -> create it and stop looping.
                		else {
                			currentFolder.addSubFolder(tokens[1]);
                			System.out.println("\nFolder Successfully created!");
                			fileMade = true;
                		}
                	}
                	break;
                }

                case 4:
                case 5:
                    System.out.println("coming soon");
                    break;
                case 3:

                    if (currentFolder.getFiles().isEmpty() && currentFolder.getSubFolders().isEmpty()) {
                        System.out.println("(empty)");
                    } else {
                        // folders first (with a trailing /), then files - both sorted
                        currentFolder.getSubFolders().keySet().stream()
                                .sorted()
                                .forEach(n -> System.out.println(n + "/"));
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
