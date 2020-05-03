package fr.Alphart.BAT.database;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import net.md_5.bungee.api.ProxyServer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.varia.NullAppender;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.zaxxer.hikari.HikariDataSource;

import fr.Alphart.BAT.BAT;

public class DataSourceHandler {
	// Connection informations
	private HikariDataSource ds;
	private String username;
	private String password;
	private String database;
	private String port;
	private String host;

	/**
	 * Constructor used for MySQL
	 * 
	 * @param host
	 * @param port
	 * @param database
	 * @param username
	 * @param password
	 * @throws SQLException 
	 */
	public DataSourceHandler(final String host, final String port, final String database, final String username, final String password, final String urlParameters) throws SQLException{
		// Check database's informations and init connection
		this.host = Preconditions.checkNotNull(host);
		this.port = Preconditions.checkNotNull(port);
		this.database = Preconditions.checkNotNull(database);
		this.username = Preconditions.checkNotNull(username);
		this.password = Preconditions.checkNotNull(password);

		BAT.getInstance().getLogger().config("Initialization of HikariCP in progress ...");
		BasicConfigurator.configure(new NullAppender());
		ds = new HikariDataSource();
		String connUrl = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + 
            "?useLegacyDatetimeCode=false&characterEncoding=utf8&serverTimezone=" + TimeZone.getDefault().getID();
		if(!urlParameters.isEmpty()){
		  connUrl+= "&" + urlParameters;
		}
		ds.setJdbcUrl(connUrl);
		ds.setUsername(this.username);
		ds.setPassword(this.password);
		ds.addDataSourceProperty("cachePrepStmts", "true");
		ds.setMaximumPoolSize(8);
		try {
			final Connection conn = ds.getConnection();
		    int intOffset = Calendar.getInstance().getTimeZone().getOffset(Calendar.getInstance().getTimeInMillis()) / 1000;
		    String offset = String.format("%02d:%02d", Math.abs(intOffset / 3600), Math.abs((intOffset / 60) % 60));
		    offset = (intOffset >= 0 ? "+" : "-") + offset;
			conn.createStatement().executeQuery("SET time_zone='" + offset + "';");
			conn.close();
			BAT.getInstance().getLogger().config("BoneCP is loaded !");
		} catch (final SQLException e) {
			BAT.getInstance().getLogger().severe("BAT encounters a problem during the initialization of the database connection."
					+ " Please check your logins and database configuration.");
			if(e.getCause() instanceof CommunicationsException){
			    BAT.getInstance().getLogger().severe(e.getCause().getMessage());
			}
			if(BAT.getInstance().getConfiguration().isDebugMode()){
			    BAT.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
			}
			throw e;
		}
	}

	public Connection getConnection() {
		try {
			return ds.getConnection();
		} catch (final SQLException e) {
			BAT.getInstance().getLogger().severe(
			        "BAT can't etablish connection with the database. Please report this and include the following lines :");
			if(e.getCause() instanceof CommunicationsException){
			    BAT.getInstance().getLogger().severe(e.getCause().getMessage());
			}
            if (BAT.getInstance().getConfiguration().isDebugMode()) {
                e.printStackTrace();
            }
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
	
	public class StreamPumper{
	    private final InputStreamReader reader;
	    private List<String> pumpedLines = null;
	    
	    public StreamPumper(final InputStream is){
	        reader = new InputStreamReader(is);
	    }
	    
	    /**
	     * Starts a new async task and pump the inputstream
	     */
	    public void pump(){
	        ProxyServer.getInstance().getScheduler().runAsync(BAT.getInstance(), new Runnable() {
                @Override
                public void run() {
                    try {
                        pumpedLines = CharStreams.readLines(reader);
                        reader.close();
                    } catch (final IOException e) {
                        BAT.getInstance().getLogger().severe("BAT encounter an error while reading the stream of subprocess. Please report this :");
                        e.printStackTrace();
                    }
                }
            });
	    }
	    
	    public List<String> getLines(){
	        if(pumpedLines == null){
	            return new ArrayList<String>();
	        }
	        return pumpedLines;
	    }
	}
}
