package pw.caple.bolt.user;

import java.util.UUID;

public class Session {

	private User user;
	private final UUID sessionID;
	private final String browserID;

	public Session(String browserID) {
		this(UUID.randomUUID(), browserID);
	}

	public Session(UUID sessionID, String browserID) {
		if (browserID.length() > 50) {
			browserID = browserID.substring(0, 50);
		}
		this.sessionID = sessionID;
		this.browserID = browserID;
	}

	public String getBrowserID() {
		return browserID;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public boolean isLoggedIn() {
		return user != null;
	}

}
