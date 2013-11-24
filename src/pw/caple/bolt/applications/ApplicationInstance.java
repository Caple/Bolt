package pw.caple.bolt.applications;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Servlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.api.BoltConfig;
import pw.caple.bolt.api.BoltConfig.Content;
import pw.caple.bolt.api.BoltConfig.SSLCert;
import pw.caple.bolt.socket.SocketServlet;

public class ApplicationInstance {

	private final File root;
	private final String name;
	private final Server server;
	private final List<Connector> connectors = new ArrayList<>();

	private Application application;
	private URLClassLoader classLoader;
	private HandlerCollection handler;
	private ProtocolEngine protocolEngine;
	private BoltConfig config;

	//TODO: update/import command + console
	//TODO: make sure I want to stick with OrientDB

	public static class AppLoadException extends Exception {

		private static final long serialVersionUID = -1680767505958197273L;

		public AppLoadException(String message) {
			super(message);
		}

		public AppLoadException(String message, Exception e) {
			super(message, e);
		}

	}

	public ApplicationInstance(Server server, File root) {
		this.server = server;
		this.root = root;
		name = root.getName();
	}

	public void load() throws AppLoadException {
		Log.getLogger(ApplicationInstance.class).info("Loading " + name + "...");
		loadClasses();
		loadApplicationClass();
		loadIntoWebServer();
		if (application != null) {
			application.onStartup();
		}
		Log.getLogger(ApplicationInstance.class).debug("Finished loading " + name);
	}

