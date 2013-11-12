package pw.caple.bolt.applications;

import pw.caple.bolt.Bolt;
import pw.caple.bolt.api.Client;

public class ReservedProtocol {

	ProtocolEngine protocolEngine;

	ReservedProtocol(ProtocolEngine protocolEngine) {
		this.protocolEngine = protocolEngine;
	}

	public void PING(Client client) {
		client.call("PONG");
	}

	public void cbAsk(Client client, int cbid, String message) {
		//		Object result = protocolEngine.process(client, message);
		//		if (result != null) {
		//			client.call("cbResult", cbid, result.toString());
		//		} else {
		//			client.call("cbResult", cbid);
		//		}
	}

	public void stop() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Bolt.shutdown();
			}
		}).start();
	}

}
