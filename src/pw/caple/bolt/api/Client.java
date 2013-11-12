package pw.caple.bolt.api;

import pw.caple.bolt.socket.ProtocolIntermediate;

/**
 * Represents a single connection using the Bolt protocol. An application can
 * extend this class and override {@link Application#getNewClient()} in order to
 * store additional data with each connection.
 */
public abstract class Client {

	private final static int DEFAULT_TIMEOUT = 10000;
	private ProtocolIntermediate protocol;

	public final void setIntermediate(ProtocolIntermediate protocol) {
		this.protocol = protocol;
	}

	/**
	 * Calls a protocol method on the client and waits for the result.
	 * 
	 * @return
	 */
	public final String call(String method, Object... args) {
		return protocol.sendBlockingMessage(DEFAULT_TIMEOUT, method, args);
	}

	/**
	 * Calls a protocol method on the client asynchronously.
	 */
	public final void callAsync(String method, Object... args) {
		protocol.sendAsynchronousMessage(method, args);
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
