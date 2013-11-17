package pw.caple.bolt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.tools.ToolProvider;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
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

	public static void main(String[] args) throws Exception {

		// Check of Java compiler is available
		if (ToolProvider.getSystemJavaCompiler() == null) {
			System.err.println("CRITICAL ERROR: Java compiler is inaccessible!");
			System.err.println("Please ensure JAVA_HOME refers to the JDK instead of the JRE. -- Exiting.");
			return;
		}

		String oServerConfig = "" +
				"<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
				"<orient-server>" +
				"	<network>" +
				"		<protocols/>" +
				"		<listeners/>" +
				"	</network>" +
				"	<users>" +
				"		<user resources='*' password='7FC0855F8EDFA26915C22CE53885DA464361F4CA82D8BC4253488749D35C651E' name='root'/>" +
				"		<user resources='connect,server.listDatabases,server.dblist' password='guest' name='guest'/>" +
				"	</users>" +
				"	<properties/>" +
				"</orient-server>";

		// Setup and start object database server
		System.setProperty("ORIENTDB_HOME", "db/");
		database = OServerMain.create().startup(oServerConfig);
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

		HandlerCollection serverHandler = new HandlerCollection(true);
		webServer.addConnector(http);
		webServer.setHandler(serverHandler);

		addResource(serverHandler, "bolt.js", "/bolt");
		addResource(serverHandler, "console.html", "/bolt/console");

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

	private static void addResource(HandlerCollection handler, String resource, String url) {
		File resourcesFolder = new File("resources/");
		resourcesFolder.mkdir();
		File resourceFile = new File(resourcesFolder, resource);
		try (
			InputStream in = Bolt.class.getClassLoader().getResourceAsStream(resource);
			OutputStream out = new FileOutputStream(new File(resourcesFolder, resource));

		) {
			byte[] buffer = new byte[1024];
			int bytes = 0;
			while ((bytes = in.read(buffer)) >= 0) {
				out.write(buffer, 0, bytes);
			}
			ResourceHandler scriptResource = new ResourceHandler();
			scriptResource.setResourceBase(resourceFile.toString());
			ContextHandler context = new ContextHandler();
			context.setContextPath(url);
			context.setHandler(scriptResource);
			handler.addHandler(context);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
