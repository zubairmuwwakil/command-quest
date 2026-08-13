package ca.zubairm.command_quest.commands;

import java.util.Arrays;
import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

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
    public void run(Navigator navigator, Scanner scanner) {
    	
    	Folder folder = navigator.current();
    	
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

        
            if (tokens[0].equals(keyword) && tokens.length > 2) {
                System.out.println("\n" + spacingLesson(tokens));
                System.out.print("Enter command: ");
            }

     
            else if (tokens.length != 2 || !tokens[0].equals(keyword) || !tokens[1].matches(namePattern)) {
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

    // typically a shell creates multiple folders with a single command, 
    //but this lesson is about creating one at a time. This method explains that to the user.
    private String spacingLesson(String[] tokens) {
        String[] names = Arrays.copyOfRange(tokens, 1, tokens.length);

        StringBuilder message = new StringBuilder()
                .append("Not quite - \"").append(keyword).append("\" makes one ")
                .append(noun).append(" at a time.\n")
                .append("A space starts a new word, so that line asks for ")
                .append(names.length).append(" ").append(noun).append("s: ")
                .append(String.join(", ", names)).append(".\n")
                .append(capitalize(noun)).append(" names cannot contain spaces.");

        
        
        String joined = joinIntoOneName(names);
        if (joined.matches(namePattern)) {
            message.append("\nDid you mean: ").append(keyword).append(" ").append(joined);
        } else {
            message.append("\nHere's an example: ").append(example);
        }

        return message.toString();
    }

    /** "nested", "folder", "me" -> "nestedFolderMe" */
    private String joinIntoOneName(String[] names) {
        StringBuilder joined = new StringBuilder(names[0]);
        for (int i = 1; i < names.length; i++) {
            joined.append(capitalize(names[i]));
        }
        return joined.toString();
    }


    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
