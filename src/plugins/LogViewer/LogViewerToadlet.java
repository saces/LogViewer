package plugins.LogViewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import freenet.client.DefaultMIMETypes;
import freenet.clients.http.PageNode;
import freenet.clients.http.ToadletContext;
import freenet.clients.http.ToadletContextClosedException;
import freenet.clients.http.WebSocketAcceptor;
import freenet.clients.http.WebSocketHandler;
import freenet.clients.http.WebSocketSender;
import freenet.support.HTMLNode;
import freenet.support.Logger;
import freenet.support.LoggerHook;
import freenet.support.api.Bucket;
import freenet.support.api.HTTPRequest;
import freenet.support.plugins.helpers1.PluginContext;
import freenet.support.plugins.helpers1.WebInterfaceToadlet;

public class LogViewerToadlet extends WebInterfaceToadlet implements WebSocketAcceptor {

	private final static String STATIC_PREFIX = "/plugins/LogViewer/";

	public LogViewerToadlet(PluginContext pluginContext2) {
		super(pluginContext2, LogViewer.PLUGIN_URI, "");
	}

	public void handleMethodGET(URI uri, HTTPRequest req, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		String path = normalizePath(req.getPath());
		if (!path.equals("/")) {
			handleStatic(path, ctx);
			return;
		}
		makePage(ctx);
	}

	private void makePage(ToadletContext ctx) throws ToadletContextClosedException, IOException {
		PageNode pageNode = pluginContext.pageMaker.getPageNode("Log Viewer", ctx);
		HTMLNode outer = pageNode.outer;
		HTMLNode contentNode = pageNode.content;
		HTMLNode headNode = pageNode.headNode;

		// sanitize first
		if (!ctx.getContainer().isFProxyJavascriptEnabled()) {
			// no java script? go away with your web 1.0 settings,
			// websockets is web 3.0.
			HTMLNode errorbox = ctx.getPageMaker().getInfobox("infobox-error", "WebSockets Error: Javascript is disabled", contentNode, "websocket-error", true);
			errorbox.addChild("#", "Websockets requires Javascript to function, but Javascript is disabled in your configuration.");
			errorbox.addChild("br");
			errorbox.addChild("#", "Please enable Javascript or stay 'Web 1.0'.");
			writeHTMLReply(ctx, 200, "OK", outer.generate());
			return;
		}

		if (!ctx.getContainer().isWebSocketEnabled()) {
			// no java script? go away with your web 1.0 settings,
			// websockets is web 3.0.
			HTMLNode errorbox = ctx.getPageMaker().getInfobox("infobox-error", "WebSockets Error: WebSockets feature is disabled", contentNode, "websocket-error", true);
			errorbox.addChild("#", "Websockets requires to be anabled to function, but WebSockets is disabled in your configuration.");
			errorbox.addChild("br");
			errorbox.addChild("#", "Please enable WebSockets or stay 'Web 1.0'.");
			writeHTMLReply(ctx, 200, "OK", outer.generate());
			return;
		}

		pageNode.bodyNode.addAttribute("onload", "WebSocketTest()");
		pageNode.bodyNode.addAttribute("onunload", "StopLogging()");

		HTMLNode scriptNode = headNode.addChild("script","//abc");
		scriptNode.addAttribute("type", "text/javascript");
		scriptNode.addAttribute("src", path()+"logviewer.js");

		HTMLNode bahbox = pluginContext.pageMaker.getInfobox("infobox-information", "Bah!", contentNode.addChild("span", "id", "bah"));

		bahbox.addChild("#", "WebSocket not supported in your Browser!");
		bahbox.addChild("br");
		bahbox.addChild("#", "Try a recent browser with HTML5/WebSockets support");

		HTMLNode logbox = pluginContext.pageMaker.getInfobox("infobox-information", "Freenet log", contentNode.addChild("span", "id", "logpanel"));
		logbox.addChild("a", "href", "javascript:StartLogging()", "Start");
		logbox.addChild("#", "\u00a0");
		logbox.addChild("a", "href", "javascript:ClearLog()", "Clear");
		logbox.addChild("#", "\u00a0");
		logbox.addChild("a", "href", "javascript:StopLogging()", "Stop");
		logbox.addChild("br");
		logbox.addChild("span", "id", "logcontent", "\u00a0");

		writeHTMLReply(ctx, 200, "OK", outer.generate());
	}

