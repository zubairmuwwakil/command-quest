package ca.zubairm.command_quest.user;

// this is class file is for user creation 

public class User {
	
	
	//user creation relies on a username and pin
	
	private String username, pin;
	
	//due to encapsulation create a constructor 
	// for getter and setter 
	
	public User (String username, String pin) {
		
		this.username = username;
		this.pin = pin;
		
		
	}
	
	public String getUsername () {
		
		return username;
		
	}
	
	public boolean checkPin (String entry) {
		
		return pin.equals(entry);
	}
	
}
