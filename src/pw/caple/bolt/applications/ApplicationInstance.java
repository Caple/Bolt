package pw.caple.bolt.applications;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.Servlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import pw.caple.bolt.api.Application;
import pw.caple.bolt.applications.ApplicationXML.Map.Content;
import pw.caple.bolt.applications.ApplicationXML.Map.Socket;
import pw.caple.bolt.socket.SocketServlet;

public class ApplicationInstance {

	private final File root;
	private final String name;
	private final Server server;

	private ApplicationXML xml;
	private Application application;
	private URLClassLoader classLoader;
	private HandlerCollection handler;
	private ProtocolScanner protocolScanner;

	//TODO: make protocol registry work from inside a jar
	//TODO: ssl/tls per app
	//TODO: update per app
	//TODO: import/export xml for apps
	//TODO: make sure I want to stick with OrientDB
	//TODO: local net-address binding?

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
		readXMLConfig();
		loadClasses();
		loadJavaApplication();
		loadIntoWebServer();
		if (application != null) {
			application.onStartup();
		}
	}

	public void shutdown() {
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
		try {
			if (classLoader != null) {
				classLoader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readXMLConfig() throws AppLoadException {
		try {
			File appXMLFile = new File(root, "config.xml");
			if (!appXMLFile.exists()) {
				throw new AppLoadException("missing config.xml in " + root.toString());
			}
			JAXBContext jaxbContext = JAXBContext.newInstance(ApplicationXML.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			xml = (ApplicationXML) jaxbUnmarshaller.unmarshal(appXMLFile);
			if (!xml.name.equals(name)) {
				throw new AppLoadException("Application name vs directory mis-match for " + xml.name);
			}
		} catch (JAXBException e) {
			throw new AppLoadException("malformed config.xml in " + root.toString(), e);
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

	private void loadJavaApplication() throws AppLoadException {
		try {
			if (classLoader.getURLs().length == 0) return;
			if (xml.mainClass == null) {
				Logger.getGlobal().warning("No main class defined for " + name);
				return;
			}
			Class<?> mainClass = classLoader.loadClass(xml.mainClass);
			if (mainClass == null) {
				classLoader.close();
				throw new AppLoadException("Main class not found for " + xml.name);
			} else if (!Application.class.isAssignableFrom(mainClass)) {
				classLoader.close();
				throw new AppLoadException("Invalid main class for " + xml.name);
			}
			application = (Application) mainClass.newInstance();
			application.initializeApplication(name);
			protocolScanner = new ProtocolScanner(root, classLoader);
		} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new AppLoadException("error initializing main class for " + name, e);
		}
	}

	private void loadIntoWebServer() throws AppLoadException {

		HandlerCollection serverHandler = (HandlerCollection) server.getHandler();

		handler = new HandlerCollection();
		String[] virtualHosts = xml.bindings.toArray(new String[xml.bindings.size()]);
		String[] indexFiles = new String[] { "index.html", "index.php" };

		if (xml.map.content != null) {
			for (Content content : xml.map.content) {
				if (content.folder == null) {
					throw new AppLoadException("invalid content definition in config.xml of " + name);
				}
				File contentDirectory = new File(root, content.folder);
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
		}

		if (xml.map.servlet != null) {
			ServletContextHandler servlets = new ServletContextHandler();
			servlets.setClassLoader(classLoader);
			if (virtualHosts.length > 0) servlets.setVirtualHosts(virtualHosts);
			for (ApplicationXML.Map.Servlet servletInfo : xml.map.servlet) {
				if (servletInfo.className == null || servletInfo.url == null) {
					throw new AppLoadException("invalid servlet definition in config.xml of " + name);
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

		if (xml.map.socket != null) {
			ServletContextHandler sockets = new ServletContextHandler();
			sockets.setClassLoader(classLoader);
			if (virtualHosts.length > 0) sockets.setVirtualHosts(virtualHosts);
			for (Socket socketInfo : xml.map.socket) {
				if (socketInfo.url == null) {
					throw new AppLoadException("invalid socket definition in config.xml of " + name);
				}
				ServletHolder holder = new ServletHolder(new SocketServlet(application, protocolScanner));
				sockets.addServlet(holder, socketInfo.url);
			}
			handler.addHandler(sockets);
		}

		serverHandler.addHandler(handler);
	}
}
