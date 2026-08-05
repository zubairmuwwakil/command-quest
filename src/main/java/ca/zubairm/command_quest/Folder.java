package ca.zubairm.command_quest;

import java.util.ArrayList;
import java.util.List;

/**
 * TASK 2a - a folder in the virtual file tree.
 *
 * For now it just holds a name and a list of file names. It is PURE DATA:
 * notice there is no System.out.println anywhere in here - printing is the
 * App's job, not the folder's. (This class grows to hold subfolders in Step 4.)
 */
public class Folder {

    // private = encapsulated. Nothing outside this class can touch these
    // fields directly; they can only go through the methods below.
    private String name;
    private List<String> files;

    // Constructor: give the folder a name and start it with an empty file list.
    public Folder(String name) {
        this.name = name;
        this.files = new ArrayList<>();
    }

    // Getter for the name.
    public String getName() {
        return name;
    }

    // Add a file name to this folder (we reuse this in Step 3's "touch").
    public void addFile(String fileName) {
        files.add(fileName);
    }

    // Getter for the list of files, so the App can read/stream them.
    public List<String> getFiles() {
        return files;
    }
}
