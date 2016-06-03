package games.strategy.engine.framework.headlessGameServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import games.strategy.util.ThreadUtil;

/**
 * Terminal line console.
 */
public class HeadlessGameServerConsole {
  protected static final int LOOP_SLEEP_MS = 20;

  private final PrintStream out;
  private final BufferedReader in;
  private final HeadlessConsoleController commandController;
  private boolean shutdown = false;

  public HeadlessGameServerConsole(final HeadlessGameServer server, final InputStream in, final PrintStream out) {
    this(new BufferedReader(new InputStreamReader(in)), out, new HeadlessConsoleController(server, in, out));
  }

  protected HeadlessGameServerConsole(final BufferedReader in, final PrintStream out,
      final HeadlessConsoleController commandController) {
    this.out = out;
    this.in = in;
    this.commandController = commandController;
  }

  public void start() {
    final Thread t = new Thread(() -> {
      printEvalLoop();
    }, "Headless console eval print loop");
    t.setDaemon(true);
    t.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      out.println("Shutting Down.   See log file.");
    }));
  }

  private void printEvalLoop() {
    out.println();
    while (!shutdown) {
      out.print(">>>>");
      out.flush();
      try {
        final String command = in.readLine();
        if (command != null && !command.trim().isEmpty()) {
          commandController.process(command.trim());
        }
      } catch (final Throwable t) {
        out.println("Error: " + t.getMessage());
        t.printStackTrace(out);
      }

      ThreadUtil.sleep(LOOP_SLEEP_MS);
    }
  }

  protected void shutdown() {
    this.shutdown = true;
  }
}
