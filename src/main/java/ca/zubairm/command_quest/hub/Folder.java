package ca.zubairm.command_quest.hub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Folder {

	//Encapsulation for OOP

	private String name;
	private List<File> files;
	private Map<String, Folder> subFolders;

	public Folder(String name) {
		this.name = name;
		this.files = new ArrayList<>();
		this.subFolders = new HashMap<>();
	}

	public String getName() {
		return name;
	}

	// Files

	public void addFile(String fileName) {
		files.add(new File(fileName));
	}

	public List<File> getFiles() {
		return files;
	}

	/** Just the names, for commands like ls that only print the listing. */
	public List<String> getFileNames() {
		List<String> names = new ArrayList<>();
		for (File file : files) {
			names.add(file.getName());
		}
		return names;
	}

	/** The file itself, or null if this folder has no such file. */
	public File getFile(String fileName) {
		for (File file : files) {
			if (file.getName().equals(fileName)) {
				return file;
			}
		}
		return null;
	}

	public boolean hasFile(String file) {

		return getFile(file) != null;
	}

	// SubFolders

	public boolean hasSubFolder(String folder) {

		return subFolders.containsKey(folder);
	}

	public void addSubFolder(String folderName) {
		subFolders.put(folderName, new Folder(folderName));
	}

	public Map<String, Folder> getSubFolders() {
		return subFolders;
	}
}
