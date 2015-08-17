package games.strategy.engine.framework.headlessGameServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Terminal line console.
 */
public class HeadlessGameServerConsole {
  protected static final int LOOP_SLEEP_MS = 20;

  private final PrintStream out;
  private final BufferedReader in;

  private final HeadlessConsoleController commandController;

  public HeadlessGameServerConsole(final HeadlessGameServer server, final InputStream in, final PrintStream out) {
    this(server, new BufferedReader(new InputStreamReader(in)),
        out, new HeadlessConsoleController(server, in, out));
  }

  protected HeadlessGameServerConsole(final HeadlessGameServer server, final BufferedReader in,
      final PrintStream out, final HeadlessConsoleController commandController) {
    this.out = out;
    this.in = in;
    this.commandController = commandController;
  }


  public void start() {
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        printEvalLoop();
      }
    }, "Headless console eval print loop");
    t.setDaemon(true);
    t.start();
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        // m_shutDown = true;
        out.println("Shutting Down.   See log file.");
      }
    }));
  }

  private void printEvalLoop() {
    out.println();
    while (true) {
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

      try {
        Thread.sleep(LOOP_SLEEP_MS);
      } catch (final InterruptedException e) {
        out.print("Interrupted exception: " + e);
        e.printStackTrace(out);
      }
    }
  }
}
