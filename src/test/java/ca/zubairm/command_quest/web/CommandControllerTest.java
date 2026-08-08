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

    private String body(String lessonId, String command, String path, String state) {
        return """
                {"lessonId":"%s","command":"%s","path":%s,"state":%s}"""
                .formatted(lessonId, command, path, state);
    }

    @Test
    @DisplayName("creates a file and returns the updated tree")
    void createsAFile() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch", "touch cat.jpg", "[]", ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.output").value("File Successfully created!"))
                .andExpect(jsonPath("$.state.files").value(org.hamcrest.Matchers.hasItem("cat.jpg")));
    }

    @Test
    @DisplayName("a wrong command is a 200 with correct=false, not an HTTP error")
    void aWrongCommandIsNotAnHttpError() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch", "touch nodot", "[]", ROOT_STATE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.hint").value("touch chicken.leg"));
    }

    @Test
    @DisplayName("cd moves the player and returns the new path")
    void cdReturnsTheNewPath() throws Exception {
        String withPhotos = """
                {"name":"root","files":[],"subFolders":{"photos":{"name":"photos","files":[],"subFolders":{}}}}""";

        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("cd", "cd photos", "[]", withPhotos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.path[0]").value("photos"));
    }

    @Test
    @DisplayName("operates on the folder the player is standing in, not root")
    void operatesOnTheCurrentFolder() throws Exception {
        String withPhotos = """
                {"name":"root","files":[],"subFolders":{"photos":{"name":"photos","files":[],"subFolders":{}}}}""";

        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch", "touch cat.jpg", "[\"photos\"]", withPhotos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.subFolders.photos.files")
                        .value(org.hamcrest.Matchers.hasItem("cat.jpg")))
                .andExpect(jsonPath("$.state.files").isEmpty());
    }

    @Test
    @DisplayName("an unknown lesson is a 400, not a 500")
    void unknownLessonIsABadRequest() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("sudo", "sudo rm", "[]", ROOT_STATE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a path that does not exist is a 400, not a 500")
    void impossiblePathIsABadRequest() throws Exception {
        mvc.perform(post("/api/command").contentType(MediaType.APPLICATION_JSON)
                        .content(body("touch", "touch cat.jpg", "[\"nowhere\"]", ROOT_STATE)))
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
                                {"lessonId":"touch","path":[],"state":%s}""".formatted(ROOT_STATE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("serves the lesson text separately from command results")
    void servesLessons() throws Exception {
        mvc.perform(get("/api/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.touch.title").value("Make a file"))
                .andExpect(jsonPath("$.touch.example").value("touch chicken.leg"));
    }

    @Test
    @DisplayName("answers a health check so the browser can wake a sleeping container")
    void health() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
