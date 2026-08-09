package ca.zubairm.command_quest.commands;

import java.util.Arrays;
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
                case SUCCEEDED -> System.out.println("\n" + result.output() + "\n");
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

        // The specific complaint has to come before the general one. A spaced
        // name fails the format check below as well, but "the format was off"
        // leaves the player staring at a line that looks perfectly reasonable
        // to them, with no clue which part of it was wrong.
        if (tokens[0].equals(keyword) && tokens.length > 2) {
            return new CommandResult(
                    CommandResult.Outcome.REJECTED,
                    spacingLesson(tokens),
                    null);
        }

        // Exactly two words: the keyword and one name. "< 2" used to let a
        // longer line through and quietly drop everything after tokens[1], so
        // "mkdir nested folder me" made a single folder called "nested".
        if (tokens.length != 2 || !tokens[0].equals(keyword) || !tokens[1].matches(namePattern)) {
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
                CommandResult.Outcome.SUCCEEDED,
                capitalize(noun) + " Successfully created!",
                null);
    }

    /**
     * The message for a name that was typed with spaces in it.
     *
     * A real shell reads "mkdir nested folder me" as three names and makes
     * three folders. Command Quest teaches one name at a time, so it refuses
     * the line - but it counts the words back to the player and offers the
     * joined-up name they almost certainly meant, so the space becomes the
     * lesson rather than a mystery.
     */
    private String spacingLesson(String[] tokens) {
        String[] names = Arrays.copyOfRange(tokens, 1, tokens.length);

        StringBuilder message = new StringBuilder()
                .append("Not quite - \"").append(keyword).append("\" makes one ")
                .append(noun).append(" at a time.\n")
                .append("A space starts a new word, so that line asks for ")
                .append(names.length).append(" ").append(noun).append("s: ")
                .append(String.join(", ", names)).append(".\n")
                .append(capitalize(noun)).append(" names cannot contain spaces.");

        // Only worth suggesting if the suggestion would actually be accepted -
        // "touch my file" joins to "myFile", which touch would reject for
        // having no extension. Falling back to the worked example means the
        // player is never left with a complaint and no way forward.
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
