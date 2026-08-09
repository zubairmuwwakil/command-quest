package ca.zubairm.command_quest.web;

import java.util.Map;

import ca.zubairm.command_quest.commands.Shell;
import ca.zubairm.command_quest.hub.Folder;
import ca.zubairm.command_quest.hub.Navigator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * The whole HTTP surface of the game.
 *
 * Every request carries the folder tree and the player's location, and the
 * server keeps nothing afterwards. That is what makes a sleeping free-tier
 * container harmless: waking up with no memory costs the player nothing,
 * because the browser was holding the state all along.
 *
 * This class used to own a dispatch table keyed by lesson, which is how the
 * lesson tabs came to decide what a player was allowed to type. Choosing a
 * command from a typed line now belongs to Shell, and this is back to what it
 * claims to be: unwrap the request, rebuild the world, hand back the result.
 */
@RestController
@RequestMapping("/api")
public class CommandController {

    private final Shell shell = new Shell();

    @PostMapping("/command")
    public CommandResponse command(@Valid @RequestBody CommandRequest request) {
        // Rebuild the world the browser described, act on it, and hand it back.
        Folder root = request.state().toDomain();
        Navigator navigator = Navigator.at(root, request.pathOrRoot());

        Shell.Result run = shell.execute(navigator, request.command());

        return new CommandResponse(
                run.commandId(),
                run.result().output(),
                run.result().succeeded(),
                run.result().finished(),
                run.result().hint(),
                navigator.pathNames(),
                FolderDto.from(root));
    }

    @GetMapping("/lessons")
    public Map<String, Lesson> lessons() {
        return Lesson.catalogue();
    }

    /**
     * Deliberately trivial. The browser calls this on page load and ignores the
     * answer; its only job is to start a sleeping container warming up while
     * the player is still reading the first lesson.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
