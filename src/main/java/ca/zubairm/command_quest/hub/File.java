package ca.zubairm.command_quest.hub;

/**
 * A single file in the game's pretend filesystem: a name plus the text it
 * holds.
 *
 * Folder models a place, File models a thing that lives in it. touch creates
 * one with no content, so content starts as "" rather than null.
 */
public class File {

	//Encapsulation for OOP

	private String name;
	private String content;

	public File(String name) {
		this.name = name;
		this.content = "";
	}

	public String getName() {
		return name;
	}

	// Content

	public String getContent() {
		return content;
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

	/** Replaces whatever the file held. A null write clears it. */
	public void write(String text) {
		this.content = (text == null) ? "" : text;
	}
}
