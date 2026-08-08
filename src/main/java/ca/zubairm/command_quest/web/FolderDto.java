package ca.zubairm.command_quest.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.zubairm.command_quest.hub.Folder;

/**
 * The folder tree as it travels over the wire.
 *
 * This exists as a separate type from Folder for two reasons.
 *
 * Jackson stays out of the domain: hub.Folder has no annotations, no no-arg
 * constructor requirement, and no knowledge that HTTP exists. The wire format
 * can change without touching game logic, and the game logic can change
 * without breaking clients.
 *
 * And it is explicit. Serialising Folder directly happens to work, but only
 * because Jackson falls back to calling getFiles() and mutating the list it
 * returns - which works solely because Folder hands out its internal
 * collections. Relying on that would mean a future defensive copy in Folder,
 * an unambiguously good change, silently emptying every player's tree.
 *
 * Note there is no parent field, and there must never be one: the tree is
 * serialised on every request, and a back-reference would recurse until the
 * stack overflows.
 */
public record FolderDto(String name, List<String> files, Map<String, FolderDto> subFolders) {

    /** Copies a domain Folder into its wire form, recursively. */
    public static FolderDto from(Folder folder) {
        Map<String, FolderDto> children = new LinkedHashMap<>();
        folder.getSubFolders().forEach((childName, child) -> children.put(childName, from(child)));

        return new FolderDto(folder.getName(), List.copyOf(folder.getFiles()), children);
    }

    /** Rebuilds a domain Folder from the wire form, recursively. */
    public Folder toDomain() {
        Folder folder = new Folder(name);

        if (files != null) {
            files.forEach(folder::addFile);
        }

        if (subFolders != null) {
            subFolders.forEach((childName, childDto) -> {
                folder.addSubFolder(childName);
                Folder child = folder.getSubFolders().get(childName);
                childDto.copyContentsInto(child);
            });
        }

        return folder;
    }

    /**
     * Folder can only create an empty subfolder by name, so a child's contents
     * are filled in afterwards rather than by handing Folder a built subtree.
     */
    private void copyContentsInto(Folder target) {
        if (files != null) {
            files.forEach(target::addFile);
        }

        if (subFolders != null) {
            subFolders.forEach((childName, childDto) -> {
                target.addSubFolder(childName);
                childDto.copyContentsInto(target.getSubFolders().get(childName));
            });
        }
    }

    /** Defensive normalisation so a null from JSON never reaches the domain. */
    public FolderDto {
        files = files == null ? new ArrayList<>() : files;
        subFolders = subFolders == null ? new LinkedHashMap<>() : subFolders;
    }
}
