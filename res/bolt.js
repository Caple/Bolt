log = {};

if (window && window.console) {
	log.info = function(text) {
		window.console.info(text);
	};
	log.debug = function(text) {
		window.console.debug(text);
	};
	log.warn = function(text) {
		window.console.warn(text);
	};
	log.error = function(text) {
		window.console.error(text);
	};
} else {
	log.info = function(text) {};
	log.debug = function(text) {};
	log.warn = function(text) {};
	log.error = function(text) {};
}

var server = {};
server.debug = true;
server.internal = {};
server.internal.callbacks = {};
server.internal.events = {};
server.internal.messageID = 0;

server.internal.readyStack = [];
server.ready = function(func) {
	server.internal.readyStack.push(func);
};

server.internal.runReadyFunctions = function() {
	var func = server.internal.readyStack.pop();
	while (func) {
		func();
		func = server.internal.readyStack.pop();
	}
};

//
// Can be used in the following ways
// send(methodName)
// send(methodName, callback)
// send(methodName, args, args, args...)
// send(methodName, args, args ... callback)
//
server.send = function() {
	if (arguments.length == 0) return;
	var callback = null;
	var id = server.internal.messageID++;

	var message = id + "*CALL!" + arguments[0];
	for (var i = 1; i < arguments.length; i++) {
		if (typeof arguments[i] == typeof Function) {
			callback = arguments[i];
		} else {
			message += "*" + String(arguments[i]).replace("*", "&#42;");
		}
	}
	if (callback) {
		server.internal.callbacks[id] = callback;
	} else {
		server.internal.callbacks[id] = function() {};
	}
	if (server.debug) log.debug('Client: ' + message);
	server.internal.socket.send(message);
};

server.internal.events.onopen = function(event) {
	if (server.debug) log.debug('Socket Open');
};

server.internal.events.onclose = function(event) {
	if (server.debug) log.debug('Socket Closed');
	if (event.code == 4040) {
		// request to upgrade to wss
		server.internal.connect(true);
	}
};

server.internal.events.onerror = function(event) {
	log.error("WebSocket error");
};

server.internal.events.onmessage = function(event) {
	if (typeof event.data === "string") {
		server.internal.events.onTextMessage(event.data);
	} else if (event.data instanceof ArrayBuffer) {
		server.internal.events.onBinaryMessage(event.data);
	} else {
		log.warn('discarded unsupported message type');
	}
};

server.internal.events.onTextMessage = function(data) {
	if (server.debug) log.debug('Server: ' + data);

	var headerLength = data.indexOf("!");
	var header = data.substring(0, headerLength).split("*");

	var id = header[0];
	var mode = header[1];
	var args = [];

	if (data.length > headerLength + 1) {
		args = data.substring(headerLength + 1).split("*");
	}

	if (mode == "RETURN") {
		var clientMessageID = args[0];
		var callback = server.internal.callbacks[clientMessageID];
		delete server.internal.callbacks[clientMessageID];
		if (args.length === 2) {
			callback(args[1]);
		} else {
			callback();
		}
		return;
	} else if (mode === "CALL") {
		var result = null;
		var funcName = args.shift();
		var func = window[funcName];
		if (func) {
			result = func.apply(this, args);
		} else {
			log.warn('missing method -> ' + funcName);
		}
		var returnMessage = server.internal.messageID++ + "*RETURN!" + id;
		if (result) {
			returnMessage += "*" + String(result).replace("*", "&#42;");
		}
		if (server.debug) log.debug('Client: ' + returnMessage);
		server.internal.socket.send(returnMessage);
	} else if (mode == "READY") {
		server.internal.runReadyFunctions();
	}

};

server.internal.events.onBinaryMessage = function(data) {
	if (server.debug) log.debug('<-< BINARY DATA');
	log.debug('discarded arraybuffer message, not implemented');
};

server.internal.connect = function(forceWSS) {

	if ('https:' === document.location.protocol) {
		wss = true;
	} else if (forceWSS || document.cookie.indexOf("forceWSS=") !== -1) {
		wss = true;
		document.cookie = "forceWSS=1";
	} else {
		wss = false;
	}

	if (wss) {
		address = 'wss://' + window.location.host + '/bolt/socket';
	} else {
		address = 'ws://' + window.location.host + '/bolt/socket';
	}

	if ('WebSocket' in window) {
		server.internal.socket = new WebSocket(address);
	} else if ('MozWebSocket' in window) {
		server.internal.socket = new MozWebSocket(address);
	} else {
		window.location = "/bolt/unsupported";
		return;
	}

	server.internal.socket.binaryType = "arraybuffer";
	server.internal.socket.onopen = server.internal.events.onopen;
	server.internal.socket.onclose = server.internal.events.onclose;
	server.internal.socket.onerror = server.internal.events.onerror;
	server.internal.socket.onmessage = server.internal.events.onmessage;
};

server.internal.connect(false);

// server.saveSession = function(sessionID) {
// $.cookie('bolt_sid', sessionID, { expires : 21, path : '/' });
// $.cookie('bolt_remember', "1", { expires : 21, path : '/' });
// };
