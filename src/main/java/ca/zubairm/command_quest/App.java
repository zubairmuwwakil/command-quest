package ca.zubairm.command_quest;

import java.util.Scanner;


public class App {
    public static void main(String[] args) {

        
        Scanner scanner = new Scanner(System.in);

        
        boolean running = true;

        // TASK 2b - create and seed the current folder ONCE, before the loop,
        // so it survives across menu iterations. Added out of alphabetical
        // order on purpose, to prove the stream sorts them in case 3.
        Folder currentFolder = new Folder("root");
        currentFolder.addFile("todo.md");
        currentFolder.addFile("notes.txt");

        while (running) {

            System.out.println("""
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
                case 2:
                case 4:
                case 5:
                    System.out.println("[coming soon]");
                    break;
                case 3:
                    // TASK 2b - list the current folder's files, sorted (stream).
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
                    break; // fix: without this, choosing 0 falls through to default
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
