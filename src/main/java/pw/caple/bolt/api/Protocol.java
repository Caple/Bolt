package pw.caple.bolt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Designates a static method as apart of the server communications protocol.
 * The method is automatically registered into the core systems at runtime. It
 * can then be called by clients communicating with the server. Do not use this
 * annotation on any code that should not be exposed to clients.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Protocol {

	/**
	 * A simple hierarchical security level for protocol methods.
	 */
	public enum SecurityClearance {

		/**
		 * Public commands
		 */
		PUBLIC(0),

		/**
		 * Private commands
		 */
		PRIVATE(1),

		/**
		 * Highly sensitive commands such as direct server or systems access
		 */
		SENSITIVE(2);

		final private int level;

		SecurityClearance(int level) {
			this.level = level;
		}

		public int getLevel() {
			return level;
		}

	}

	/**
	 * Restricts who can access a protocol method. Clients with a security
	 * clearance below this level will be denied access to the method, but will
	 * still be able to query information about it. Group based and context
	 * based security should still be used. This setting is only an additional
	 * firewall to protect more sensitive methods.
	 */
	SecurityClearance security() default SecurityClearance.PUBLIC;
}
