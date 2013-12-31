package pw.caple.bolt.socket;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import pw.caple.bolt.api.Callback;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.applications.ProtocolEngine;
import pw.caple.bolt.socket.ProtocolMessage.Mode;

/**
 * Stands between the socket connection code and the application. Allows for
 * synchronized method calls from server to client and client to server.
 */
public class ProtocolIntermediate implements Closeable {

	private final ProtocolEngine engine;
	private final Client client;
	private final Thread thread;
	private final ThreadCode code;
	private final RemoteEndpoint remote;
	private final BlockingQueue<ProtocolMessage> queue = new LinkedTransferQueue<>();

	private final AtomicLong messageID = new AtomicLong();
	private final Map<Long, Callback> callbacks = new ConcurrentHashMap<>();

	private final AtomicLong blockedOnMessageID = new AtomicLong(-1);
	private volatile String blockResult;

	ProtocolIntermediate(RemoteEndpoint remote, Client client, ProtocolEngine engine) {
		this.remote = remote;
		this.client = client;
		this.engine = engine;
		code = new ThreadCode();
		thread = new Thread(code);
		thread.start();
	}

	public void processIncoming(String message) {
		ProtocolMessage msg = new ProtocolMessage(message);
		switch (msg.getMode()) {
		case CALL:
			queue.add(msg);
			break;
		case RETURN:
			String[] args = msg.getArgs();
			long id = Long.parseLong(args[0]);
			String result = args.length > 1 ? args[1] : null;
			if (blockedOnMessageID.get() == id) {
				blockResult = result;
				synchronized (thread) {
					thread.notifyAll();
				}
			} else {
				if (callbacks.containsKey(id)) {
					Callback callback = callbacks.get(id);
					callbacks.remove(id);
					callback.run(result);
				}
			}
			break;
		case DIE:
			sendAsynchronousCall("error", "die mode is forbidden");
			return;
		case READY:
			break;
		}
	}

	public String sendBlockingCall(int timeout, String method, Object... args) {
		String[] stringArgs = new String[args.length + 1];
		stringArgs[0] = method;
		for (int i = 0; i < args.length; i++) {
			stringArgs[i + 1] = args[i].toString();
		}
		long id = messageID.incrementAndGet();
		blockedOnMessageID.set(id);
		ProtocolMessage message = new ProtocolMessage(id, Mode.CALL, stringArgs);
		try {
			try {
				remote.sendString(message.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			synchronized (thread) {
				thread.wait(timeout);
			}
			return blockResult;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void sendAsynchronousCall(String method, Object... args) {
		String[] stringArgs = new String[args.length + 1];
		stringArgs[0] = method;
		Callback callback = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof Callback) {
				callback = (Callback) args[i];
			} else {
				stringArgs[i + 1] = args[i].toString();
			}
		}
		long id = messageID.incrementAndGet();
		ProtocolMessage message = new ProtocolMessage(id, Mode.CALL, stringArgs);
		if (callback != null) {
			callbacks.put(id, callback);
		}
		remote.sendStringByFuture(message.toString());
	}

	@Override
	public void close() {
		try {
			queue.add(ProtocolMessage.getPoison());
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class ThreadCode extends Thread {

		@Override
		public void run() {
			boolean running = true;
			try {
				while (running) {
					ProtocolMessage message = queue.take();
					if (message.getMode() == ProtocolMessage.Mode.DIE) {
						running = false;
						break;
					}
					String[] args = message.getArgs();
					String functionName = args[0];
					String[] functionArgs = new String[args.length - 1];
					System.arraycopy(args, 1, functionArgs, 0, args.length - 1);
					Object result = engine.callMethod(functionName, functionArgs, client);

					// send callback
					long id = messageID.incrementAndGet();
					String[] returnArray;
					if (result != null) {
						returnArray = new String[] { Long.toString(message.getId()), result.toString() };
					} else {
						returnArray = new String[] { Long.toString(message.getId()) };
					}
					ProtocolMessage returnMessage = new ProtocolMessage(id, Mode.RETURN, returnArray);
					remote.sendStringByFuture(returnMessage.toString());
				}
			} catch (InterruptedException e) {
				return;
			}
		}

	}

}
