package games.strategy.engine.lobby.server.userDB;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.lobby.server.LobbyContext;
import games.strategy.util.ThreadUtil;

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
  private static final Logger s_logger = Logger.getLogger(Database.class.getName());
  private static final Object s_dbSetupLock = new Object();
  private static boolean s_isDbSetup = false;
  private static boolean s_areDBTablesCreated = false;

  private static File getCurrentDataBaseDir() {
    final File dbRootDir = getDBRoot();
    final File dbDir = new File(dbRootDir, "current");
    if (!dbDir.exists()) {
      if (!dbDir.mkdirs()) {
        throw new IllegalStateException("Could not create derby dir");
      }
    }
    return dbDir;
  }

  private static File getDBRoot() {
    final File root;
    if (System.getProperties().containsKey(ServerLauncher.SERVER_ROOT_DIR_PROPERTY)) {
      root = new File(System.getProperties().getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY));
    } else {
      root = ClientFileSystemHelper.getRootFolder();
    }
    if (!root.exists()) {
      throw new IllegalStateException("Root dir does not exist");
    }
    return new File(root, "derby_db");
  }

  public static Connection getDerbyConnection() {
    final String url = "jdbc:derby:ta_users;create=true";
    return getConnection(url, getDbProps());
  }

  public static Connection getPostgresConnection() {
    final Connection connection = getConnection("jdbc:postgresql://localhost/ta_users", getPostgresDbProps());
    try {
      connection.setAutoCommit(false);
    } catch (final SQLException e) {
      throw new RuntimeException("could not set autocommit to false on DB connection, connection not there?", e);
    }
    return connection;
  }

  public static Connection getConnection(final String url, final Properties props) {
    ensureDbIsSetup();
    final Connection conn;
    /*
     * The connection specifies create=true to cause
     * the database to be created. To remove the database,
     * remove the directory derbyDB and its contents.
     * The directory derbyDB will be created under
     * the directory that the system property
     * derby.system.home points to, or the current
     * directory if derby.system.home is not set.
     */
    try {
      conn = DriverManager.getConnection(url, props);
    } catch (final SQLException e) {
      throw new IllegalStateException("Could not create db connection", e);
    }
    ensureDbTablesAreCreated(conn);
    return conn;
  }

  /**
   * The connection passed in to this method is not closed, except in case of error.
   */
  private static void ensureDbTablesAreCreated(final Connection conn) {
    synchronized (s_dbSetupLock) {
      try {
        if (s_areDBTablesCreated) {
          return;
        }
        final ResultSet rs = conn.getMetaData().getTables(null, null, null, null);
        final List<String> existing = new ArrayList<>();
        while (rs.next()) {
          existing.add(rs.getString("TABLE_NAME").toUpperCase());
        }
        rs.close();
        if (!existing.contains("TA_USERS")) {
          final Statement s = conn.createStatement();
          s.execute("create table ta_users" + "(" + "userName varchar(40) NOT NULL PRIMARY KEY, "
              + "password varchar(40) NOT NULL, " + "email varchar(40) NOT NULL, " + "joined timestamp NOT NULL, "
              + "lastLogin timestamp NOT NULL, " + "admin integer NOT NULL " + ")");
          s.close();
        }
        if (!existing.contains("BANNED_USERNAMES")) {
          final Statement s = conn.createStatement();
          s.execute("create table banned_usernames" + "(" + "username varchar(40) NOT NULL PRIMARY KEY, "
              + "ban_till timestamp  " + ")");
          s.close();
        }
        if (!existing.contains("BANNED_IPS")) {
          final Statement s = conn.createStatement();
          s.execute(
              "create table banned_ips" + "(" + "ip varchar(40) NOT NULL PRIMARY KEY, " + "ban_till timestamp  " + ")");
          s.close();
        }
        if (!existing.contains("BANNED_MACS")) {
          final Statement s = conn.createStatement();
          s.execute("create table banned_macs" + "(" + "mac varchar(40) NOT NULL PRIMARY KEY, " + "ban_till timestamp  "
              + ")");
          s.close();
        }
        if (!existing.contains("MUTED_USERNAMES")) {
          final Statement s = conn.createStatement();
          s.execute("create table muted_usernames" + "(" + "username varchar(40) NOT NULL PRIMARY KEY, "
              + "mute_till timestamp  " + ")");
          s.close();
        }
        if (!existing.contains("MUTED_IPS")) {
          final Statement s = conn.createStatement();
          s.execute(
              "create table muted_ips" + "(" + "ip varchar(40) NOT NULL PRIMARY KEY, " + "mute_till timestamp  " + ")");
          s.close();
        }
        if (!existing.contains("MUTED_MACS")) {
          final Statement s = conn.createStatement();
          s.execute("create table muted_macs" + "(" + "mac varchar(40) NOT NULL PRIMARY KEY, " + "mute_till timestamp  "
              + ")");
          s.close();
        }
        if (!existing.contains("BAD_WORDS")) {
          final Statement s = conn.createStatement();
          s.execute("create table bad_words" + "(" + "word varchar(40) NOT NULL PRIMARY KEY " + ")");
          s.close();
        }
        s_areDBTablesCreated = true;
      } catch (final SQLException sqle) {
        // only close if an error occurs
        try {
          conn.close();
        } catch (final SQLException e) {
          // ignore close errors
        }
        s_logger.log(Level.SEVERE, sqle.getMessage(), sqle);
        throw new IllegalStateException("Could not create tables");
      }
    }
  }

  /**
   * Set up folders and environment variables for database.
   */
  private static void ensureDbIsSetup() {
    synchronized (s_dbSetupLock) {
      if (s_isDbSetup) {
        return;
      }
      // setup the derby location
      System.getProperties().setProperty("derby.system.home", getCurrentDataBaseDir().getAbsolutePath());
      // shut the database down on finish
      Runtime.getRuntime().addShutdownHook(new Thread(() -> shutDownDB()));
      s_isDbSetup = true;
    }
    // we want to backup the database on occassion
    final Thread backupThread = new Thread(() -> {
      while (true) {
        // wait 7 days
        if (!ThreadUtil.sleep(7 * 24 * 60 * 60 * 1000)) {
          break;
        }
        backup();
      }
    }, "TripleA Database Backup Thread");
    backupThread.setDaemon(true);
    backupThread.start();
  }

  private static Properties getDbProps() {
    final Properties props = new Properties();
    props.put("user", "user1");
    props.put("password", "user1");
    return props;
  }

  private static Properties getPostgresDbProps() {
    final Properties props = new Properties();
    props.put("user", LobbyContext.lobbyPropertyReader().getPostgresUser());
    props.put("password", LobbyContext.lobbyPropertyReader().getPostgresPassword());
    return props;
  }


  static void backup() {
    final String backupDirName =
        "backup_at_" + DateTimeFormatter.ofPattern("yyyy_MM_dd__kk_mm_ss").format(LocalDateTime.now());
    final File backupRootDir = getBackupDir();
    final File backupDir = new File(backupRootDir, backupDirName);
    if (!backupDir.mkdirs()) {
      s_logger.severe("Could not create backup dir" + backupDirName);
      return;
    }
    s_logger.log(Level.INFO, "Backing up database to " + backupDir.getAbsolutePath());
    try (final Connection con = getDerbyConnection()) {
      // http://www-128.ibm.com/developerworks/db2/library/techarticle/dm-0502thalamati/
      final String sqlstmt = "CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)";
      final CallableStatement cs = con.prepareCall(sqlstmt);
      cs.setString(1, backupDir.getAbsolutePath());
      cs.execute();
      cs.close();
    } catch (final Exception e) {
      s_logger.log(Level.SEVERE, "Could not back up database", e);
    }
    s_logger.log(Level.INFO, "Done backing up database");
  }

  private static File getBackupDir() {
    return new File(getDBRoot(), "backups");
  }

  private static void shutDownDB() {
    try {
      DriverManager.getConnection("jdbc:derby:ta_users;shutdown=true");
    } catch (final SQLException se) {
      if (se.getErrorCode() != 45000) {
        s_logger.log(Level.WARNING, se.getMessage(), se);
      }
    }
    s_logger.info("Databse shut down");
  }
}
