package games.strategy.engine.lobby.server.headless;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicInteger;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.DebugUtils;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.ui.DBExplorerPanel;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.util.TimeManager;

/**
 * Headless admin console for the lobby.
 * Reads commands from stdin, and writes responses to stdout.
 */
public class HeadlessLobbyConsole {
  private final LobbyServer server;
  private final PrintStream out;
  private final BufferedReader in;
  private final String startDate = TimeManager.getGMTString(new Date());
  private final AtomicInteger totalLogins = new AtomicInteger();
  private final AtomicInteger currentConnections = new AtomicInteger();
  private volatile int maxConcurrentLogins = 0;

  public HeadlessLobbyConsole(final LobbyServer server, final InputStream in, final PrintStream out) {
    this.out = out;
    this.in = new BufferedReader(new InputStreamReader(in));
    this.server = server;
    server.getMessenger().addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionAdded(final INode to) {
        currentConnections.incrementAndGet();
        totalLogins.incrementAndGet();
        // not strictly thread safe, but good enough
        maxConcurrentLogins = Math.max(maxConcurrentLogins, currentConnections.get());
      }

      @Override
      public void connectionRemoved(final INode to) {
        currentConnections.decrementAndGet();
      }
    });
  }

  public void start() {
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        printEvalLoop();
      }
    }, "Headless console eval print loop");
    t.start();
  }

  private void printEvalLoop() {
    out.println();
    while (true) {
      out.print(">>>>");
      out.flush();
      try {
        final String command = in.readLine();
        process(command.trim());
      } catch (final Throwable t) {
        t.printStackTrace();
        t.printStackTrace(out);
      }
    }
  }

  private void process(final String command) {
    if (command.equals("")) {
      return;
    }
    final String noun = command.split("\\s")[0];
    switch (noun) {
      case "help":
        showHelp();
        break;
      case "status":
        showStatus();
        break;
      case "sql":
        executeSql(command.substring("sql".length(), command.length()).trim());
        break;
      case "quit":
        quit();
        break;
      case "backup":
        backup();
        break;
      case "memory":
        memory();
        break;
      case "threads":
        threads();
        break;
      default:
        out.println("unrecognized command:" + command);
        showHelp();
        break;
    }
  }

  private void threads() {
    out.println(DebugUtils.getThreadDumps());
  }

  private void memory() {
    out.println(DebugUtils.getMemory());
  }

  private static void backup() {
    Database.backup();
  }

  private void quit() {
    out.println("Are you sure? (y/n)");
    try {
      final String readin = in.readLine();
      if (readin != null && readin.toLowerCase().startsWith("y")) {
        System.exit(0);
      }
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void executeSql(final String sql) {
    try (final Connection con = Database.getConnection()) {
      final Statement ps = con.createStatement();
      if (DBExplorerPanel.isNotQuery(sql)) {
        final int rs = ps.executeUpdate(sql);
        out.println("Update count:" + rs);
      } else {
        final ResultSet rs = ps.executeQuery(sql);
        print(rs);
        rs.close();
      }
    } catch (final SQLException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private void print(final ResultSet rs) {
    try (final Formatter f = new Formatter(out)) {
      final String itemFormat = "%20s ";
      f.format(itemFormat, "Count");
      final int count = rs.getMetaData().getColumnCount();
      for (int i = 1; i <= count; i++) {
        final String columnName = rs.getMetaData().getColumnName(i);
        f.format(itemFormat, columnName);
      }
      f.format("\n");
      for (int i = 0; i < count; i++) {
        f.format(itemFormat, "-----------");
      }
      f.format("\n");
      int row = 1;
      while (rs.next()) {
        f.format(itemFormat, row++);
        for (int i = 1; i <= count; i++) {
          f.format(itemFormat, rs.getString(i));
        }
        f.format("\n");
        f.flush();
      }
      // do not close, because this closes the underlying stream, which is system.out, which you should never close
      // f.close();
    } catch (final SQLException e) {
      e.printStackTrace(out);
    }
  }

  private void showStatus() {
    final int port = server.getMessenger().getServerNode().getPort();
    out.print(String.format(
        "port:%s\n" + "up since:%s\n" + "total logins:%s\n" + "current connections:%s\n"
            + "max concurrent connections:%s\n",
        port, startDate, totalLogins.get(), currentConnections.get(), maxConcurrentLogins));
  }

  private void showHelp() {
    out.println("available commands:\n" + "  backup - backup the database \n" + "  help - show this message\n"
        + "  memory - show memory usage\n" + "  status - show status information\n"
        + "  sql {sql} - execute a sql command and print the results\n" + "  threads - get thread dumps\n"
        + "  quit - quit\n");
  }
}
