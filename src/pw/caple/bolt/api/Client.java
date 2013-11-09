package pw.caple.bolt.api;

import pw.caple.bolt.Connection;


public class Client {

	private final Connection socket;

	public Client(Connection socket) {
		this.socket = socket;
	}

	public void sendErrorMessage(String message) {
		send("error \"" + message + "\"");
	}

	public void send(String string) {
		socket.send(string);
	}

}
