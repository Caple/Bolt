package pw.caple.bolt.user;

import java.util.Arrays;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.api.Protocol;
import pw.caple.bolt.api.Protocol.SecurityClearance;

public class User {

	private int id;

	static void newUser(String username, String password) {

	}

	static User getUser(String name) {
		return null;
	}

	private String name;

	private byte[] passwordHash;

	private byte[] passwordSalt;

	public boolean isPassword(String password) {
		byte[] hash = Hashing.computeHash(password, passwordSalt);
		return Arrays.equals(hash, passwordHash);
	}

	public void setPassword(String password) {
		byte[] salt = Hashing.generateSalt();
		byte[] hash = Hashing.computeHash(password, salt);
		passwordHash = hash;
		passwordSalt = salt;
	}

	//Session session = client.getSession();
	//		if (!session.isLoggedIn()) {
	//			try (Connection connection = Core.getDB()) {
	//				String selectStatement = "SELECT id, password_hash, password_salt FROM bolt.user WHERE name=?";
	//				PreparedStatement statement = connection.prepareStatement(selectStatement);
	//				statement.setString(1, username);
	//				ResultSet result = statement.executeQuery();
	//				if (result.next()) {
	//					Hashing sec = new Hashing();
	//					byte[] hash = result.getBytes("password_hash");
	//					byte[] salt = result.getBytes("password_salt");
	//					byte[] inputHash = sec.computeHash(password, salt);
	//					if (Arrays.equals(hash, inputHash)) {
	//						User user = new User(result.getInt("id"));
	//						session.setUser(user);
	//						client.send("loginResult success");
	//					} else {
	//						client.send("loginResult fail wrong_password");
	//					}
	//				} else {
	//					client.send("loginResult fail no_such_username");
	//				}
	//				result.close();
	//				statement.close();
	//			} catch (SQLException e) {
	//				e.printStackTrace();
	//			}
	//		} else {
	//			client.sendErrorMessage("login attempt failed. you are already logged in!");
	//		}

	@Protocol(security = SecurityClearance.PRIVATE)
	static void whois(Client client, String username) {
		//		try (Connection connection = Core.getDB()) {
		//			String selectStatement = "SELECT id, name FROM bolt.user WHERE name=?";
		//			PreparedStatement statement = connection.prepareStatement(selectStatement);
		//			statement.setString(1, username);
		//			ResultSet result = statement.executeQuery();
		//			if (result.next()) {
		//				client.send("Name: " + result.getString("name")
		//						+ "\nID: " + result.getInt("id"));
		//			} else {
		//				client.send("log username not found");
		//			}
		//			result.close();
		//			statement.close();
		//		} catch (SQLException e) {
		//			e.printStackTrace();
		//		}
	}

	// END PROTOCOL

	private final Protocol.SecurityClearance clearance;

	public User(int id) {
		clearance = SecurityClearance.PUBLIC;
	}

	public static void testDB(Client client) {
		//		try (Connection connection = Core.getDB()) {
		//			String selectStatement = "SELECT name FROM bolt.user WHERE session_id=?";
		//			PreparedStatement statement = connection.prepareStatement(selectStatement);
		//			statement.setString(1, "blah");
		//			ResultSet result = statement.executeQuery();
		//			if (result.next()) {
		//				String name = result.getString("name");
		//				client.send(name);
		//			} else {
		//				client.send("no session present");
		//			}
		//			result.close();
		//			statement.close();
		//		} catch (SQLException e) {
		//			e.printStackTrace();
		//		}
	}

}
