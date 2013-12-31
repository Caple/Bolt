package pw.caple.bolt.applications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.FetchResult;
import pw.caple.bolt.Bolt;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.api.Protocol;
import pw.caple.bolt.api.Protocol.SecurityClearance;

public class Update {

	private Update() {}

	/*
	 * How this works...
	 * 
	 * V - User initiates update with command
	 * V -
	 * V - Check if files are up to date
	 * V - Download latest version of /update/ directory
	 * V - Compile update source files
	 * V - Shut down server
	 * V - Run update class in new JVM
	 * V - EXIT PROGRAM
	 * V -
	 * V - IN NEW PROCESS
	 * V - Download latest version of everything
	 * V - Recompile whole project
	 * V - Start server
	 * V - EXIT PROGRAM
	 * V -
	 * V - IN NEW PROCESS
	 * = - Done. Server starts up again
	 */

	public enum UpdateResult {
		AlreadyUpToDate,
		CompilationError,
		NoJavaCompilerFound,
		Success;
	}

	@Protocol(security = SecurityClearance.PUBLIC)
	private static String update(Client client) {
		final Update update = new Update();
		try {
			switch (update.fetchUpdate()) {
			case Success:
				client.send("log", "update running");
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Bolt.shutdown();
							update.exitAndUpdate();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
				;
				return "running update";
			case AlreadyUpToDate:
				return "server is already up to date";
			case CompilationError:
				return "update failed; compiler error";
			case NoJavaCompilerFound:
				return "update failed; use JDK instead of JRE";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "update failed; unexpected error";
		}
		return null;
	}

	private UpdateResult fetchUpdate() throws Exception {

		// check if update is even necessary
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.readEnvironment().findGitDir().build();
		Git git = new Git(repository);
		FetchResult fetch = git.fetch().call();
		Ref headLocal = repository.getRef(Constants.HEAD);
		Ref headRemote = fetch.getAdvertisedRef(Constants.HEAD);
		if (headRemote == null || headLocal.getObjectId().equals(headRemote.getObjectId())) {
			return UpdateResult.AlreadyUpToDate;
		}

		// download update files from remote repo
		git.checkout().addPath("update/").setStartPoint("origin/" + repository.getBranch()).call();

		// prep temporary bin directory
		File bin = new File("update/bin/");
		if (bin.exists()) deleteDirectory(bin);
		bin.mkdir();

		// prepare compiler information
		List<String> arguments = new ArrayList<>();
		arguments.add("-d");
		arguments.add(bin.toString());
		arguments.add("-cp");
		arguments.add(buildClassPath());
		arguments.addAll(getFiles("update/src"));
		String[] argsArray = arguments.toArray(new String[arguments.size()]);

		// compile updater source files to class files
		JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
		if (javac == null) {
			return UpdateResult.NoJavaCompilerFound;
		}
		int result = javac.run(null, System.out, System.err, argsArray);
		if (result != 0) {
			return UpdateResult.CompilationError;
		}

		// exitAndUpdate() should now be called
		return UpdateResult.Success;
	}

	private void exitAndUpdate() {
		String updateClass = getUpdateClass();
		String classPath = buildClassPath() + System.getProperty("path.separator") + "update/bin/";
		String java = "\"" + System.getProperty("java.home") + "/bin/java" + "\"";
		String command = java + " -cp " + classPath + " " + updateClass;
		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Runtime.getRuntime().exit(0);
	}

	private String buildClassPath() {
		String seperator = System.getProperty("path.separator");
		StringBuilder paths = new StringBuilder();
		for (String file : getFiles("update/lib/")) {
			paths.append(file);
			paths.append(seperator);
		}
		if (paths.length() > 0) paths.setLength(paths.length() - 1);
		return paths.toString();
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

	private String getUpdateClass() {
		String value = null;
		try (
			FileReader reader = new FileReader("update/update_class");
			BufferedReader buffer = new BufferedReader(reader)

		) {
			value = buffer.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return value;
	}

}
