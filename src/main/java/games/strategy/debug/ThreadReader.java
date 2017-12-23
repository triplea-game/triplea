package games.strategy.debug;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.BooleanSupplier;

import javax.swing.JTextArea;

import games.strategy.util.ThreadUtil;

final class ThreadReader implements Runnable {
  private static final int CONSOLE_UPDATE_INTERVAL_MS = 100;
  private final JTextArea text;
  private final ByteArrayOutputStream src;
  private final BooleanSupplier displayConsoleOnWriteSupplier;
  private final GenericConsole parentConsole;
  private final PrintStream out;

  ThreadReader(
      final PrintStream out,
      final ByteArrayOutputStream src,
      final JTextArea text,
      final BooleanSupplier displayConsoleOnWriteSupplier,
      final GenericConsole parentConsole) {
    this.out = out;
    this.src = src;
    this.text = text;
    this.displayConsoleOnWriteSupplier = displayConsoleOnWriteSupplier;
    this.parentConsole = parentConsole;
  }

  @Override
  public void run() {
    while (true) {
      text.append(nextInput());
      if (displayConsoleOnWriteSupplier.getAsBoolean()) {
        parentConsole.setVisible(true);
      }
      if (!ThreadUtil.sleep(CONSOLE_UPDATE_INTERVAL_MS)) {
        break;
      }
    }
  }

  private String nextInput() {
    while (src.size() == 0) {
      ThreadUtil.sleep(100);
    }
    final String result = new String(src.toByteArray());
    src.reset();
    out.print(result);
    return result;
  }
}
