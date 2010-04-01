package plugins.LogViewer;

import freenet.l10n.BaseL10n.LANGUAGE;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginL10n;
import freenet.pluginmanager.FredPluginRealVersioned;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.Logger;
import freenet.support.plugins.helpers1.PluginContext;
import freenet.support.plugins.helpers1.WebInterface;

/**
 * @author saces
 * 
 */
public class LogViewer implements FredPlugin, 
		FredPluginVersioned, FredPluginRealVersioned, FredPluginThreadless, FredPluginL10n {

	public static final String PLUGIN_URI = "/LogViewer";
	private static final String PLUGIN_CATEGORY = "Log Viewer";
	public static final String PLUGIN_TITLE = "Log Viewer Plugin";

	private static volatile boolean logMINOR;
	private static volatile boolean logDEBUG;

	static {
		Logger.registerClass(LogViewer.class);
	}

	PluginRespirator pr;

	private PluginContext pluginContext;
	private WebInterface webInterface;

	public void runPlugin(PluginRespirator pluginRespirator) {
		this.pr = pluginRespirator;

		pluginContext = new PluginContext(pluginRespirator);

		webInterface = new WebInterface(pluginContext);

		webInterface.addNavigationCategory(PLUGIN_URI+"/", PLUGIN_CATEGORY, "Log Viewer", this);

		// Visible pages
		LogViewerToadlet lvToadlet = new LogViewerToadlet(pluginContext);
		webInterface.registerVisible(lvToadlet, PLUGIN_CATEGORY, "Log Viewer", "Log Viewer");
	}

	public void terminate() {
		webInterface.kill();
	}

	public String getVersion() {
		return Version.longVersionString;
	}

	public long getRealVersion() {
		return Version.version;
	}

	public String getString(String key) {
		return key;
	}

	public void setLanguage(LANGUAGE newLanguage) {}
}
