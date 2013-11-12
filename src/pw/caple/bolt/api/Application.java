package pw.caple.bolt.api;

import pw.caple.bolt.Bolt;
import pw.caple.bolt.socket.GenericClient;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;

/**
 * The main class of a Bolt web application. There should only be one such class
 * found in a single application.
 */
public abstract class Application {

	protected Application() {}

	private String name;

	public final void initializeApplication(String name) {
		this.name = name;
	}

	/**
	 * The name of the application as determined by it's path.
	 */
	public String getName() {
		return name;
	}

	/**
	 * An application may override this method in order to create a custom
	 * client with any extra connection-bound data that it may need.
	 */
	public Client getNewClient() {
		return new GenericClient();
	}

	/**
	 * Returns the database object for this application.
	 */
	protected final OObjectDatabaseTx getDB() {
		return Bolt.aquireDB("local:db/" + name, "root", "root");
	}

	/**
	 * Called after the application is started.
	 */
	public abstract void onStartup();

	/**
	 * Called before the application is stopped.
	 */
	public abstract void onShutdown();

}
