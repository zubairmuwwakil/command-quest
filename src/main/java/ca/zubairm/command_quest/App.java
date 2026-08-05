package ca.zubairm.command_quest;

import java.util.Scanner;


public class App {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        boolean running = true;

        int operatingSystem = -1;

        Folder currentFolder = new Folder("root");
        Command makeFile   = new TouchCommand();
        Command makeFolder = new MkDirCommand();
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
                		
                // abstraction complete
                //complexity of how files are created are hidden
                case 1: makeFile.run(currentFolder, scanner);   break;
                case 2: makeFolder.run(currentFolder, scanner); break;


                case 4:
                case 5:
                    System.out.println("coming soon");
                    break;
                //view of current folder
                    
                case 3:
                	System.out.println("\nHere are your current files and folders!");
                	System.out.println("");
           

                    if (currentFolder.getFiles().isEmpty() && currentFolder.getSubFolders().isEmpty()) {
                        System.out.println("(empty)");
                    } else {
                        // folders first (with a trailing /), then files - both sorted
                        currentFolder.getSubFolders().keySet().stream()
                                .sorted()
                                .forEach(n -> System.out.println(n + "/"));
                        currentFolder.getFiles().stream()
                                .sorted()
                                //printing each folder and name
                                .forEach(name -> System.out.println(name+"\n"));
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
