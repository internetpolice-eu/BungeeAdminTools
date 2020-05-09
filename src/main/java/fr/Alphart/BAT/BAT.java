package fr.Alphart.BAT;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import com.google.common.base.Preconditions;
import fr.Alphart.BAT.I18n.I18n;
import fr.Alphart.BAT.Modules.ModulesManager;
import fr.Alphart.BAT.Utils.CallbackUtils.Callback;
import fr.Alphart.BAT.Utils.Utils;
import fr.Alphart.BAT.database.DataSourceHandler;

public class BAT extends Plugin {
    private static BAT instance;
    private static DataSourceHandler dsHandler;
    private Configuration config;
    private static String prefix;
    private ModulesManager modules;

    @Override
    public void onEnable() {
        instance = this;
        config = new Configuration();
        prefix = config.getPrefix();
        getLogger().setLevel(Level.INFO);

        loadDB((dbEnabled, throwable) -> {
            if (dbEnabled) {
                modules = new ModulesManager(this);
                modules.loadModules();
            } else {
                getLogger().severe("BAT is gonna shutdown because it can't connect to the database.");
                return;
            }
            // Init the I18n module
            I18n.getString("global");
        });
    }

    @Override
    public void onDisable() {
        modules.unloadModules();
        instance = null;
    }

    public void loadDB(final Callback<Boolean> dbState) {
        final String username = config.getMysql_user();
        final String password = config.getMysql_password();
        final String database = config.getMysql_database();
        final String port = config.getMysql_port();
        final String host = config.getMysql_host();
        final String urlParameters = config.getMysql_urlParameters();
        // BoneCP can accept no database and we want to avoid that
        Preconditions.checkArgument(!"".equals(database), "You must set the database.");
        getProxy().getScheduler().runAsync(this, () -> {
            try {
                dsHandler = new DataSourceHandler(this, host, port, database, username, password, urlParameters);
                final Connection c = dsHandler.getConnection();
                if (c != null) {
                    c.close();
                    dbState.done(true, null);
                    return;
                }
            } catch (final SQLException ignored) {
            }
            getLogger().severe("The connection pool (database connection)"
                + " wasn't able to be launched !");
            dbState.done(false, null);
        });
    }

    public static BAT getInstance() {
        return BAT.instance;
    }

    public static BaseComponent[] __(final String message) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    public static void broadcast(final String message, final String perm) {
        // The IP address won't be displayed to all players (bat.broadcast.displayip)
        final String DISPLAY_IP_BROADCAST_PERM = "bat.broadcast.displayip";
        String ipInMessage = Utils.extractIpFromString(message);
        boolean containsIp = !ipInMessage.isEmpty();
        final BaseComponent[] ipIncludedMsg = __(message);
        final BaseComponent[] ipFreeMsg = containsIp ?
            __(message.replace(ipInMessage, "<hiddenIP>")) : ipIncludedMsg;

        for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            if (p.hasPermission("bat.admin")) {
                p.sendMessage(ipIncludedMsg);
            } else if (p.hasPermission(perm)) {
                p.sendMessage(p.hasPermission(DISPLAY_IP_BROADCAST_PERM) ? ipIncludedMsg : ipFreeMsg);
            }
            // If he has a grantall permission, he will have the broadcast on all the servers
            else {
                for (final String playerPerm : p.getPermissions()) {
                    if (playerPerm.startsWith("bat.grantall.")) {
                        p.sendMessage(p.hasPermission(DISPLAY_IP_BROADCAST_PERM) ? ipIncludedMsg : ipFreeMsg);
                        break;
                    }
                }
            }
        }
        getInstance().getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }

    public ModulesManager getModules() {
        return modules;
    }

    public Configuration getConfiguration() {
        return config;
    }

    public static Connection getConnection() {
        return dsHandler.getConnection();
    }

    /**
     * Kick a player from the proxy for a specified reason
     */
    public static void kick(final ProxiedPlayer player, final String reason) {
        if (reason == null || reason.equals("")) {
            player.disconnect(TextComponent.fromLegacyText("You have been disconnected of the server."));
        } else {
            player.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', reason)));
        }
    }

    /**
     * Gets the configured default server for the given {@link ProxiedPlayer}.
     *
     * @param player Player to get default server for.
     * @return Default server for the given player.
     */
    public String getDefaultServer(ProxiedPlayer player) {
        return player.getPendingConnection().getListener().getServerPriority().get(0);
    }

    /**
     * Sends the given {@link ProxiedPlayer} to the default server / the server with highest priority.
     *
     * @param player Player to send.
     */
    public void sendToDefaultServer(ProxiedPlayer player) {
        player.connect(getProxy().getServerInfo(getDefaultServer(player)));
    }
}