	private void handleStatic(String path, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		// be very strict about what characters we allow in the path, since
		if (!path.matches("^[A-Za-z0-9\\._\\/\\-]*$") || (path.indexOf("..") != -1)) {
			this.sendErrorPage(ctx, 404, "pathNotFoundTitle", "pathInvalidChars");
			return;
		}

		String mypath = STATIC_PREFIX+path;
		System.out.println(("PATHTEST: "+ mypath));
		InputStream strm = getClass().getResourceAsStream(STATIC_PREFIX+path);
		if (strm == null) {
			this.sendErrorPage(ctx, 404, "pathNotFoundTitle", "pathNotFound");
			return;
		}
		Bucket data = ctx.getBucketFactory().makeBucket(strm.available());
		OutputStream os = data.getOutputStream();
		byte[] cbuf = new byte[4096];
		while(true) {
			int r = strm.read(cbuf);
			if(r == -1) break;
			os.write(cbuf, 0, r);
		}
		strm.close();
		os.close();

		URL url = getClass().getResource(STATIC_PREFIX+path);
		Date mTime = getUrlMTime(url);

		ctx.sendReplyHeaders(200, "OK", null, DefaultMIMETypes.guessMIMEType(path, false), data.size(), mTime);

		ctx.writeData(data);
		data.free();
	}

	/**
	 * Try to find the modification time for a URL, or return null if not possible
	 * We usually load our resources from the JAR, or possibly from a file in some setups, so we check the modification time of
	 * the JAR for resources in a jar and the mtime for files.
	 */
	private Date getUrlMTime(URL url) {
		if (url.getProtocol().equals("jar")) {
			File f = new File(url.getPath().substring(0, url.getPath().indexOf('!')));
			return new Date(f.lastModified());
		} else if (url.getProtocol().equals("file")) {
			File f = new File(url.getPath());
			return new Date(f.lastModified());
		} else {
			return null;
		}
	}

	public WebSocketHandler acceptUpgrade(String host, String origin, String protocol) {
		return new LogSocket();
	}

	public static class LogSocket implements WebSocketHandler {

		private LogHook logHook;

		public LogSocket() {
		}

		public void onBeginService(WebSocketSender sender) {
			logHook = new LogHook(sender);
			Logger.globalAddHook(logHook);
		}

		public void onMessage(String message) {
			// ignore for now
			Logger.error(this, "Client tried to send us: "+message);
		}

		public void onClose() {
			Logger.error(this, "Connection got closed remotly.");
			Logger.globalRemoveHook(logHook);
		}
	}

	public static class LogHook extends LoggerHook {

		private final WebSocketSender wsSender;

		LogHook(WebSocketSender sender) {
			super(Logger.DEBUG);
			wsSender = sender;
		}

		@Override
		public void log(Object o, Class<?> c, String msg, Throwable e,
				int priority) {
			if (!instanceShouldLog(priority, c))
				return;

			StringBuilder sb = new StringBuilder( e == null ? 512 : 1024 );
			int sctr = 0;

//			for (int i = 0; i < fmt.length; ++i) {
//				switch (fmt[i]) {
//					case 0 :
//						sb.append(str[sctr++]);
//						break;
//					case DATE :
//						long now = System.currentTimeMillis();
//						synchronized (this) {
//							myDate.setTime(now);
//							sb.append(df.format(myDate));
//						}
//						break;
//					case CLASS :
//						sb.append(c == null ? "<none>" : c.getName());
//						break;
//					case HASHCODE :
//						sb.append(
//							o == null
//								? "<none>"
//								: Integer.toHexString(o.hashCode()));
//						break;
//					case THREAD :
//						sb.append(Thread.currentThread().getName());
//						break;
//					case PRIORITY :
//						sb.append(LoggerHook.priorityOf(priority));
//						break;
//					case MESSAGE :
//						sb.append(msg);
//						break;
//					case UNAME :
//						sb.append(uname);
//						break;
//				}
//			}
			sb.append(msg);
			sb.append('\n');

			// Write stacktrace if available
			for(int j=0;j<20 && e != null;j++) {
				sb.append(e.toString());
				
				StackTraceElement[] trace = e.getStackTrace();
				
				if(trace == null)
					sb.append("(null)\n");
				else if(trace.length == 0)
					sb.append("(no stack trace)\n");
				else {
					sb.append('\n');
					for(int i=0;i<trace.length;i++) {
						sb.append("\tat ");
						sb.append(trace[i].toString());
						sb.append('\n');
					}
				}
				
				Throwable cause = e.getCause();
				if(cause != e) e = cause;
				else break;
			}

			try {
				wsSender.sendMessage(sb.toString());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		public long anyFlags() {
			return ((2 * ERROR) - 1) & ~(threshold - 1);
		}

		@Override
		public long minFlags() {
			return 0;
		}

		@Override
		public long notFlags() {
			return 0;
		}

	}
}
