package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;

//abstraction for OOP

public abstract class AbstractCommand implements Command {

    private final String keyword;    
    private final String noun;        
    private final String namePattern;  
    private final String lesson;       
    private final String example;      

    protected AbstractCommand(String keyword, String noun, String namePattern,
                              String lesson, String example) {
        this.keyword = keyword;
        this.noun = noun;
        this.namePattern = namePattern;
        this.lesson = lesson;
        this.example = example;
    }

    
    protected abstract boolean exists(Folder folder, String name);
    protected abstract void create(Folder folder, String name);

    
    @Override
    public void run(Folder folder, Scanner scanner) {
        System.out.println(lesson);
        System.out.print("Enter command: ");

        boolean done = false;
        while (!done) {
            String line = scanner.nextLine();

            
            if (line.trim().equals("*")) {
                System.out.println("Returning to the main menu...\n");
                break;
            }

            String[] tokens = line.trim().split("\\s+");

          
            if (tokens.length < 2 || !tokens[0].equals(keyword) || !tokens[1].matches(namePattern)) {
                System.out.println("\nNot quite - the format was off.");
                System.out.println("Here's another example: " + example + "\n");
                System.out.print("Enter command: ");
            }
 
            else if (exists(folder, tokens[1])) {
                System.out.println("\nYour command format was correct, congrats! :)");
                System.out.println("The only problem is that " + noun + " already exists :( Try another " + noun + " name.");
                System.out.print("Enter command: ");
            }
           
            else {
                create(folder, tokens[1]);
                System.out.println("\n" + capitalize(noun) + " Successfully created!\n");
                done = true;
            }
        }
    }

   
    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
