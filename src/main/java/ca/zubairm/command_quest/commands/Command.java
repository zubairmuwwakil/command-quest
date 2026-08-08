package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Folder;

//interface
public interface Command {
    void run(Folder folder, Scanner scanner);
}
