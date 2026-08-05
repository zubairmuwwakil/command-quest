package ca.zubairm.command_quest;

public class TouchCommand extends AbstractCommand {

    public TouchCommand() {
    	super(
    		    "touch",
    		    "file",
    		    "\\w+\\.\\w+",
    		    """
    		    To make a file, type the touch command.
    		    Next, type the name you want the file to have.
    		    Finally, type the file extension.

    		    Here's an example: touch itsAMeMario.jpg

    		    Easy, right?

    		    Now you try.

    		    To go back to the main menu, type *.
    		    Otherwise, proceed with your file creation!
    		    """,
    		    "touch chicken.leg"
    		);
    }

    @Override
    protected boolean exists(Folder folder, String name) {
        // return whether the file already exists
    	return (folder.hasFile(name));
    }

    @Override
    protected void create(Folder folder, String name) {
        // create the file 
    	folder.addFile(name);
    	
    }
}