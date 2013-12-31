package pw.caple.bolt.api;

import pw.caple.bolt.socket.ProtocolIntermediate;

/**
 * Represents a single connection using the Bolt protocol. An application can
 * extend this class and override {@link Application#getNewClient()} in order to
 * store additional data with each connection.
 */
public abstract class Client {

	/**
	 * Maximum time in milliseconds to wait for a result.
	 */
	private final static int BLOCKING_TIMEOUT = 10000;
	private ProtocolIntermediate protocol;
	private Application application;

	public final void init(Application application, ProtocolIntermediate protocol) {
		this.protocol = protocol;
	}

	/**
	 * In most situations you should use {@link Client#send} instead. This
	 * function calls a protocol method on the client and blocks until it
	 * receives a result. If the call to the client takes longer than the time
	 * specified by {@link Client#BLOCKING_TIMEOUT} to complete, then the thread
	 * resumes and <code>null</code> is returned instead.
	 * 
	 * @param method
	 *            the client function to call (<code>case-sensitive</code>)
	 * @param args
	 *            arguments to send
	 * @return The result of the call or <code>null</code> if no result.
	 */
	public final String call(String method, Object... args) {
		return protocol.sendBlockingCall(BLOCKING_TIMEOUT, method, args);
	}

	/**
	 * Runs a method on the client asynchronously.
	 * 
	 * @param method
	 *            the client function to call (<code>case-sensitive</code>)
	 * @param args
	 *            arguments to send; may also include a {@link Callback}
	 */
	public final void send(String method, Object... args) {
		protocol.sendAsynchronousCall(method, args);
	}

	/**
	 * Gets the application this client is connected to.
	 * 
	 * @return The application the client belongs to.
	 */
	public final Application getApplication() {
		return application;
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
