package pw.caple.bolt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import pw.caple.bolt.applications.ApplicationManager;
import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.ext.ExtObjectContainer;
import com.db4o.query.Predicate;

public class Bolt {

	private static Server webServer;
	private static ExtObjectContainer db;
	private static ApplicationManager appManager;

	public static void shutdown() {
		try {
			appManager.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			webServer.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ObjectContainer getDB() {
		return db.ext().openSession();
	}

	@SuppressWarnings("serial")
	public static void main(String[] args) throws Exception {

		File dbFile = new File("bolt.db");
		if (dbFile.exists()) dbFile.delete();

		EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
		db = Db4oEmbedded.openFile(config, "bolt.db").ext();

		List<DBTest> results;
		Benchmark bm = new Benchmark();

		List<DBTest> tests = new ArrayList<DBTest>();
		for (int i = 0; i < 1000000; i++) {
			tests.add(new DBTest("Test" + i, i));
		}
		bm.lap("create objects (jvm)");

		ObjectContainer container = getDB();
		for (DBTest test : tests) {
			container.store(test);
		}
		bm.lap("insert");
		container.close();
		bm.lap("close sssion");

		container = getDB();
		results = db.query(new Predicate<DBTest>() {
			@Override
			public boolean match(DBTest obj) {
				return obj.id == 1500;
			}
		});
		container.close();
		bm.lap("native query");

		for (DBTest test : results) {
			System.out.println(test.name);
		}
		bm.lap("read results");

		// Configure connection thread pooling
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);
		webServer = new Server(threadPool);
		threadPool.setIdleTimeout(5000);
		threadPool.setStopTimeout(1);

		// Configure connectors
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(443);
		httpConfig.setOutputBufferSize(32768);

		// Allow HTTP connections
		ServerConnector http = new ServerConnector(webServer, new HttpConnectionFactory(httpConfig));
		http.setPort(80);
		http.setIdleTimeout(30000);
		http.setReuseAddress(false);
		webServer.addConnector(http);

		// Allow localhost HTTPS connections
		writeResourceToDisk("dev.localhost.cert");
		SslContextFactory factory = new SslContextFactory();
		factory.setKeyStorePath("resources/dev.localhost.cert");
		factory.setKeyStorePassword("boltdev");
		factory.setKeyManagerPassword("boltdev");
		httpConfig.addCustomizer(new SecureRequestCustomizer());
		ServerConnector https = new ServerConnector(webServer,
				new SslConnectionFactory(factory, "http/1.1"),
				new HttpConnectionFactory(httpConfig));
		https.setPort(443);
		https.setIdleTimeout(500000);
		https.setHost("localhost");
		webServer.addConnector(https);

		HandlerCollection serverHandler = new HandlerCollection(true);
		webServer.setHandler(serverHandler);

		writeResourceToDisk("bolt.js");
		writeResourceToDisk("console.html");
		addResourceHandler(serverHandler, "bolt.js", "/bolt");
		addResourceHandler(serverHandler, "console.html", "/bolt/console");

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

	private static void writeResourceToDisk(String resource) {
		File resourcesFolder = new File("resources/");
		resourcesFolder.mkdir();
		try (
			InputStream in = Bolt.class.getClassLoader().getResourceAsStream(resource);
			OutputStream out = new FileOutputStream(new File(resourcesFolder, resource));

		) {
			byte[] buffer = new byte[1024];
			int bytes = 0;
			while ((bytes = in.read(buffer)) >= 0) {
				out.write(buffer, 0, bytes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addResourceHandler(HandlerCollection handler, String resource, String url) {
		File resourcesFolder = new File("resources/");
		File resourceFile = new File(resourcesFolder, resource);
		ResourceHandler scriptResource = new ResourceHandler();
		scriptResource.setResourceBase(resourceFile.toString());
		ContextHandler context = new ContextHandler();
		context.setContextPath(url);
		context.setHandler(scriptResource);
		handler.addHandler(context);
	}

}
