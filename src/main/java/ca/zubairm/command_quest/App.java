package ca.zubairm.command_quest;

import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("Welcome to CommandQuest!");

        while (running) {
            System.out.println("""

                1) Make a file
                2) Make a folder
                3) View current folder
                4) Change folder
                5) My progress
                0) Exit""");
            System.out.print("Enter choice: ");

            String line = scanner.nextLine();

            try {
                int choice = Integer.parseInt(line.trim());

                switch (choice) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        System.out.println("[coming soon]");
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
