package pw.caple.bolt.applications;

import pw.caple.bolt.Bolt;
import pw.caple.bolt.api.Client;

public class ReservedProtocol {

	ProtocolEngine protocolEngine;

	ReservedProtocol(ProtocolEngine protocolEngine) {
		this.protocolEngine = protocolEngine;
	}

	public void PING(Client client) {
		client.send("PONG");
	}

	public void cbAsk(Client client, int cbid, String message) {
		Object result = protocolEngine.processMessage(client, message);
		if (result != null) {
			client.send("cbResult " + cbid + " " + result.toString());
		} else {
			client.send("cbResult " + cbid);
		}
	}

	public void cbResult(Client client, int cbid) {
		cbResult(client, cbid, null);
	}

	public void cbResult(Client client, int cbid, String result) {
		client.serverCallback(cbid, result);
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
