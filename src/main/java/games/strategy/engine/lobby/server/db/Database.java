package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.lobby.server.LobbyContext;

/**
 * Utility to get connections to the database.
 *
 * <p>
 * The database is embedded within the jvm.
 * </p>
 *
 * <p>
 * Getting a connection will cause the database (and the neccessary tables) to be created if it does not already exist.
 * </p>
 *
 * <p>
 * The database will be shutdown on System.exit through a shutdown hook.
 * </p>
 *
 * <p>
 * Getting a connection will also schedule backups at regular intervals.
 * </p>
 */
public class Database {
  private static final Logger logger = Logger.getLogger(Database.class.getName());
  private static final Object dbSetupLock = new Object();
  private static final boolean isDbSetup = false;
  private static boolean areDbTablesCreated = false;

  public static Connection getPostgresConnection() {
    final Connection connection = getConnection("jdbc:postgresql://localhost/ta_users", getPostgresDbProps());
    try {
      connection.setAutoCommit(false);
    } catch (final SQLException e) {
      throw new RuntimeException("could not set autocommit to false on DB connection, connection not there?", e);
    }
    return connection;
  }

  private static Properties getPostgresDbProps() {
    final Properties props = new Properties();
    props.put("user", LobbyContext.lobbyPropertyReader().getPostgresUser());
    props.put("password", LobbyContext.lobbyPropertyReader().getPostgresPassword());
    return props;
  }

  public static Connection getConnection(final String url, final Properties props) {
    final Connection conn;
    try {
      conn = DriverManager.getConnection(url, props);
    } catch (final SQLException e) {
      throw new IllegalStateException("Could not create db connection", e);
    }
//    ensureDbTablesAreCreated(conn);
    return conn;
  }

  /**
   * The connection passed in to this method is not closed, except in case of error.
   */
  private static void ensureDbTablesAreCreated(final Connection conn) {
    synchronized (dbSetupLock) {
      try {
        if (areDbTablesCreated) {
          return;
        }

        final List<String> existing = new ArrayList<>();
        try (final ResultSet rs = conn.getMetaData().getTables(null, null, null, null)) {
          while (rs.next()) {
            existing.add(rs.getString("TABLE_NAME").toUpperCase());
          }
        }

        if (!existing.contains("TA_USERS")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table ta_users" + "(" + "userName varchar(40) NOT NULL PRIMARY KEY, "
                + "password varchar(40) NOT NULL, " + "email varchar(40) NOT NULL, " + "joined timestamp NOT NULL, "
                + "lastLogin timestamp NOT NULL, " + "admin integer NOT NULL " + ")");
          }
        }
        if (!existing.contains("BANNED_USERNAMES")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table banned_usernames" + "(" + "username varchar(40) NOT NULL PRIMARY KEY, "
                + "ban_till timestamp  " + ")");
          }
        }
        if (!existing.contains("BANNED_IPS")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table banned_ips" + "(" + "ip varchar(40) NOT NULL PRIMARY KEY, "
                + "ban_till timestamp  " + ")");
          }
        }
        if (!existing.contains("BANNED_MACS")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table banned_macs" + "(" + "mac varchar(40) NOT NULL PRIMARY KEY, "
                + "ban_till timestamp  " + ")");
          }
        }
        if (!existing.contains("MUTED_USERNAMES")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table muted_usernames" + "(" + "username varchar(40) NOT NULL PRIMARY KEY, "
                + "mute_till timestamp  " + ")");
          }
        }
        if (!existing.contains("MUTED_IPS")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table muted_ips" + "(" + "ip varchar(40) NOT NULL PRIMARY KEY, "
                + "mute_till timestamp  " + ")");
          }
        }
        if (!existing.contains("MUTED_MACS")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table muted_macs" + "(" + "mac varchar(40) NOT NULL PRIMARY KEY, "
                + "mute_till timestamp  " + ")");
          }
        }
        if (!existing.contains("BAD_WORDS")) {
          try (final Statement s = conn.createStatement()) {
            s.execute("create table bad_words" + "(" + "word varchar(40) NOT NULL PRIMARY KEY " + ")");
          }
        }
        areDbTablesCreated = true;
      } catch (final SQLException sqle) {
        // only close if an error occurs
        try {
          conn.close();
        } catch (final SQLException e) {
          // ignore close errors
        }
        logger.log(Level.SEVERE, sqle.getMessage(), sqle);
        throw new IllegalStateException("Could not create tables");
      }
    }
  }
}
