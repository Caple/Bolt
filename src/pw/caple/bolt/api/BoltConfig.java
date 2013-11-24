package pw.caple.bolt.api;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class BoltConfig {

	private final List<String> domainList = new ArrayList<>();
	private final List<Content> contentList = new ArrayList<>();
	private final List<Servlet> servletList = new ArrayList<>();
	private final List<SSLCert> certList = new ArrayList<>();
	private boolean forceWSS = false;

	public static class Content {
		public String folder;
		public String url = "/";
		public boolean secure = false;
	}

	public static class Servlet {
		public String url;
		public String className;
		public boolean secure = false;
	}

	public static class SSLCert {
		public String keystore;
		public String password;
		public String ip;
	}

	public boolean getForcedWSS() {
		return forceWSS;
	}

	public List<String> getDomains() {
		return domainList;
	}

	public List<Content> getContent() {
		return contentList;
	}

	public List<Servlet> getServlets() {
		return servletList;
	}

	public List<SSLCert> getCertificates() {
		return certList;
	}

	public void setForcedWSS(boolean value) {
		forceWSS = value;
	}

	public void addDomain(String domain) {
		domainList.add(domain);
	}

	public void addContent(File folder) {
		addContent(folder, "/");
	}

	public void addContent(File folder, String url) {
		Content content = new Content();
		content.url = url.toString();
		content.folder = folder.toString();
		content.secure = false;
		contentList.add(content);
	}

	public void addServlet(Class clazz, String url) {
		Servlet servlet = new Servlet();
		servlet.className = clazz.getCanonicalName();
		servlet.url = url.toString();
		servletList.add(servlet);
	}

	public void addSSL(File keystore, File passwordFile, InetAddress ip) {
		SSLCert cert = new SSLCert();
		cert.keystore = keystore.toString();
		cert.password = passwordFile.toString();
		cert.ip = ip.toString();
		certList.add(cert);
	}

}
