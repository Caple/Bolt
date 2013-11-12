package pw.caple.bolt.socket;

import java.io.IOException;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WebSocketTimeoutException;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.applications.ProtocolEngine;

public class SocketConnection implements WebSocketListener {

	private Client client;
	private Session session;
	private RemoteEndpoint remote;

	private final Application application;
	private final ProtocolEngine protocolEngine;
	private ProtocolIntermediate protocolIntermediate;

	SocketConnection(Application application, ProtocolEngine protocolEngine) {
		this.application = application;
		this.protocolEngine = protocolEngine;
	}

	public final void send(String message) {
		try {
			remote.sendString(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final void close(int code, String reason) {
		if (protocolIntermediate != null) {
			protocolIntermediate.close();
		}
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

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		send("binary not yet supported");
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		close(0, "closing");
	}

	@Override
	public void onWebSocketConnect(Session session) {
		this.session = session;
		remote = session.getRemote();
		client = application.getNewClient();
		protocolIntermediate = new ProtocolIntermediate(remote, client, protocolEngine);
		client.setIntermediate(protocolIntermediate);
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
		protocolIntermediate.processIncoming(message);
	}

}