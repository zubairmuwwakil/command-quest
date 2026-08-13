package ca.zubairm.command_quest.user;

import java.util.HashMap;
import java.util.Map;

// this class holds the user data and checks if a user name exists in the system


public class UserManager {
	
	// a map is used to store users for fast lookup by username
	private Map<String, User> users = new HashMap<>();

	public boolean usernameExists ( String username) {
		
		return username != null && users.containsKey(username);
	}
	
	public void addUser (String username, String pin) {
		
		users.put(username, new User(username, pin));
	}
	public void changePin (String username, String newPin) {
		
		
		users.put(username, new User(username, newPin));
	}
	
	
	public User login (String username, String pin) {
		
		if (usernameExists(username)) {
			
		
			User user = users.get(username);

			if (user.checkPin(pin)) {
				
				return user;
				
			}
			
		}
		
		return null;
	}
	
}
