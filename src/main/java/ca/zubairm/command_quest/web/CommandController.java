package ca.zubairm.command_quest.web;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import ca.zubairm.command_quest.commands.CdCommand;
import ca.zubairm.command_quest.commands.CommandResult;
import ca.zubairm.command_quest.commands.MkDirCommand;
import ca.zubairm.command_quest.commands.TouchCommand;
import ca.zubairm.command_quest.commands.ViewCommand;
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
 */
@RestController
@RequestMapping("/api")
public class CommandController {

    private final TouchCommand touch = new TouchCommand();
    private final MkDirCommand mkdir = new MkDirCommand();
    private final ViewCommand ls = new ViewCommand();
    private final CdCommand cd = new CdCommand();

    /**
     * One uniform dispatch table built from two different contracts.
     *
     * touch, mkdir, and ls take a Folder - they change what is in front of you.
     * cd takes the Navigator - it changes where you are. Widening the Command
     * interface so all four matched would hand every command the power to move
     * the player, which only one of them needs.
     *
     * Adapting differently shaped things into one interface for routing is a
     * boundary concern, so it lives here rather than in the domain.
     */
    private final Map<String, BiFunction<Navigator, String, CommandResult>> lessons = Map.of(
            "touch", (nav, input) -> touch.execute(nav.current(), input),
            "mkdir", (nav, input) -> mkdir.execute(nav.current(), input),
            "ls",    (nav, input) -> ls.execute(nav.current(), input),
            "cd",    (nav, input) -> cd.execute(nav, input));

    @PostMapping("/command")
    public CommandResponse command(@Valid @RequestBody CommandRequest request) {
        BiFunction<Navigator, String, CommandResult> lesson = lessons.get(request.lessonId());
        if (lesson == null) {
            throw new IllegalArgumentException("Unknown lesson: " + request.lessonId());
        }

        // Rebuild the world the browser described, act on it, and hand it back.
        Folder root = request.state().toDomain();
        Navigator navigator = Navigator.at(root, request.pathOrRoot());

        CommandResult result = lesson.apply(navigator, request.command());

        return new CommandResponse(
                result.output(),
                result.succeeded(),
                result.finished(),
                result.hint(),
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

    /** Exposed for the front end to know what it may ask for. */
    public List<String> lessonIds() {
        return List.copyOf(lessons.keySet());
    }
}
