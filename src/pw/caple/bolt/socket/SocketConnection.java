package pw.caple.bolt.socket;

import java.io.IOException;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WebSocketTimeoutException;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.api.Protocol;
import pw.caple.bolt.applications.ProtocolEngine;

public class SocketConnection implements WebSocketListener {

	private Client client;
	private Session session;
	private RemoteEndpoint remote;

	private final Application application;
	private final ProtocolEngine protocolEngine;

	SocketConnection(Application application, ProtocolEngine protocolScanner) {
		this.application = application;
		this.protocolEngine = protocolScanner;
	}

	public final void send(String message) {
		try {
			remote.sendString(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final void close(int code, String reason) {
		if (client != null) {
			client.onClose();
		}
		if (session.isOpen()) {
			try {
				session.close(code, reason);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Protocol
	static void PING(Client client) {
		client.send("PONG");
	}

	//	@Protocol
	//	private static void requestCallback(Client client, String callbackID, String... args) {
	//		StringBuilder builder = new StringBuilder();
	//		for (String arg : args) {
	//			builder.append(arg);
	//			builder.append(' ');
	//		}
	//		if (builder.length() > 0) {
	//			builder.setLength(builder.length() - 1);
	//		}
	//		Object returnValue = Bolt.getProtocolCore().processMessage(client, builder.toString());
	//		if (returnValue != null) {
	//			client.send("callback " + callbackID + " " + returnValue.toString());
	//		} else {
	//			client.send("callback " + callbackID);
	//		}
	//	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		send("binary not yet supported");
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		if (client != null) client.onClose();
	}

	@Override
	public void onWebSocketConnect(Session session) {
		this.session = session;
		remote = session.getRemote();
		client = application.initializeConnection(this);
		client.onOpen();
		if (!session.isSecure()) {
			try {
				session.close(1002, "only accepting secured connections");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		if (cause instanceof WebSocketTimeoutException) {
			try {
				session.close(1001, "client timed out");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			cause.printStackTrace();
		}
	}

	@Override
	public void onWebSocketText(String message) {
		protocolEngine.processMessage(client, message);
	}

}