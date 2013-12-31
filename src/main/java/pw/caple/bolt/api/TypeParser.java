package pw.caple.bolt.api;

public interface TypeParser<T> {
	public abstract T parse(String string);
}
