package pw.caple.bolt.api;

/**
 * This class can be included in the arguments of an asynchronous client
 * function call. It will then be called when the asynchronous function has
 * returned a result.
 */
public interface Callback {
	public void run(String result);
}
