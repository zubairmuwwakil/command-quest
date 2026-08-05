package ca.zubairm.command_quest;

public class TouchCommand extends AbstractCommand {

    public TouchCommand() {
        super(
            "touch",              
            "file",              
            "\\w+\\.\\w+",        
            """
            To make a file type the touch command
			Next type name of the file you want it to be called
			Finally type the file extension
	
			Here's an example: touch itsAMeMario.jpg
	
			Easy right?
	
			Now you try.
	
			To go back to main menu type * else proceed with your file creation!
	
			Enter command:
            """,
            "touch chicken.leg"   
        );
    }

    @Override
    protected boolean exists(Folder folder, String name) {
        // return whether the file already exists   (hint: folder.hasFile)
    }

    @Override
    protected void create(Folder folder, String name) {
        // create the file   (hint: folder.addFile)
    }
}