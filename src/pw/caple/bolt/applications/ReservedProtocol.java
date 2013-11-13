package pw.caple.bolt.applications;

import pw.caple.bolt.Bolt;

public class ReservedProtocol {

	ProtocolEngine protocolEngine;

	ReservedProtocol(ProtocolEngine protocolEngine) {
		this.protocolEngine = protocolEngine;
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
