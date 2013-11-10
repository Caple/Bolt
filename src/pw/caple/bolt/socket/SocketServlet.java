package pw.caple.bolt.socket;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.applications.ProtocolScanner;

public class SocketServlet extends WebSocketServlet {

	private static final long serialVersionUID = 5L;

	private final Application application;
	private final ProtocolScanner protocolScanner;

	public SocketServlet(Application application, ProtocolScanner protocolScanner) {
		this.application = application;
		this.protocolScanner = protocolScanner;
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setIdleTimeout(120000);
		factory.setCreator(new SocketCreator(application, protocolScanner));
	}
}
