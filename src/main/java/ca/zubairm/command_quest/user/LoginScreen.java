package ca.zubairm.command_quest.user;

import java.util.Scanner;

/**
 * The UI layer for accounts: prints the gate, reads input, validates the PIN
 * format, and asks UserManager to do the actual account work.
 *
 * Returns the User now using the App (a real account or a Guest), or null if
 * the user chose Exit.
 */
public class LoginScreen {

    private UserManager userManager;

    public LoginScreen(UserManager userManager) {
        this.userManager = userManager;
    }

    public User show(Scanner scanner) {
        while (true) {
            System.out.println("""
                    New or Returning User?

                    1) Make an Account
                    2) Login
                    3) Continue as Guest
                    0) Exit
                    """);
            System.out.print("Enter choice: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("\nPlease enter a number.\n");
                continue;
            }

            switch (choice) {
            case 1: {
                System.out.print("\nChoose a username: ");
                String username = scanner.nextLine().trim();

                if (userManager.usernameExists(username)) {
                    System.out.println("\nThat username is taken. Try another.\n");
                    break;
                }

                System.out.print("Choose a 4-digit PIN: ");
                String pin = scanner.nextLine().trim();

                if (!pin.matches("\\d{4}")) {
                    System.out.println("\nPIN must be exactly 4 digits.\n");
                    break;
                }

                userManager.addUser(username, pin);
                System.out.println("\nAccount created! You're logged in.\n");
                return userManager.login(username, pin);
            }

            case 2: {
                System.out.print("\nUsername: ");
                String username = scanner.nextLine().trim();

                System.out.print("PIN: ");
                String pin = scanner.nextLine().trim();

                User u = userManager.login(username, pin);
                if (u == null) {
                    System.out.println("\nAccess denied - wrong username or PIN.\n");
                    break;
                }

                System.out.println("\nWelcome back, " + u.getUsername() + "!\n");
                return u;
            }

            case 3:
                System.out.println("\nContinuing as Guest.\n");
                return new User("Guest", "");

            case 0:
                return null;

            default:
                System.out.println("\nUnknown option.\n");
            }
        }
    }
}
