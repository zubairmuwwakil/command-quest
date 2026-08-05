package ca.zubairm.command_quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class Folder {

    
    private String name;
    private List<String> files;
    private Map<String, Folder> subFolders;

    public Folder(String name) {
        this.name = name;
        this.files = new ArrayList<>();
        this.subFolders = new HashMap<>();
    }
    

    public String getName() {
        return name;
    }

    public void addFile(String fileName) {
        files.add(fileName);
    }

    public List<String> getFiles() {
        return files;
    }
    public boolean hasFile (String file) {
    	
    	return files.contains(file);
    }
    public boolean hasFolder (String folder) {
	
    	return subFolders.containsKey(folder);
    }
    
    
    
    
}
