package pw.caple.bolt.socket;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.applications.ProtocolEngine;

public class SocketServlet extends WebSocketServlet {

	private static final long serialVersionUID = 5L;

	private final Application application;
	private final ProtocolEngine protocolEngine;

	public SocketServlet(Application application, ProtocolEngine protocolEngine) {
		this.application = application;
		this.protocolEngine = protocolEngine;
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setIdleTimeout(120000);
		factory.setCreator(new SocketCreator(application, protocolEngine));
	}
}
