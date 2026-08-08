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

    
    /**
     * The console conversation: print the lesson, then keep asking until the
     * player succeeds or gives up.
     *
     * All the judgement now lives in execute(). What is left here is the part
     * that is genuinely about a terminal - printing, reading, and looping - so
     * there is exactly one copy of the rules and two ways to reach them.
     */
    @Override
    public void run(Folder folder, Scanner scanner) {
        System.out.println(lesson);
        System.out.print("Enter command: ");

        while (true) {
            CommandResult result = execute(folder, scanner.nextLine());

            switch (result.outcome()) {
                case CANCELLED -> System.out.println(result.output() + "\n");
                case CREATED -> System.out.println("\n" + result.output() + "\n");
                case REJECTED -> {
                    System.out.println("\n" + result.output());
                    if (result.hint() != null) {
                        System.out.println("Here's another example: " + result.hint() + "\n");
                    }
                    System.out.print("Enter command: ");
                }
            }

            if (result.finished()) {
                break;
            }
        }
    }

    @Override
    public CommandResult execute(Folder folder, String input) {
        if (input.trim().equals("*")) {
            return new CommandResult(
                    CommandResult.Outcome.CANCELLED,
                    "Returning to the main menu...",
                    null);
        }

        String[] tokens = input.trim().split("\\s+");

        if (tokens.length < 2 || !tokens[0].equals(keyword) || !tokens[1].matches(namePattern)) {
            return new CommandResult(
                    CommandResult.Outcome.REJECTED,
                    "Not quite - the format was off.",
                    example);
        }

        if (exists(folder, tokens[1])) {
            return new CommandResult(
                    CommandResult.Outcome.REJECTED,
                    "Your command format was correct, congrats! :)\n"
                            + "The only problem is that " + noun + " already exists :( "
                            + "Try another " + noun + " name.",
                    null);
        }

        create(folder, tokens[1]);
        return new CommandResult(
                CommandResult.Outcome.CREATED,
                capitalize(noun) + " Successfully created!",
                null);
    }


    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
