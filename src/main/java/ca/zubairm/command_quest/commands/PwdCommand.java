package ca.zubairm.command_quest.commands;

import ca.zubairm.command_quest.hub.Navigator;

/**
 * The pwd command - "print working directory", or plainly: where am I?
 *
 * Like CdCommand this takes a Navigator and deliberately does NOT implement
 * Command, whose contract is execute(Folder, String). The reason here is the
 * inverse of cd's: cd needs the Navigator to CHANGE the location, pwd needs it
 * to READ one. A Folder cannot answer the question at all, because it holds no
 * reference to its parent - an omission the stateless web API depends on, since
 * a tree with parent links cannot be serialised to JSON without cycles.
 *
 * There is no run() and no "*" branch: pwd is reachable from the browser only,
 * so it never takes part in a console retry loop.
 */
public class PwdCommand {

    public CommandResult execute(Navigator navigator, String input) {
        if (!input.trim().equals("pwd")) {
            return new CommandResult(
                    CommandResult.Outcome.REJECTED,
                    "Not quite - pwd takes no arguments. Type it on its own.",
                    "pwd");
        }

        return new CommandResult(
                CommandResult.Outcome.SUCCEEDED,
                navigator.breadcrumb(),
                null);
    }
}
