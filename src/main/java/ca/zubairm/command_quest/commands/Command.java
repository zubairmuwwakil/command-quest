package ca.zubairm.command_quest.commands;

import java.util.Scanner;

import ca.zubairm.command_quest.hub.Navigator;

//interface - gives a contract for what a command can do
// allows for any command to be executed by the navigator
public interface Command {
    void run(Navigator navigator, Scanner scanner);
}
