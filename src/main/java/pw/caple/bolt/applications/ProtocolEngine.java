package pw.caple.bolt.applications;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URLClassLoader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import pw.caple.bolt.api.Client;
import pw.caple.bolt.api.Protocol;
import pw.caple.bolt.api.SetParser;
import pw.caple.bolt.api.TypeParser;

public class ProtocolEngine {

	//TODO: search jars and other places than just bin

	private final Map<String, List<Method>> methods = new ConcurrentHashMap<>();
	private final Map<Class<?>, TypeParser<?>> typeParsers = new ConcurrentHashMap<>();
	private final ReservedProtocol reservedProtocol;
	private final URLClassLoader classLoader;

	public ProtocolEngine(URLClassLoader classLoader) {
		this.classLoader = classLoader;
		reservedProtocol = new ReservedProtocol(this);
		loadCustomParsers();
		loadDefaultParsers();
		loadProtocolMethods();
	}

	private void loadProtocolMethods() {
		ClassScanner scanner = new ClassScanner(classLoader);
		for (Method method : scanner.getAnnotatedMethods(Protocol.class)) {
			if (Modifier.isStatic(method.getModifiers())) {
				if (!method.isAccessible()) {
					method.setAccessible(true);
				}
				String methodName = method.getName();
				if (!methods.containsKey(methodName)) {
					methods.put(methodName, new CopyOnWriteArrayList<Method>());
				}
				List<Method> methodList = methods.get(methodName);
				methodList.add(method);
			} else {
				throw new RuntimeException("Error on method " + method.getName() + "(...) - Method must be static.");
			}
		}

		for (Method method : reservedProtocol.getClass().getDeclaredMethods()) {
			String methodName = method.getName();
			if (!methods.containsKey(method.getName())) {
				methods.put(methodName, new CopyOnWriteArrayList<Method>());
			}
			List<Method> methodList = methods.get(methodName);
			methodList.add(method);
		}

	}

