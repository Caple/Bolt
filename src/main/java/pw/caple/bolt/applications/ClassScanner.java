package pw.caple.bolt.applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassScanner {

	private final URLClassLoader classLoader;
	private final List<Class<?>> classes;

	public ClassScanner(URLClassLoader classLoader) {
		this.classLoader = classLoader;
		List<Class<?>> classes = new ArrayList<>();
		for (URL url : classLoader.getURLs()) {
			try {
				File file = new File(url.toURI());
				if (file.exists()) {
					file = file.getCanonicalFile();
					if (file.isDirectory()) {
						classes.addAll(findLooseClasses(file.toPath()));
					} else if (file.toString().endsWith(".jar")) {
						classes.addAll(findJarredClasses(file));
					}
				}
			} catch (URISyntaxException | IOException e) {
				e.printStackTrace();
			}
		}
		this.classes = classes;
	}

	public List<Class<?>> getSubclassesOf(Class<?> type, ClassLoader loader) {
		List<Class<?>> subClasses = new ArrayList<>();
		for (Class<?> clazz : classes) {
			if (type.isAssignableFrom(clazz)) {
				subClasses.add(clazz);
			}
		}
		return subClasses;
	}

	public List<Method> getAnnotatedMethods(Class<? extends Annotation> annotation) {
		List<Method> methods = new ArrayList<>();
		for (Class<?> clazz : classes) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotation)) {
					methods.add(method);
				}
			}
		}
		return methods;
	}

	public List<Class<?>> getAllClasses() {
		return classes;
	}

	private List<Class<?>> findLooseClasses(Path path) {
		final List<Class<?>> classes = new ArrayList<>();
		try {
			final int packageIndex = path.toString().length();
			final PathMatcher matcher = path.getFileSystem().getPathMatcher("glob:**/*.class");
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (matcher.matches(file)) {
						String name = file.toAbsolutePath().toString().substring(packageIndex);
						if (name.startsWith(File.separator)) name = name.substring(1);
						name = name.substring(0, name.length() - 6).replace(File.separator, ".");
						try {
							Class<?> clazz = Class.forName(name, false, classLoader);
							classes.add(clazz);
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classes;
	}

	private List<Class<?>> findJarredClasses(File file) {
		final List<Class<?>> classes = new ArrayList<>();
		try (
			ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
					String name = entry.getName();
					name = name.substring(0, name.length() - 6).replace(File.separator, ".").replace("/", ".");
					try {
						Class<?> clazz = Class.forName(name, false, classLoader);
						classes.add(clazz);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classes;
	}

}
