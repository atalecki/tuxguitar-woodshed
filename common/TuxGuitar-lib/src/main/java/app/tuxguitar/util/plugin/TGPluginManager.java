package app.tuxguitar.util.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import app.tuxguitar.resource.TGResourceManager;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGServiceReader;
import app.tuxguitar.util.error.TGErrorManager;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.util.singleton.TGSingletonFactory;
import app.tuxguitar.util.singleton.TGSingletonUtil;

public class TGPluginManager {

	private static final TGLog LOG = TGLog.get(TGPluginManager.class);

	private static final String PLUGIN_ERROR_ON_LOOKUP = "An error occurred when trying to lookup plugin";
	private static final String PLUGIN_ERROR_ON_CONNECT = "An error occurred when trying to connect plugin";
	private static final String PLUGIN_ERROR_ON_EARLY_INIT = "An error occurred when trying to initialize plugin early";
	private static final String PLUGIN_ERROR_ON_DISCONNECT = "An error occurred when trying to disconnect plugin";
	private static final String PLUGIN_ERROR_ON_GET_STATUS = "An error occurred when trying to get plugin status";
	private static final String PLUGIN_ERROR_ON_SET_STATUS = "An error occurred when trying to set plugin status";

	private TGContext context;
	private List<TGPlugin> plugins;

	private TGPluginManager(TGContext context){
		this.context = context;
		this.plugins = new ArrayList<TGPlugin>();
		this.lookupPlugins();
	}

	public List<TGPlugin> getPlugins(){
		return this.plugins;
	}

	public void lookupPlugins(){
		try {
			this.plugins.clear();

			// Search available providers
			Iterator<TGPlugin> it = TGServiceReader.lookupProviders(TGPlugin.class, TGResourceManager.getInstance(this.context));
			while( it.hasNext() ){
				try {
					TGPlugin tgPlugin = it.next();
					if( tgPlugin.getModuleId() != null ) {
						this.plugins.add(tgPlugin);
					}
				} catch(Throwable throwable) {
					TGErrorManager.getInstance(this.context).handleError(new TGPluginException(PLUGIN_ERROR_ON_LOOKUP, throwable));
				}
			}
		} catch(Throwable throwable) {
			TGErrorManager.getInstance(this.context).handleError(new TGPluginException(PLUGIN_ERROR_ON_LOOKUP, throwable));
		}
	}

	public void disconnectAll(){
		this.changeAll(plugin -> true, this::disconnectPlugin, "plugins disconnected: ");
	}

	public void connectEnabled() {
		this.changeAll(plugin -> this.isEnabled(plugin.getModuleId()), this::connectPlugin, "plugins connected: ");
	}

	public void earlyInitPlugins() {
		for(TGPlugin plugin : this.plugins) {
			if( this.isEnabled(plugin.getModuleId()) && TGEarlyInitPlugin.class.isAssignableFrom(plugin.getClass())) {
				this.earlyInitPlugin((TGEarlyInitPlugin) plugin);
			}
		}
	}
	public void connectPlugins(String moduleId) {
		this.changeAll(plugin -> plugin.getModuleId().equals(moduleId), this::connectPlugin, "plugins of module " + moduleId + " connected: ");
	}

	public void disconnectPlugins(String moduleId) {
		this.changeAll(plugin -> plugin.getModuleId().equals(moduleId), this::disconnectPlugin, "plugins of module " + moduleId + " disconnected: ");
	}

	// changes the chosen plugins one by one, then logs how many it changed (each at DEBUG, the count at INFO)
	private void changeAll(Predicate<TGPlugin> chosen, Predicate<TGPlugin> change, String summary) {
		int changed = 0;
		for(TGPlugin plugin : this.plugins) {
			if( chosen.test(plugin) && change.test(plugin) ) {
				changed ++;
			}
		}
		LOG.info(summary + changed);
	}

	// whether it connected
	public boolean connectPlugin(TGPlugin tgPlugin) {
		return this.change(tgPlugin, " connected: ", PLUGIN_ERROR_ON_CONNECT, plugin -> plugin.connect(this.context));
	}

	// whether the plugin connected or disconnected, logged; its failure handled as it is, an unexpected one wrapped
	private boolean change(TGPlugin tgPlugin, String done, String failure, Change change) {
		try {
			change.apply(tgPlugin);
			LOG.debug("plugin " + tgPlugin.getModuleId() + done + tgPlugin.getClass().getName());
			return true;
		} catch(TGPluginException exception) {
			TGErrorManager.getInstance(this.context).handleError(exception);
		} catch(Throwable throwable) {
			TGErrorManager.getInstance(this.context).handleError(new TGPluginException(failure, throwable));
		}
		return false;
	}

	private interface Change {
		void apply(TGPlugin plugin) throws TGPluginException;
	}

	public void earlyInitPlugin(TGEarlyInitPlugin tgPlugin) {
		try {
			tgPlugin.earlyInit(this.context);
		} catch(TGPluginException exception) {
			exception.printStackTrace();
			TGErrorManager.getInstance(this.context).handleError(exception);
		} catch(Throwable throwable) {
			throwable.printStackTrace();
			TGErrorManager.getInstance(this.context).handleError(new TGPluginException(PLUGIN_ERROR_ON_EARLY_INIT, throwable));
		}
	}

	// whether it disconnected
	public boolean disconnectPlugin(TGPlugin tgPlugin) {
		return this.change(tgPlugin, " disconnected: ", PLUGIN_ERROR_ON_DISCONNECT, plugin -> plugin.disconnect(this.context));
	}

	public void updatePluginStatus(String moduleId, boolean enabled) {
		try {
			TGPluginProperties.getInstance(this.context).setEnabled(moduleId, enabled);

			if( enabled ) {
				this.connectPlugins(moduleId);
			} else {
				this.disconnectPlugins(moduleId);
			}
		} catch(Throwable throwable) {
			TGErrorManager.getInstance(this.context).handleError(new TGPluginException(PLUGIN_ERROR_ON_SET_STATUS,throwable));
		}
	}

	public boolean isEnabled(String moduleId){
		try {
			return TGPluginProperties.getInstance(this.context).isEnabled(moduleId);
		} catch(Throwable throwable) {
			TGErrorManager.getInstance(this.context).handleError(new TGPluginException(PLUGIN_ERROR_ON_GET_STATUS,throwable));
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public <T extends TGPlugin> List<T> getPluginInstances(Class<T> pluginClass){
		List<T> pluginInstances = new ArrayList<T>();
		for(TGPlugin plugin : this.plugins) {
			if( pluginClass.isInstance(plugin) ){
				pluginInstances.add( (T)plugin );
			}
		}
		return pluginInstances;
	}

	public static TGPluginManager getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGPluginManager.class.getName(), new TGSingletonFactory<TGPluginManager>() {
			public TGPluginManager createInstance(TGContext context) {
				return new TGPluginManager(context);
			}
		});
	}
}
