package ca.zubairm.command_quest.user;

import java.util.HashMap;
import java.util.Map;

// this class holds the user data and checks if a user name exists in the system


public class UserManager {
	
	private Map<String, User> users = new HashMap<>();

	public boolean usernameExists ( String username) {
		
		return username != null && users.containsKey(username);
	}
	
	public void addUser (String username, String pin) {
		
		users.put(username, new User(username, pin));
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
