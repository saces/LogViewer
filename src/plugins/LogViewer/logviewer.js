var ws = null;

function WebSocketTest()
{
	if ("WebSocket" in window)
	{
		// remove the note about 'use a websock browser'
		document.getElementById('bah').innerText = "";
	}
	else
	{
		// remove the logpanel
		document.getElementById('logpanel').innerText = "";
	}
}

function ClearLog()
{
	document.getElementById('logcontent').innerText = "";
}

function StartLogging() {
	if (!("WebSocket" in window))
	{
		// hu, ancient browser, go away
		return;
	}

	// replace the leading 'http' with 'ws'
	ws = new WebSocket("ws"+document.location.href.substr(4), "logger");

	ws.onopen = function(evt) { 
		ws.send("Hello Server!");
	};
	ws.onmessage = function(evt) {
		// alert("Got a sock from server: " + evt.data);
		previusContent = document.getElementById('logcontent').innerText;
		if (previusContent.length > 4096) {
			previusContent = previusContent.substr(1024);
		}
		previusContent = previusContent.concat("" + evt.data); 
		document.getElementById('logcontent').innerText = previusContent;
	};
	ws.onclose = function(evt) {
		alert("Conn closed");
	};
	// spec says something about, but goggling for
	// 'websocket onerror' shows only the spec, no samples :(
	// ws.onerror = fiction(evt) {
	//     alert(errmsg);
	// };
}

function StopLogging() {
	if (!("WebSocket" in window))
	{
		// hu, ancient browser, go away
		return;
	}
	// close the socket, nothing else to do to stop
	ws.close();
}