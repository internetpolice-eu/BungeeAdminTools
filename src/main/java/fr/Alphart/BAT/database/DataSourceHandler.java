package fr.Alphart.BAT.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;

import fr.Alphart.BAT.BAT;

public class DataSourceHandler {
    private BAT plugin;
    // Connection informations
    private HikariDataSource ds;
    private String username;
    private String password;
    private String database;
    private String port;
    private String host;

    public DataSourceHandler(BAT plugin, String host, String port, String database, String username, String password,
                             String urlParameters) throws SQLException {
        this.plugin = plugin;
        // Check database's informations and init connection
        this.host = Preconditions.checkNotNull(host);
        this.port = Preconditions.checkNotNull(port);
        this.database = Preconditions.checkNotNull(database);
        this.username = Preconditions.checkNotNull(username);
        this.password = Preconditions.checkNotNull(password);

        ds = new HikariDataSource();
        String connUrl = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database +
            "?useLegacyDatetimeCode=false&characterEncoding=utf8&serverTimezone=" + TimeZone.getDefault().getID();
        if (!urlParameters.isEmpty()) {
            connUrl += "&" + urlParameters;
        }
        ds.setJdbcUrl(connUrl);
        ds.setUsername(this.username);
        ds.setPassword(this.password);
        ds.addDataSourceProperty("cachePrepStmts", "true");

        // Pool settings:
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(10);
        ds.setMaxLifetime(600000L);
        ds.setConnectionTimeout(5000L);

        try {
            final Connection conn = ds.getConnection();
            int intOffset = Calendar.getInstance().getTimeZone().getOffset(Calendar.getInstance().getTimeInMillis()) / 1000;
            String offset = String.format("%02d:%02d", Math.abs(intOffset / 3600), Math.abs((intOffset / 60) % 60));
            offset = (intOffset >= 0 ? "+" : "-") + offset;
            conn.createStatement().executeUpdate("SET time_zone='" + offset + "';");
            conn.close();
        } catch (final SQLException e) {
            plugin.getLogger().severe("BAT encounters a problem during the initialization of the database connection.");
            plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
    }

    public Connection getConnection() {
        try {
            return ds.getConnection();
        } catch (final SQLException e) {
            plugin.getLogger().severe(
                "BAT can't etablish connection with the database. Please report this and include the following lines :");
            plugin.getLogger().severe(e.getCause().getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Useful methods
    public static String handleException(final SQLException e) {
        BAT.getInstance()
            .getLogger()
            .severe("BAT encounters a problem with the database. Please report this and include the following lines :");
        e.printStackTrace();
        return "An error related to the database occured. Please check the log.";
    }

    public static void close(final AutoCloseable... closableList) {
        for (final AutoCloseable closable : closableList) {
            if (closable != null) {
                try {
                    closable.close();
                } catch (final Throwable ignored) {
                }
            }
        }
    }
}
