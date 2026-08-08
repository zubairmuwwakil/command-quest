package ca.zubairm.command_quest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.zubairm.command_quest.hub.Folder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

/**
 * The folder tree crosses the network on every request in this design, so
 * these are the tests that matter most: if the tree does not survive the round
 * trip intact, the player silently loses work and nothing in the logs says so.
 */
@DisplayName("FolderDto")
class FolderDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("survives Folder -> DTO -> JSON -> DTO -> Folder unchanged")
    void roundTripsThroughJson() {
        Folder root = new Folder("root");
        root.addFile("todo.md");
        root.addFile("notes.txt");
        root.addSubFolder("photos");
        root.getSubFolders().get("photos").addFile("cat.jpg");

        String json = mapper.writeValueAsString(FolderDto.from(root));
        Folder revived = mapper.readValue(json, FolderDto.class).toDomain();

        assertEquals("root", revived.getName());
        assertTrue(revived.hasFile("todo.md"));
        assertTrue(revived.hasFile("notes.txt"));
        assertTrue(revived.hasSubFolder("photos"), "the subfolder survived");
        assertTrue(revived.getSubFolders().get("photos").hasFile("cat.jpg"),
                "nested contents survived - this is the one that silently vanishes");
    }

    @Test
    @DisplayName("keeps nesting several levels deep")
    void roundTripsDeepNesting() {
        Folder root = new Folder("root");
        root.addSubFolder("a");
        Folder a = root.getSubFolders().get("a");
        a.addSubFolder("b");
        a.getSubFolders().get("b").addFile("deep.txt");

        String json = mapper.writeValueAsString(FolderDto.from(root));
        Folder revived = mapper.readValue(json, FolderDto.class).toDomain();

        assertTrue(revived.getSubFolders().get("a").getSubFolders().get("b").hasFile("deep.txt"));
    }

    @Test
    @DisplayName("serialises an empty folder without inventing contents")
    void roundTripsAnEmptyFolder() {
        Folder revived = mapper
                .readValue(mapper.writeValueAsString(FolderDto.from(new Folder("root"))), FolderDto.class)
                .toDomain();

        assertEquals("root", revived.getName());
        assertTrue(revived.getFiles().isEmpty());
        assertTrue(revived.getSubFolders().isEmpty());
    }

    @Test
    @DisplayName("produces a tree with no back-references that could cycle")
    void jsonHasNoParentReference() {
        Folder root = new Folder("root");
        root.addSubFolder("photos");

        String json = mapper.writeValueAsString(FolderDto.from(root));

        assertFalse(json.contains("parent"),
                "a parent link would make serialisation recurse forever; json was:\n" + json);
    }
}
