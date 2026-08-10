package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Navigator;

//interface
public interface Command {
    void run(Navigator navigator, Scanner scanner);
}
