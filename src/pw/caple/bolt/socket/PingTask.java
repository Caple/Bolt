package pw.caple.bolt.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

class PingTask implements Runnable {

	private final RemoteEndpoint remote;
	private final ByteBuffer buffer;

	PingTask(RemoteEndpoint remote) {
		this.remote = remote;
		buffer = ByteBuffer.wrap("!".getBytes());
	}

	@Override
	public void run() {
		try {
			remote.sendPing(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}