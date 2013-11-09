package pw.caple.bolt.api;

import pw.caple.bolt.Core;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;

public abstract class Application {

	private String name;

	protected String getName() {
		return name;
	}

	protected Application() {}

	protected final OObjectDatabaseTx getDB() {
		return Core.aquireDB("local:db/" + name, "root", "root");
	}

	public abstract void onStartup();

	public abstract void onShutdown();

	public abstract void onClientConnect(Client client);

	public abstract void onClientDisconnect(Client client);

}
