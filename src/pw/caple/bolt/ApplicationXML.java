package pw.caple.bolt;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "application")
class ApplicationXML {

	@XmlElement
	String name;

	@XmlElement
	String mainClass;

	@XmlElement
	Git git;

	@XmlRootElement
	static class Git {
		@XmlElement
		String url;
		@XmlElement
		String username;
		@XmlElement
		String password;
	}

	@XmlElementWrapper
	@XmlElement(name = "address")
	List<String> bindings;

	@XmlElement
	Map map;

	@XmlRootElement
	static class Map {

		@XmlElement
		List<Content> content;
		@XmlElement
		List<Socket> socket;
		@XmlElement
		List<Servlet> servlet;

		@XmlRootElement
		static class Content {
			@XmlAttribute
			String folder;
			@XmlAttribute
			String url;
		}

		@XmlRootElement
		static class Socket {
			@XmlAttribute
			String url;
		}

		@XmlRootElement
		static class Servlet {
			@XmlAttribute
			String className;
			@XmlAttribute
			String url;
		}
	}

	@XmlElement
	Security security;

	@XmlRootElement
	static class Security {
		@XmlElement
		Keystore keystore;
		@XmlElement
		String pepper;

		static class Keystore {
			@XmlElement
			String path;
			@XmlElement
			String password;
		}
	}

}