	public void shutdown() {
		Log.getLog().info("Shutting down " + name + "...");
		try {
			if (application != null) {
				application.onShutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (handler != null) {
				handler.stop();
				HandlerCollection serverHandlers = (HandlerCollection) server.getHandler();
				serverHandlers.removeHandler(handler);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Connector connector : connectors) {
			try {
				connector.stop();
				server.removeConnector(connector);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			if (classLoader != null) {
				classLoader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadClasses() throws AppLoadException {
		try {
			File binFolder = new File(root, "bin");
			File libFolder = new File(root, "lib");
			final List<URL> itemsToLoad = new ArrayList<>();
			if (libFolder.exists()) {
				try {
					Files.walkFileTree(libFolder.toPath(), new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (file.toString().endsWith(".jar")) {
								itemsToLoad.add(file.toUri().toURL());
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (binFolder.exists()) {
				itemsToLoad.add(binFolder.toURI().toURL());
			}
			if (itemsToLoad.size() > 0) {
				URL[] loadArray = itemsToLoad.toArray(new URL[itemsToLoad.size()]);
				classLoader = new URLClassLoader(loadArray);
			} else {
				classLoader = new URLClassLoader(new URL[] {});
			}
		} catch (MalformedURLException e) {
			throw new AppLoadException("error loading class file or dependency for " + name, e);
		}
	}

	private void loadApplicationClass() throws AppLoadException {
		try {
			if (classLoader.getURLs().length == 0) return;
			ClassScanner reflection = new ClassScanner(classLoader);
			List<Class<?>> classes = reflection.getSubclassesOf(Application.class, classLoader);
			if (classes.size() < 1) {
				throw new AppLoadException(name + " does not define an Application class");
			} else if (classes.size() > 1) {
				throw new AppLoadException(name + " defines multiple Application classes. Only one is allowed per application.");
			}
			application = (Application) classes.get(0).newInstance();
			config = application.initialize(root);
			protocolEngine = new ProtocolEngine(classLoader);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new AppLoadException("error initializing main class for " + name, e);
		}
	}

	private void loadIntoWebServer() throws AppLoadException {

		HandlerCollection serverHandler = (HandlerCollection) server.getHandler();

		handler = new HandlerCollection();
		String[] virtualHosts = config.getDomains().toArray(new String[config.getDomains().size()]);
		String[] indexFiles = new String[] { "index.html", "index.php", "index.jsp" };

		config.getCertificates();

		for (SSLCert ssl : config.getCertificates()) {
			SslContextFactory factory = new SslContextFactory();
			File keystoreFile = new File(root, ssl.keystore);
			File passwordFile = new File(root, ssl.password);
			if (!keystoreFile.exists()) {
				Log.getLogger(ApplicationInstance.class).warn("=======================================");
				Log.getLogger(ApplicationInstance.class).warn("SSL certificate error. Could not find keystore " + keystoreFile.toString() + ".");
				Log.getLogger(ApplicationInstance.class).warn("=======================================");
				break;
			}
			if (!passwordFile.exists()) {
				Log.getLogger(ApplicationInstance.class).warn("=======================================");
				Log.getLogger(ApplicationInstance.class).warn("SSL certificate error. Could not find password file " + passwordFile.toString() + ".");
				Log.getLogger(ApplicationInstance.class).warn("=======================================");
				break;
			}
			String password = null;
			try {
				byte[] bytes = Files.readAllBytes(passwordFile.toPath());
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				password = Charset.defaultCharset().decode(buffer).toString();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			factory.setKeyStorePath(keystoreFile.toString());
			factory.setKeyStorePassword(password);
			factory.setKeyManagerPassword(password);

			HttpConfiguration config = new HttpConfiguration();
			config.setSecureScheme("https");
			config.setSecurePort(443);
			config.setOutputBufferSize(32768);
			config.addCustomizer(new SecureRequestCustomizer());
			ServerConnector https = new ServerConnector(server,
					new SslConnectionFactory(factory, "http/1.1"),
					new HttpConnectionFactory(config));
			https.setPort(443);
			https.setIdleTimeout(500000);
			if (ssl.ip != null) {
				https.setHost(ssl.ip);
			}
			server.addConnector(https);
			connectors.add(https);
			try {
				https.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (Content content : config.getContent()) {
			if (content.folder == null) {
				throw new AppLoadException("invalid content definition in bolt.xml of " + name);
			}
			File contentDirectory = new File(content.folder);
			if (!contentDirectory.exists()) {
				throw new AppLoadException("content folder missing for" + name);
			}
			ResourceHandler resource = new ResourceHandler();
			resource.setWelcomeFiles(indexFiles);
			resource.setResourceBase(new File(root, content.folder).toString());
			ContextHandler context = new ContextHandler();
			context.setClassLoader(classLoader);
			if (virtualHosts.length > 0) context.setVirtualHosts(virtualHosts);
			if (content.url != null) context.setContextPath(content.url);
			context.setHandler(resource);
			handler.addHandler(context);
		}

		if (config.getServlets().size() > 0) {
			ServletContextHandler servlets = new ServletContextHandler();
			servlets.setClassLoader(classLoader);
			if (virtualHosts.length > 0) servlets.setVirtualHosts(virtualHosts);
			for (pw.caple.bolt.api.BoltConfig.Servlet servletInfo : config.getServlets()) {
				if (servletInfo.className == null || servletInfo.url == null) {
					throw new AppLoadException("invalid servlet definition in bolt.xml of " + name);
				}
				try {
					Class<?> clazz = classLoader.loadClass(servletInfo.className);
					if (!Servlet.class.isAssignableFrom(clazz)) {
						throw new AppLoadException("defined class is not a servlet - " + servletInfo.className);
					}
				} catch (ClassNotFoundException e) {
					throw new AppLoadException("missing servlet " + servletInfo.className, e);
				}
				servlets.addServlet(servletInfo.className, servletInfo.url);
			}
			handler.addHandler(servlets);
		}

		ServletContextHandler socket = new ServletContextHandler();
		socket.setClassLoader(classLoader);
		if (virtualHosts.length > 0) socket.setVirtualHosts(virtualHosts);
		ServletHolder holder = new ServletHolder(new SocketServlet(application, protocolEngine, config.getForcedWSS()));
		socket.addServlet(holder, "/bolt/socket");
		handler.addHandler(socket);

		serverHandler.addHandler(handler);
	}
}
