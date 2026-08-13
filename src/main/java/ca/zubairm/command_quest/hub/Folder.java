package ca.zubairm.command_quest.hub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Folder {
	
	//Encapsulation for OOP
	
	private String name;
	//for files a list was used because it preserves the order of insertion 
	private List<String> files;
	//for subfolders a map was used because it allows for fast lookup by name
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
		files.add(fileName);
	}

	public List<String> getFiles() {
		return files;
	}

	public boolean hasFile(String file) {

		return files.contains(file);
	}

	// SubFolders

	public boolean hasSubFolder(String folder) {

		return subFolders.containsKey(folder);
	}

	public void addSubFolder(String folderName) {
		subFolders.put(folderName, new Folder(folderName));
	}

	// get a subfolder used by nested folders 
	
	public Folder getSubFolder(String folderName) {
		return subFolders.get(folderName);
	}

	public Map<String, Folder> getSubFolders() {
		return subFolders;
	}
}
