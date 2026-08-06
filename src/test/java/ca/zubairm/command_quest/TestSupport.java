package ca.zubairm.command_quest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Shared helpers for the test suite.
 *
 * The game talks to the user through System.out and reads through a Scanner, so
 * almost every test needs to (a) feed it scripted keystrokes and (b) read back
 * what it printed. Both concerns live here so the individual tests stay short
 * and readable.
 */
final class TestSupport {

    private TestSupport() {
        // utility class - never instantiated
    }

    /**
     * Builds a Scanner over scripted input, exactly as if the user had typed
     * each line and pressed Enter.
     *
     * This is the reason AbstractCommand.run() is testable at all: it accepts a
     * Scanner rather than creating one over System.in, so a test can supply
     * keystrokes without a keyboard.
     */
    static Scanner keystrokes(String... lines) {
        return new Scanner(new StringReader(String.join("\n", lines) + "\n"));
    }

    /** Captures everything written to System.out while the given action runs. */
    static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Runs the whole game start to finish on scripted input and returns
     * everything it printed.
     *
     * App.main() builds its own Scanner over System.in, so unlike
     * AbstractCommand.run() it can only be driven by swapping the real standard
     * input out from underneath it. That extra ceremony is the practical cost
     * of not injecting the dependency.
     *
     * The script must end with "0" so the game exits; main() calls
     * scanner.nextLine() unguarded and would otherwise fail on end of input.
     */
    static String runApp(String... lines) {
        InputStream originalIn = System.in;
        byte[] script = (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8);
        try {
            System.setIn(new ByteArrayInputStream(script));
            return captureOutput(() -> App.main(new String[0]));
        } finally {
            System.setIn(originalIn);
        }
    }
}
