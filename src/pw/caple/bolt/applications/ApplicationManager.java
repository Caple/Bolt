package pw.caple.bolt.applications;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.FetchResult;
import pw.caple.bolt.applications.ApplicationInstance.AppLoadException;

public class ApplicationManager {

	private final Server webServer;
	private final Map<String, ApplicationInstance> apps = new ConcurrentHashMap<>();

	public ApplicationManager(Server webServer) {
		this.webServer = webServer;
	}

	public void shutdown() {
		for (ApplicationInstance app : apps.values()) {
			app.shutdown();
		}
		apps.clear();
	}

	public void reloadAll() {
		for (ApplicationInstance app : apps.values()) {
			app.shutdown();
		}
		apps.clear();
		File appFolder = new File("apps");
		if (!appFolder.exists()) appFolder.mkdir();
		String[] appSubFolders = appFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}
		});
		for (String dir : appSubFolders) {
			File appRoot = new File("apps/" + dir);
			ApplicationInstance app = new ApplicationInstance(webServer, appRoot);
			try {
				app.load();
				apps.put(dir, app);
			} catch (AppLoadException e) {
				app.shutdown();
				e.printStackTrace();
			}
		}
		if (new File("bolt.xml").exists()) {
			// load application in server root.
			// this logic exists to allow easy debugging of new projects
			try {
				File root = new File(".").getCanonicalFile();
				ApplicationInstance app = new ApplicationInstance(webServer, root);
				try {
					app.load();
					apps.put(root.getName(), app);
				} catch (AppLoadException e) {
					app.shutdown();
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void importFromGit(URL url, String branch, String username, String password) {
		//		try {
		//
		//			if (branch == null) branch = "master";
		//			File tmpFolder = new File("tmp/clone/");
		//
		//			// download config.xml from specified git repository
		//			CloneCommand clone = Git.cloneRepository().setDirectory(tmpFolder).setURI(url.toString()).setBranch(branch);
		//			if (username != null && password != null) {
		//				CredentialsProvider provider;
		//				provider = new UsernamePasswordCredentialsProvider(username, password);
		//				clone = clone.setCredentialsProvider(provider);
		//			}
		//			Git git = clone.setNoCheckout(true).call();
		//			git.checkout().addPath("config.xml").call();
		//
		//			File appXMLFile = new File("tmp/clone/config.xml");
		//			ApplicationXML xml = parseXML(appXMLFile);
		//
		//			// clean up
		//			appXMLFile.delete();
		//			tmpFolder.delete();
		//
		//			// download the rest of the files from the repository
		//			File importFolder = new File("apps/" + xml.name);
		//			if (importFolder.exists()) {
		//				throw new RuntimeException("Import failed; application with same name already exists.");
		//			}
		//			importFolder.mkdir();
		//			clone = clone.setNoCheckout(false).setDirectory(importFolder);
		//			clone.call();
		//
		//			compileSource(xml.name);
		//
		//		} catch (GitAPIException e) {
		//			throw new RuntimeException("Import failed; Git error: " + e.getMessage());
		//		}
	}

	private void update(String name) {
		try {

			// prepare
			File root = new File("apps/" + name);
			File gitFolder = new File(root, ".git");
			if (!gitFolder.exists()) {
				throw new RuntimeException(name + " does not support git updating.");
			}

			// connect to remote repository and check if any new commits exist
			Git git = Git.open(root);
			FetchResult fetch = git.fetch().call();
			Ref headLocal = git.getRepository().getRef(Constants.HEAD);
			Ref headRemote = fetch.getAdvertisedRef(Constants.HEAD);
			if (headRemote == null || headLocal.getObjectId().equals(headRemote.getObjectId())) {
				throw new RuntimeException(name + " is already up to date.");
			}

			// wipe any outstanding changes to tracked files and pull from remote repository
			if (!git.status().call().isClean()) {
				git.reset().setMode(ResetType.HARD).call();
			}
			git.pull().call();

			// compile
			compileSource(name);

		} catch (GitAPIException | IOException e) {
			throw new RuntimeException("Update failed; Git error: " + e.getMessage());
		}
	}

	private void compileSource(String name) {
		try {

			// prepare
			File root = new File("apps/" + name);
			File binFolder = new File(root, "bin");
			File srcFolder = new File(root, "scr");
			if (binFolder.exists()) deleteDirectory(binFolder);
			binFolder.mkdir();
			srcFolder.mkdir();

			List<String> arguments = new ArrayList<>();

			// set target directory
			arguments.add("-d");
			arguments.add(binFolder.getCanonicalPath());

			// add class path if necessary
			File libFolder = new File(root, "lib");
			if (libFolder.exists()) {
				arguments.add("-cp");
				String seperator = System.getProperty("path.separator");
				StringBuilder paths = new StringBuilder();
				for (String file : getFiles("update/lib/")) {
					paths.append(file);
					paths.append(seperator);
				}
				if (paths.length() > 0) paths.setLength(paths.length() - 1);
				arguments.add(paths.toString());
			}

			// add all source files to be compiled
			arguments.addAll(getFiles("src/"));

			// compile!
			String[] argsArray = arguments.toArray(new String[arguments.size()]);
			JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
			int result = javac.run(null, System.out, System.err, argsArray);
			if (result != 0) {
				throw new RuntimeException("Build failed for " + name);
			}

		} catch (IOException e) {
			throw new RuntimeException("Compile failed; IO error: " + e.getMessage());
		}
	}

	// START HELPER METHODS

	private void deleteDirectory(File file) {
		Path path = Paths.get(file.getAbsolutePath());
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					if (e == null) Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> getFiles(String path) {
		final List<String> files = new ArrayList<>();
		try {
			Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					files.add(file.toString());
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return files;
	}

}
