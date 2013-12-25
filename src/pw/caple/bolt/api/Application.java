package pw.caple.bolt.api;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import pw.caple.bolt.socket.GenericClient;

/**
 * The main class of a Bolt web application. There should only be one such class
 * found in a single application.
 */
public abstract class Application {

	protected Application() {}

	private File appPath;
	private String name;

	public Application(File appPath) {
		this.appPath = appPath;
		name = appPath.getName();
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
	 * Retrieves a connection from this application's database pool. If no
	 * database information was provided during configuration, the current
	 * database configuration is invalid, or the connection pool is closed, then
	 * this method will throw a SQLException.
	 * 
	 * @throws SQLException
	 */
	protected Connection getDatabase() throws SQLException {
		//TODO complete database connection method
		throw new SQLException("getDatabase() method is not finished. Sorry.");
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
