package fr.Alphart.BAT.Modules.Kick;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import fr.Alphart.BAT.I18n.I18n;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.BATCommand;
import fr.Alphart.BAT.Modules.IModule;
import fr.Alphart.BAT.Modules.ModuleConfiguration;
import fr.Alphart.BAT.Modules.Core.Core;
import fr.Alphart.BAT.database.DataSourceHandler;
import fr.Alphart.BAT.database.SQLQueries;

public class Kick implements IModule {
    private final BAT plugin;
    private final String name = "kick";
    private KickCommand commandHandler;
    private final KickConfig config;

    public Kick(BAT plugin) {
        this.plugin = plugin;
        config = new KickConfig();
    }

    @Override
    public List<BATCommand> getCommands() {
        return commandHandler.getCmds();
    }

    @Override
    public String getMainCommand() {
        return "kick";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ModuleConfiguration getConfig() {
        return config;
    }

    @Override
    public boolean load() {
        // Init table
        Statement statement = null;
        try (Connection conn = BAT.getConnection()) {
            statement = conn.createStatement();
            statement.executeUpdate(SQLQueries.Kick.createTable);
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

        // Register commands
        commandHandler = new KickCommand(this);
        commandHandler.loadCmds();

        return true;
    }

    @Override
    public boolean unload() {

        return false;
    }

    public class KickConfig extends ModuleConfiguration {
        public KickConfig() {
            init(name);
        }
    }

    /**
     * Kicks the given {@link ProxiedPlayer} and sends to the default server.
     *
     * @param player Player to kick.
     * @param reason Reason for kicking.
     */
    public String kick(ProxiedPlayer player, String staff, String reason) {
        plugin.sendToDefaultServer(player);
        player.sendMessage(TextComponent.fromLegacyText(I18n.tr_("wasKickedNotif", new String[]{reason})));
        return kickSQL(Core.getUUID(player.getName()), player.getServer().getInfo().getName(), staff, reason);
    }

    public String kickSQL(final String pUUID, final String server, final String staff, final String reason) {
        PreparedStatement statement = null;
        try (Connection conn = BAT.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
            statement.setString(1, pUUID);
            statement.setString(2, staff);
            statement.setString(3, reason);
            statement.setString(4, server);
            statement.executeUpdate();
            statement.close();

            return I18n.tr_("kickBroadcast", new String[]{Core.getPlayerName(pUUID), staff, server, reason});
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }

    /**
     * Kicks the given {@link ProxiedPlayer} from the network.
     *
     * @param player Player to kick.
     * @param reason Reason for kicking.
     */
    public String gKick(ProxiedPlayer player, String staff, String reason) {
        final String message = gKickSQL(Core.getUUID(player.getName()), staff, reason);
        player.disconnect(TextComponent.fromLegacyText(I18n.tr_("wasKickedNotif", new String[]{reason})));
        return message;
    }

    public String gKickSQL(final String pUUID, final String staff, final String reason) {
        PreparedStatement statement = null;
        try (Connection conn = BAT.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
            statement.setString(1, pUUID);
            statement.setString(2, staff);
            statement.setString(3, reason);
            statement.setString(4, GLOBAL_SERVER);
            statement.executeUpdate();
            statement.close();

            return I18n.tr_("gKickBroadcast", new String[]{Core.getPlayerName(pUUID), staff, reason});
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }

    /**
     * Get all kick data of a player <br>
     * <b>Should be runned async to optimize performance</b>
     *
     * @param pName 's name
     * @return List of KickEntry of the player
     */
    public List<KickEntry> getKickData(final String pName) {
        final List<KickEntry> kickList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BAT.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Kick.getKick);
            statement.setString(1, Core.getUUID(pName));
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final String server = resultSet.getString("kick_server");
                String reason = resultSet.getString("kick_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                final String staff = resultSet.getString("kick_staff");
                final Timestamp date;
                date = resultSet.getTimestamp("kick_date");
                kickList.add(new KickEntry(pName, server, reason, staff, date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return kickList;
    }

    public List<KickEntry> getManagedKick(final String staff) {
        final List<KickEntry> kickList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BAT.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Kick.getManagedKick);
            statement.setString(1, staff);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final String server = resultSet.getString("kick_server");
                String reason = resultSet.getString("kick_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                String pName = Core.getPlayerName(resultSet.getString("UUID"));
                if (pName == null) {
                    pName = "UUID:" + resultSet.getString("UUID");
                }
                final Timestamp date;
                date = resultSet.getTimestamp("kick_date");
                kickList.add(new KickEntry(pName, server, reason, staff, date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return kickList;
    }
}
