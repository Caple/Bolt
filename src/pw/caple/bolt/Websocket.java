package pw.caple.bolt;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class Websocket extends WebSocketServlet {

	private static final long serialVersionUID = -6671992407911218933L;

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setIdleTimeout(120000);
		factory.register(Connection.class);
	}
}
