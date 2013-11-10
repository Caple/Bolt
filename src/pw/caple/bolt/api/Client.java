package pw.caple.bolt.api;

import pw.caple.bolt.socket.SocketConnection;

/**
 * Represents a single connection using the Bolt protocol. An application can
 * extend this class and override {@link Application#getNewClient()} in order to
 * store additional data with each connection.
 */
public abstract class Client {

	private SocketConnection socket;

	public final void setSocket(SocketConnection socket) {
		this.socket = socket;
	}

	/**
	 * Sends a protocol message to the client.
	 */
	public final void send(String string) {
		socket.send(string);
	}

	/**
	 * Called after the client connects.
	 */
	public abstract void onOpen();

	/**
	 * Called after the client disconnects.
	 */
	public abstract void onClose();

}
