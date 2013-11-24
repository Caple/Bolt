package pw.caple.bolt.api;

import java.io.File;
import pw.caple.bolt.socket.GenericClient;

/**
 * The main class of a Bolt web application. There should only be one such class
 * found in a single application.
 */
public abstract class Application {

	protected Application() {}

	private boolean initialized = false;
	private File appPath;
	private String name;

	public final BoltConfig initialize(File appPath) {
		if (initialized) {
			throw new IllegalStateException("Application " + name + " already initialized.");
		}
		initialized = true;
		this.appPath = appPath;
		name = appPath.getName();
		BoltConfig config = new BoltConfig();
		configure(config);
		return config;
	}

	protected File getStartupPath() {
		return appPath;
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
	 * Called to configure the application.
	 */
	public abstract void configure(BoltConfig config);

	/**
	 * Called after the application is started.
	 */
	public abstract void onStartup();

	/**
	 * Called before the application is stopped.
	 */
	public abstract void onShutdown();

}
