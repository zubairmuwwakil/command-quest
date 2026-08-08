package ca.zubairm.command_quest.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns bad input into a clean 400 rather than a 500 with a stack trace.
 *
 * This matters more than usual in a stateless design: the browser supplies the
 * folder tree and the path, so anything malformed, impossible, or hostile
 * arrives as ordinary request data. A rejected command is still a 200 - being
 * wrong is how the game teaches. These are the cases where the request itself
 * does not make sense.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Unknown lessonId, or a path that does not exist in the supplied tree. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /** A field failed its constraint - missing command, path too deep, and so on. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("request body was not valid");

        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    /**
     * The body was not parseable JSON at all.
     *
     * Spring already maps this to 400 by default. It needs an explicit handler
     * only because the catch-all below would otherwise intercept it first and
     * relabel a client mistake as a server fault - telling the browser "my
     * fault, retry later" when the truth is "your JSON is broken".
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Request body was not valid JSON."));
    }

    /** Anything unforeseen: the player gets a generic message, the log gets the detail. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong handling that command."));
    }
}
