package pw.caple.bolt.socket;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.applications.ProtocolEngine;

/**
 * Stands between the socket connection code and the application. Allows for
 * synchronized method calls from server to client and client to server.
 */
public class ProtocolIntermediate implements Closeable {

	private final static String MODE_BLOCKING = "BLOCK";
	private final static String MODE_ASYNCHRONOUS = "ASYNC";

	private final static String DIE_MESSAGE = "THREAD_DIE";
	private final static String UNBLOCK_MESSAGE = "ASYNC*UNBLOCK!";

	private final Thread thread;
	private final ThreadCode code;
	private final RemoteEndpoint remote;
	private final BlockingQueue<String> queue;
	private volatile String callbackResult;

	ProtocolIntermediate(RemoteEndpoint remote, Client client, ProtocolEngine engine) {
		this.remote = remote;
		code = new ThreadCode(client, engine);
		queue = new LinkedTransferQueue<String>();
		thread = new Thread(code);
		thread.start();
	}

	public void processIncoming(String message) {
		if (message.equals(DIE_MESSAGE)) {
			return; // client shouldn't be sending this...
		} else if (message.startsWith(UNBLOCK_MESSAGE)) {
			callbackResult = null;
			if (message.length() > UNBLOCK_MESSAGE.length()) {
				callbackResult = message.substring(UNBLOCK_MESSAGE.length());
			}
			synchronized (thread) {
				thread.notifyAll();
			}
		} else {
			queue.add(message);
		}
	}

	public String sendBlockingMessage(int timeout, String method, Object... args) {
		try {
			String header = buildHeader(MODE_BLOCKING, method);
			String message = buildMessage(header, args);
			try {
				remote.sendString(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
			synchronized (thread) {
				thread.wait(timeout);
			}
			return callbackResult;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void sendAsynchronousMessage(String method, Object... args) {
		String header = buildHeader(MODE_ASYNCHRONOUS, method);
		String message = buildMessage(header, args);
		remote.sendStringByFuture(message);
	}

	private String buildHeader(String mode, String method) {
		final StringBuilder builder = new StringBuilder(mode);
		builder.append('*');
		builder.append(method);
		builder.append('!');
		return builder.toString();
	}

	private String buildMessage(String header, Object[] args) {
		final StringBuilder builder = new StringBuilder(header);
		for (Object arg : args) {
			builder.append(arg.toString().replace("*", "&#42;"));
			builder.append('*');
		}
		if (args.length > 0) builder.setLength(builder.length() - 1);
		return builder.toString();
	}

	@Override
	public void close() {
		try {
			queue.add(DIE_MESSAGE);
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class ThreadCode extends Thread {

		private final ProtocolEngine engine;
		private final Client client;

		ThreadCode(Client client, ProtocolEngine engine) {
			this.client = client;
			this.engine = engine;
		}

		@Override
		public void run() {
			boolean running = true;
			try {
				while (running) {
					String message = queue.take();
					if (message.equals(DIE_MESSAGE)) {
						running = false;
					} else {
						process(message);
					}
				}
			} catch (InterruptedException e) {
				return;
			}
		}

		private void process(String message) {
			int headerLength = message.indexOf("!");
			String[] header = message.substring(0, headerLength).split("\\*");

			String mode = header[0];
			String command = header[1];
			String[] args = new String[0];

			if (message.length() > headerLength + 1) {
				args = message.substring(headerLength + 1).split("\\*");
			}

			Object result = engine.runCommand(client, command, args);
			if (mode == MODE_BLOCKING) {
				//TODO: client blocking
			}
		}

	}

}
