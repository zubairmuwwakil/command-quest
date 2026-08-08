package ca.zubairm.command_quest.web;

import java.util.List;

/**
 * What happened, and the state the browser should now hold.
 *
 * The domain calls it "succeeded"; the wire calls it "correct". The domain is
 * describing whether an operation worked, the API is describing whether the
 * player got the lesson right. They coincide today and need not always.
 *
 * @param output   the message to show the player
 * @param correct  whether the lesson was passed, which advances progress
 * @param finished whether the exchange is over, by success or by giving up
 * @param hint     a worked example to try next, or null
 * @param path     where the player now stands; changed only by cd
 * @param state    the whole folder tree after the command
 */
public record CommandResponse(
        String output,
        boolean correct,
        boolean finished,
        String hint,
        List<String> path,
        FolderDto state) {
}
