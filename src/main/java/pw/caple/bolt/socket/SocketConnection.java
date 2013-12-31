package pw.caple.bolt.socket;

import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WebSocketTimeoutException;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.applications.ProtocolEngine;

@WebSocket
public class SocketConnection implements WebSocketListener {

	private Client client;
	private Session session;
	private RemoteEndpoint remote;

	private final boolean forceWSS;
	private final Application application;
	private final ProtocolEngine protocolEngine;
	private ProtocolIntermediate protocolIntermediate;
	private ScheduledThreadPoolExecutor pingExecutor;

	SocketConnection(Application application, ProtocolEngine protocolEngine, boolean forceWSS) {
		this.application = application;
		this.protocolEngine = protocolEngine;
		this.forceWSS = forceWSS;
	}

	public final void close(int code, String reason) {
		if (pingExecutor != null) {
			pingExecutor.shutdown();
		}
		if (protocolIntermediate != null) {
			protocolIntermediate.close();
		}
		if (client != null) {
			client.onClose();
		}
		if (session.isOpen()) {
			session.close(code, reason);
		}
	}

	@Override
	public void onWebSocketText(String message) {
		protocolIntermediate.processIncoming(message);
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {

	}

	@Override
	public void onWebSocketConnect(Session session) {
		this.session = session;
		if (forceWSS && !session.isSecure()) {
			session.close(4040, "only accepting secured connections");
		} else {
			remote = session.getRemote();
			client = application.getNewClient();
			pingExecutor = new ScheduledThreadPoolExecutor(1);
			pingExecutor.scheduleAtFixedRate(new PingTask(remote), 0, 60, TimeUnit.SECONDS);
			protocolIntermediate = new ProtocolIntermediate(remote, client, protocolEngine);
			try {
				remote.sendString("0*READY!");
			} catch (IOException e) {
				e.printStackTrace();
			}
			client.init(application, protocolIntermediate);
			client.onOpen();
		}
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		close(1000, "closing");
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		if (cause instanceof WebSocketTimeoutException) {
			session.close(1002, "client timed out");
		} else {
			cause.printStackTrace();
		}
	}

}