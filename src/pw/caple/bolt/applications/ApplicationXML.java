package pw.caple.bolt.applications;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "config")
class ApplicationXML {

	@XmlElement
	List<String> domain = new ArrayList<>();

	@XmlElement
	List<Content> content = new ArrayList<>();

	@XmlRootElement
	static class Content {
		@XmlAttribute(required = true)
		String folder;
		@XmlAttribute
		String url = "/";
		@XmlAttribute
		boolean secure = false;
	}

	@XmlElement
	List<Servlet> servlet = new ArrayList<>();

	@XmlRootElement
	static class Servlet {
		@XmlAttribute(required = true)
		String url;
		@XmlAttribute(name = "class", required = true)
		String className;
		@XmlAttribute
		boolean secure = false;
	}

	@XmlElement
	Security security = new Security();

	@XmlRootElement
	static class Security {
		@XmlElement
		boolean forceWSS = false;

		@XmlElement
		List<SSL> ssl = new ArrayList<>();

		static class SSL {
			@XmlAttribute(required = true)
			String keystore;
			@XmlAttribute(required = true)
			String password;
			@XmlAttribute
			String alias;
			@XmlAttribute
			String ip;
		}
	}

}
