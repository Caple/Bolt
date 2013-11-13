package pw.caple.bolt.socket;

public class ProtocolMessage {

	private final long id;
	private final Mode mode;
	private final String[] args;

	enum Mode {
		CALL, // make a remote function call
		RETURN, // return the result of a remote call
		DIE; // poison the message queue and kill the connection
	}

	ProtocolMessage(long id, Mode mode, String[] args) {
		this.id = id;
		this.mode = mode;
		this.args = args;
	}

	ProtocolMessage(String message) {
		int headerLength = message.indexOf("!");
		String[] header = message.substring(0, headerLength).split("\\*");

		id = Long.parseLong(header[0]);
		mode = Mode.valueOf(header[1]);
		if (message.length() > headerLength + 1) {
			args = message.substring(headerLength + 1).split("\\*");
		} else {
			args = new String[0];
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(id);
		builder.append('*');
		builder.append(mode);
		builder.append('!');
		boolean join = false;
		for (String arg : args) {
			if (join) {
				builder.append('*');
			} else {
				join = true;
			}
			builder.append(arg);
		}
		return builder.toString();
	}

	public long getId() {
		return id;
	}

	public Mode getMode() {
		return mode;
	}

	public String[] getArgs() {
		return args;
	}

	static ProtocolMessage getPoison() {
		return new ProtocolMessage("0*DIE!");
	}

}
