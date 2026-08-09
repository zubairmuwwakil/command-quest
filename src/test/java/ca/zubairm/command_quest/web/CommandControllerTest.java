package ca.zubairm.command_quest.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice tests for the one endpoint the game runs on.
 *
 * These boot only the web layer, not the whole application, so they stay fast
 * and fail for reasons to do with HTTP rather than with wiring.
 */
@WebMvcTest(CommandController.class)
@DisplayName("POST /api/command")
class CommandControllerTest {

    @Autowired
    private MockMvc mvc;

    private static final String ROOT_STATE = """
            {"name":"root","files":["todo.md"],"subFolders":{}}""";

    /**
     * No lessonId. The request no longer carries one, which is the whole point:
     * there is nothing in the payload that could say which command is allowed.
     */
    private String body(String command, String path, String state) {
        return """
                {"command":"%s","path":%s,"state":%s}"""
                .formatted(command, path, state);
    }

    @Test
    @DisplayName("creates a file and returns the updated tree")
    void createsAFile() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch cat.jpg", "[]", ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.commandId").value("touch"))
                .andExpect(jsonPath("$.output").value("File Successfully created!"))
                .andExpect(jsonPath("$.state.files").value(org.hamcrest.Matchers.hasItem("cat.jpg")));
    }

    @Test
    @DisplayName("any command runs, whatever the player was last reading")
    void anyCommandRunsAtAnyTime() throws Exception {
        // The regression test for this whole change. There is no longer a field
        // in which to say "the player is on the touch lesson", so mkdir simply
        // works. Under the old dispatch this line was handed to TouchCommand
        // and came back as "Not quite - the format was off."
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("mkdir homework", "[]", ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.commandId").value("mkdir"))
                .andExpect(jsonPath("$.state.subFolders.homework").exists());
    }

    @Test
    @DisplayName("a wrong command is a 200 with correct=false, not an HTTP error")
    void aWrongCommandIsNotAnHttpError() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch nodot", "[]", ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.commandId").value("touch"))
                .andExpect(jsonPath("$.hint").value("touch chicken.leg"));
    }

    @Test
    @DisplayName("an old client still sending lessonId is not broken by the change")
    void toleratesAStaleLessonId() throws Exception {
        // Pages and Render deploy independently, so a browser holding a cached
        // copy of the old front end will keep sending this field for a while.
        // Spring Boot leaves FAIL_ON_UNKNOWN_PROPERTIES off, and this pins that.
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lessonId":"touch","command":"mkdir homework","path":[],"state":%s}"""
                                .formatted(ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandId").value("mkdir"));
    }

    @Test
    @DisplayName("cd moves the player and returns the new path")
    void cdReturnsTheNewPath() throws Exception {
        String withPhotos = """
                {"name":"root","files":[],"subFolders":{"photos":{"name":"photos","files":[],"subFolders":{}}}}""";

        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("cd photos", "[]", withPhotos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.path[0]").value("photos"));
    }

    @Test
    @DisplayName("pwd reports where the player is standing")
    void pwdReportsTheLocation() throws Exception {
        String withPhotos = """
                {"name":"root","files":[],"subFolders":{"photos":{"name":"photos","files":[],"subFolders":{}}}}""";

        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("pwd", "[\"photos\"]", withPhotos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.commandId").value("pwd"))
                .andExpect(jsonPath("$.output").value("root/photos"));
    }

    @Test
    @DisplayName("operates on the folder the player is standing in, not root")
    void operatesOnTheCurrentFolder() throws Exception {
        String withPhotos = """
                {"name":"root","files":[],"subFolders":{"photos":{"name":"photos","files":[],"subFolders":{}}}}""";

        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch cat.jpg", "[\"photos\"]", withPhotos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.subFolders.photos.files")
                        .value(org.hamcrest.Matchers.hasItem("cat.jpg")))
                .andExpect(jsonPath("$.state.files").isEmpty());
    }

    @Test
    @DisplayName("an unknown command is a helpful 200, not a 400")
    void unknownCommandIsAnswered() throws Exception {
        // This test used to assert the bug. A beginner typing a command the
        // game has not taught is not a protocol violation, and answering with
        // a bare HTTP error taught them nothing.
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("sudo rm", "[]", ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.commandId").doesNotExist())
                .andExpect(jsonPath("$.output").value(org.hamcrest.Matchers.containsString("sudo")))
                .andExpect(jsonPath("$.hint").value("help"));
    }

    @Test
    @DisplayName("a near miss is answered with the command the player meant")
    void aNearMissIsCorrected() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("mkdr homework", "[]", ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.output").value(org.hamcrest.Matchers.containsString("mkdir")));
    }

    @Test
    @DisplayName("a path that does not exist is a 400, not a 500")
    void impossiblePathIsABadRequest() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch cat.jpg", "[\"nowhere\"]", ROOT_STATE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("malformed JSON is a 400 with no stack trace")
    void malformedJsonIsABadRequest() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a missing command is rejected by validation")
    void missingCommandIsRejected() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"path":[],"state":%s}""".formatted(ROOT_STATE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("serves the lesson text separately from command results")
    void servesLessons() throws Exception {
        mvc.perform(get("/api/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.touch.title").value("Make a file"))
                .andExpect(jsonPath("$.touch.example").value("touch chicken.leg"))
                .andExpect(jsonPath("$.pwd.title").value("Find where you are"));
    }

    @Test
    @DisplayName("answers a health check so the browser can wake a sleeping container")
    void health() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
