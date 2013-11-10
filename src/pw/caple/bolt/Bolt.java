package pw.caple.bolt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import javax.tools.ToolProvider;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import pw.caple.bolt.api.Protocol;
import pw.caple.bolt.applications.ApplicationManager;
import com.orientechnologies.orient.object.db.OObjectDatabasePool;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

public class Bolt {

	private static Server webServer;
	private static OServer database;
	private static OObjectDatabasePool dbPool;
	private static ApplicationManager appManager;

	private final static String serverPath = new File(Bolt.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toString();
	private final static Properties config = new Properties();

	@Protocol
	private static void stop() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				shutdown();
			}
		}).start();
	}

	public static void shutdown() {
		try {
			appManager.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			database.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			webServer.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static OObjectDatabaseTx aquireDB(String url, String user, String password) {
		return dbPool.acquire(url, user, password);
	}

	public static String getServerPath() {
		return serverPath;
	}

	public static Properties getConfig() {
		return config;
	}

	public static void main(String[] args) throws Exception {

		// Check of Java compiler is available
		if (ToolProvider.getSystemJavaCompiler() == null) {
			System.err.println("CRITICAL ERROR: Java compiler is inaccessible!");
			System.err.println("Please ensure JAVA_HOME refers to the JDK instead of the JRE. -- Exiting.");
			return;
		}

		// Setup config file
		try {
			File configFile = new File("bolt.conf");
			if (!configFile.exists()) {
				Path from = Paths.get("dev/default.properties");
				Path to = Paths.get("bolt.conf");
				Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES);
			}
			InputStream stream = new FileInputStream(configFile);
			config.load(stream);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Setup and start object database server
		System.setProperty("ORIENTDB_HOME", "db/");
		database = OServerMain.create().startup(new File("dev/db.xml"));
		database.activate();
		dbPool = new OObjectDatabasePool();
		dbPool.setup(1, 50);
		System.out.println();

		// Configure connection thread pooling
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);
		webServer = new Server(threadPool);
		threadPool.setIdleTimeout(5000);
		threadPool.setStopTimeout(1);

		// Handle HTTP connections
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(443);
		httpConfig.setOutputBufferSize(32768);
		ServerConnector http = new ServerConnector(webServer, new HttpConnectionFactory(httpConfig));
		http.setPort(80);
		http.setIdleTimeout(30000);
		http.setReuseAddress(false);

		// Handle TLS HTTPS connections
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(getConfig().getProperty("ssl_keystore"));
		sslContextFactory.setKeyStorePassword(getConfig().getProperty("ssl_password"));
		sslContextFactory.setKeyManagerPassword(getConfig().getProperty("ssl_password"));
		HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());
		ServerConnector https = new ServerConnector(webServer,
				new SslConnectionFactory(sslContextFactory, "http/1.1"),
				new HttpConnectionFactory(httpsConfig));
		https.setPort(443);
		https.setIdleTimeout(500000);
		https.setReuseAddress(false);

		webServer.setConnectors(new Connector[] { http, https });
		webServer.setHandler(new HandlerCollection(true));

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});

		webServer.start();
		appManager = new ApplicationManager(webServer);
		appManager.reloadAll();
		webServer.join();
		shutdown();
	}

}
