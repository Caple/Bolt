package pw.caple.bolt.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import pw.caple.bolt.socket.SocketConnection;

/**
 * Represents a single connection using the Bolt protocol. An application can
 * extend this class and override {@link Application#getNewClient()} in order to
 * store additional data with each connection.
 */
public abstract class Client {

	private SocketConnection socket;

	private final AtomicLong callbackID = new AtomicLong();
	private final Map<Long, Callback> callbacks = new ConcurrentHashMap<>();

	/**
	 * Sends a protocol message to the client.
	 */
	public final void send(String message) {
		socket.send(message);
	}

	/**
	 * Sends a protocol message to the client and synchronously waits for the
	 * client to return a result.
	 */
	public final void send(String message, Callback callback) {
		long cbid = callbackID.incrementAndGet();
		callbacks.put(cbid, callback);
		socket.send("cbAsk " + callbackID + " " + message);
	}

	/**
	 * Called after the client connects.
	 */
	public abstract void onOpen();

	/**
	 * Called after the client disconnects.
	 */
	public abstract void onClose();

	public final void serverCallback(int cbid, String result) {
		if (!callbacks.containsKey(cbid)) return;
		Callback callback = callbacks.get(cbid);
		callbacks.remove(cbid);
		callback.run(result);
	}

	public final void setSocket(SocketConnection socket) {
		this.socket = socket;
	}

	/**
	 * Simple runnable with an argument
	 */
	public static interface Callback {
		public void run(String result);
	}

}
