package ca.zubairm.command_quest.web;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
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
 * @param lessonId which command is being taught - touch, mkdir, ls, or cd
 * @param command  the line the player typed
 * @param path     folder names from root to where the player stands; empty is root
 * @param state    the whole folder tree, from root
 */
public record CommandRequest(
        @NotBlank(message = "lessonId is required")
        String lessonId,

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
