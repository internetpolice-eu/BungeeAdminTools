package fr.Alphart.BAT.Modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.cubespace.Yamler.Config.InvalidConfigurationException;
import net.md_5.bungee.api.plugin.Listener;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Ban.Ban;
import fr.Alphart.BAT.Modules.Comment.Comment;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.Modules.Kick.Kick;
import fr.Alphart.BAT.Modules.Mute.Mute;

public class ModulesManager {
    protected final BAT plugin;
	private final Map<IModule, Integer> modules;
	private final Map<String, IModule> modulesNames;
	private Map<String, IModule> cmdsModules;

	public ModulesManager(BAT plugin) {
	    this.plugin = plugin;
		modules = new LinkedHashMap<>();
		modulesNames = new HashMap<>();
	}

	public void loadModules() {
		// The core module MUST NOT be disabled.
		modules.put(new Core(), IModule.OFF_STATE);
		modules.put(new Ban(plugin), IModule.OFF_STATE);
		modules.put(new Mute(plugin), IModule.OFF_STATE);
		modules.put(new Kick(plugin), IModule.OFF_STATE);
		modules.put(new Comment(plugin), IModule.OFF_STATE);
		cmdsModules = new HashMap<>();
		for (final IModule module : modules.keySet()) {
			// The core doesn't have settings to enable or disable it
			if (!module.getName().equals("core")) {
				if (!module.getConfig().isEnabled()) {
					continue;
				}
			}
			if (module.load()) {
				modulesNames.put(module.getName(), module);
				modules.put(module, IModule.ON_STATE);

				if (module instanceof Listener) {
					plugin.getProxy().getPluginManager().registerListener(plugin, (Listener) module);
				}

				for (final BATCommand cmd : module.getCommands()) {
					cmdsModules.put(cmd.getName(), module);
					plugin.getProxy().getPluginManager().registerCommand(BAT.getInstance(), cmd);
				}
				if(module.getConfig() != null){
					try {
						module.getConfig().save();
					} catch (final InvalidConfigurationException e) {
						e.printStackTrace();
					}
				}
			} else {
				plugin.getLogger().severe("The " + module.getName() + " module encountered an error.");
			}
		}
	}

	public void unloadModules() {
		for (final IModule module : getLoadedModules()) {
			module.unload();
			if(module instanceof Listener){
				plugin.getProxy().getPluginManager().unregisterListener((Listener) module);
			}
			modules.put(module, IModule.OFF_STATE);
		}
		plugin.getProxy().getPluginManager().unregisterCommands(plugin);
		modules.clear();
	}

	public Set<IModule> getLoadedModules() {
		final Set<IModule> modulesList = new HashSet<>();
		for (final Entry<IModule, Integer> entry : modules.entrySet()) {
			if (entry.getValue().equals(IModule.ON_STATE)) {
				modulesList.add(entry.getKey());
			}
		}
		return modulesList;
	}

	public boolean isLoaded(final String name) {
		try {
			if (getModule(name) != null) {
				return true;
			}
		} catch (final InvalidModuleException ignored) {}
		return false;
	}

	public Core getCore() {
		try {
			final IModule module = getModule("core");
			if (module != null) {
				return (Core) module;
			}
		} catch (final InvalidModuleException e) {
			plugin.getLogger().severe("The core module encountered a problem. Please report this to the developper :");
			e.printStackTrace();
		}
		return null;
	}

	public Ban getBanModule() throws InvalidModuleException {
		final IModule module = getModule("ban");
		if (module != null) {
			return (Ban) module;
		}
		return null;
	}

	public Mute getMuteModule() throws InvalidModuleException {
		final IModule module = getModule("mute");
		if (module != null) {
			return (Mute) module;
		}
		return null;
	}

	public Kick getKickModule() throws InvalidModuleException {
		final IModule module = getModule("kick");
		if (module != null) {
			return (Kick) module;
		}
		return null;
	}

	public Comment getCommentModule() throws InvalidModuleException {
		final IModule module = getModule("comment");
		if (module != null) {
			return (Comment) module;
		}
		return null;
	}
	
	public IModule getModule(final String name) throws InvalidModuleException {
		final IModule module = modulesNames.get(name);
		if (module != null && modules.get(module).equals(IModule.ON_STATE)) {
			return module;
		}
		throw new InvalidModuleException("Module not found or invalid");
	}
}
