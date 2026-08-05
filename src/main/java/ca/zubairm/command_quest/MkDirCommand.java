package ca.zubairm.command_quest;

public class MkDirCommand extends AbstractCommand {

    public MkDirCommand() {
        super(
            "mkdir",             
            "folder",             
            "\\w+",        
            """
            To make a folder type the mkdir command
			Next type the name of the folder (what you want it to be called)
			Finally... jk thats it you're done !!

			Here's an example: mkdir UScream4IceCream

			Easy right?

			Now you try.

			To go back to main menu type * else proceed with your folder creation!
 
            """,
            "mkdir one4All"  
        );
    }

    @Override
    protected boolean exists(Folder folder, String name) {
    	
    	//check if folder exists
    	return(folder.hasSubFolder(name));
    	
    }

    @Override
    protected void create(Folder folder, String name) {
    	
    	//create a folder
    	folder.addSubFolder(name);
    	
    }
}