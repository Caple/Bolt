package pw.caple.bolt.socket;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.applications.ProtocolEngine;

public class SocketCreator implements WebSocketCreator {

	private final SocketConnection socket;

	SocketCreator(Application application, ProtocolEngine protocolEngine, boolean forceWSS) {
		socket = new SocketConnection(application, protocolEngine, forceWSS);
	}

	@Override
	public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
		return socket;
	}

}
