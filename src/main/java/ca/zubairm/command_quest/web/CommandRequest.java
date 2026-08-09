package ca.zubairm.command_quest.web;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One typed line, plus everything the server needs to act on it.
 *
 * The server keeps nothing between requests, so the browser sends the whole
 * folder tree and its location every time. That makes the client untrusted
 * input, which is why the size limits below exist: without them a crafted
 * request could make the server allocate without bound.
 *
 * There is deliberately no lessonId. This record used to carry one, and it
 * decided which command the typed line was allowed to reach - so selecting a
 * lesson tab silently narrowed what the player could type. Dispatch is now on
 * the first word of the command itself, and removing the field is what stops
 * the gate coming back: there is nothing left to narrow with.
 *
 * @param command the line the player typed
 * @param path    folder names from root to where the player stands; empty is root
 * @param state   the whole folder tree, from root
 */
public record CommandRequest(
        @NotNull(message = "command is required")
        @Size(max = 200, message = "command is too long")
        String command,

        @Size(max = 10, message = "folders cannot nest more than 10 deep")
        List<String> path,

        @NotNull(message = "state is required")
        FolderDto state) {

    /** Null path and empty path both mean "at root". */
    public List<String> pathOrRoot() {
        return path == null ? List.of() : path;
    }
}
