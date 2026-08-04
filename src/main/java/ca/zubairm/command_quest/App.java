package ca.zubairm.command_quest;

import java.util.Scanner;

/**
 * CommandQuest - Step 1: the menu loop.
 * Shows TWO ways to write the same loop. Pattern A runs; Pattern B is a
 * commented-out alternative so you can compare. Use only ONE in real code.
 */
public class App {
    public static void main(String[] args) {

        // One Scanner for the whole program, reading from the keyboard.
        Scanner scanner = new Scanner(System.in);

        // ===== PATTERN A: boolean flag (this is the version that runs) =====

        // Sentinel flag: the loop runs while this is true.
        // Only the Exit option (0) flips it to false.
        boolean running = true;

        while (running) {

            // Print the menu EVERY pass so it reappears after each action.
            System.out.println("""
                    1) Make a file
                    2) Make a folder
                    3) View current folder
                    4) Change folder
                    5) My progress
                    0) Exit
                    """);
            System.out.print("Enter choice: ");

            // Read the whole line as text (avoids the nextInt/nextLine trap).
            String line = scanner.nextLine();

            try {
                // Convert text -> int. Throws NumberFormatException if not a number.
                int selection = Integer.parseInt(line.trim());

                switch (selection) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    System.out.println("[coming soon]");
                    break;
                case 0:
                    System.out.println("Goodbye!");
                    running = false; // flips the sentinel -> loop ends on next check
                    break;
                default:
                    System.out.println("Unknown option.");
                }
            } catch (NumberFormatException e) {
                // Runs only if parseInt failed (e.g. the user typed "abc").
                System.out.println("Please enter a number.");
            }
        }

        scanner.close();

        /*
         * ===== PATTERN B: value sentinel (alternative - commented out) =====
         *
         * Same behaviour, different shape: while(true) + break, and the value 0
         * itself ends the loop instead of a boolean flag. Note there is no
         * "case 0" here, because the check above the switch handles exit.
         *
         * while (true) {
         *     System.out.println(menuText);      // print the menu each pass
         *     System.out.print("Enter choice: ");
         *     String line = scanner.nextLine();
         *
         *     int selection;
         *     try {
         *         selection = Integer.parseInt(line.trim());
         *     } catch (NumberFormatException e) {
         *         System.out.println("Please enter a number.");
         *         continue;                       // skip the rest, loop again
         *     }
         *
         *     if (selection == 0) {               // sentinel VALUE ends the loop
         *         System.out.println("Goodbye!");
         *         break;
         *     }
         *
         *     switch (selection) {
         *         case 1: case 2: case 3: case 4: case 5:
         *             System.out.println("[coming soon]");
         *             break;
         *         default:
         *             System.out.println("Unknown option.");
         *     }
         * }
         */
    }
}