	private void loadCustomParsers() {
		ClassScanner scanner = new ClassScanner(classLoader);
		for (Class<?> clazz : scanner.getAllClasses()) {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(SetParser.class)) {
					if (TypeParser.class != field.getType()) {
						throw new RuntimeException("Error on " + SetParser.class.getSimpleName() + " field [" + field.getName() + "] of type " + clazz.getSimpleName() + " - Wrong type.");
					}
					if (!Modifier.isStatic(field.getModifiers())) {
						throw new RuntimeException("Error on " + SetParser.class.getSimpleName() + " field [" + field.getName() + "] of type " + clazz.getSimpleName() + " - Must be static.");
					}
					if (!field.isAccessible()) field.setAccessible(true);
					try {
						TypeParser<?> value = (TypeParser<?>) field.get(null);
						typeParsers.put(clazz, value);
						break;
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	private void loadDefaultParsers() {
		TypeParser<?> parser;

		parser = new TypeParser<String>() {
			@Override
			public String parse(String string) {
				return string;
			}
		};
		typeParsers.put(String.class, parser);

		parser = new TypeParser<Boolean>() {
			@Override
			public Boolean parse(String string) {
				if (string.equals("true") || string.equals("yes") || string.equals("1")) {
					return true;
				} else if (string.equals("false") || string.equals("no") || string.equals("0")) {
					return false;
				} else {
					return null;
				}
			}
		};
		typeParsers.put(Boolean.class, parser);
		typeParsers.put(boolean.class, parser);

		parser = new TypeParser<Byte>() {
			@Override
			public Byte parse(String string) {
				try {
					return Byte.parseByte(string);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		};
		typeParsers.put(Byte.class, parser);
		typeParsers.put(byte.class, parser);

		parser = new TypeParser<Short>() {
			@Override
			public Short parse(String string) {
				try {
					return Short.parseShort(string);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		};
		typeParsers.put(Short.class, parser);
		typeParsers.put(short.class, parser);

		parser = new TypeParser<Integer>() {
			@Override
			public Integer parse(String string) {
				try {
					return Integer.parseInt(string);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		};
		typeParsers.put(Integer.class, parser);
		typeParsers.put(int.class, parser);

		parser = new TypeParser<Long>() {
			@Override
			public Long parse(String string) {
				try {
					return Long.parseLong(string);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		};
		typeParsers.put(Long.class, parser);
		typeParsers.put(long.class, parser);

		parser = new TypeParser<Float>() {
			@Override
			public Float parse(String string) {
				try {
					return Float.parseFloat(string);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		};
		typeParsers.put(Float.class, parser);
		typeParsers.put(float.class, parser);

		parser = new TypeParser<Double>() {
			@Override
			public Double parse(String string) {
				try {
					return Double.parseDouble(string);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		};
		typeParsers.put(Double.class, parser);
		typeParsers.put(double.class, parser);

		parser = new TypeParser<Character>() {
			@Override
			public Character parse(String string) {
				if (string.length() == 1) {
					return string.charAt(0);
				} else {
					return null;
				}
			}
		};
		typeParsers.put(Character.class, parser);
		typeParsers.put(char.class, parser);

		parser = new TypeParser<Date>() {
			@Override
			public Date parse(String string) {
				try {
					long utc = Long.parseLong(string);
					return new Date(utc);
				} catch (NumberFormatException e) {}
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				try {
					return format.parse(string.replace('/', '-'));
				} catch (ParseException ignore) {}
				format = new SimpleDateFormat("HH:mm:ss");
				try {
					return format.parse(string);
				} catch (ParseException ignore) {}
				return null;
			}
		};
		typeParsers.put(Date.class, parser);

		parser = new TypeParser<BigDecimal>() {
			@Override
			public BigDecimal parse(String string) {
				try {
					return new BigDecimal(string);
				} catch (NumberFormatException e) {
					return null;
				}
			}
		};
		typeParsers.put(BigDecimal.class, parser);

	}

	public Object callMethod(String methodName, String[] args, Client client) {
		if (methods.containsKey(methodName)) {
			for (Method method : methods.get(methodName)) {
				if (isTarget(method, args)) {
					return call(client, method, args);
				}
			}
			client.send("error", methodName + ": wrong number of parameters");
		} else {
			client.send("error", methodName + ": unknown command");
		}
		return null;
	}

	private boolean isTarget(Method method, String[] args) {
		Class<?>[] params = method.getParameterTypes();
		if (params.length > 0 && Client.class.isAssignableFrom(params[0])) {
			if (method.isVarArgs() && args.length + 1 >= params.length) {
				return true;
			} else {
				return params.length == args.length + 1;
			}
		} else {
			if (method.isVarArgs() && args.length >= params.length) {
				return true;
			} else {
				return params.length == args.length;
			}
		}
	}

	private Object call(Client client, Method method, String[] given) {
		Class<?>[] params = method.getParameterTypes();
		Object[] args = new Object[params.length];
		int offset = 0;
		if (params.length > 0) {
			if (Client.class.isAssignableFrom(params[0])) {
				offset += 1;
				args[0] = client;
			}
		}
		for (int i = offset; i < params.length; i++) {
			if (method.isVarArgs() && i == params.length - 1) {
				Class<?> type = params[i].getComponentType();
				Object varArgs = Array.newInstance(type, given.length - i + offset);
				for (int givenIndex = i; givenIndex < given.length + offset; givenIndex++) {
					Object value = parseParameter(type, given[givenIndex - offset]);
					Array.set(varArgs, givenIndex - i, value);
				}
				args[i] = varArgs;
				break;
			} else {
				Object object = parseParameter(params[i], given[i - offset]);
				if (object != null) {
					args[i] = object;
				} else {
					client.send("error", method.getName() + ": syntax incorrect; expected "
							+ params[i].getSimpleName() + " for parameter " + (i - offset + 1));
					return null;
				}
			}
		}
		try {
			if (method.getDeclaringClass() == ReservedProtocol.class) {
				return method.invoke(reservedProtocol, args);
			} else {
				return method.invoke(null, args);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Object parseParameter(Class<?> type, String string) {
		if (typeParsers.containsKey(type)) {
			return typeParsers.get(type).parse(string);
		} else {
			throw new RuntimeException("unsupported parameter type [" + type.getSimpleName() + "] found in a protocol method");
		}
	}

}
