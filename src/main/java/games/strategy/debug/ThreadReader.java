package games.strategy.debug;

import java.util.function.BooleanSupplier;

import javax.swing.JTextArea;

import games.strategy.util.ThreadUtil;

final class ThreadReader implements Runnable {
  private static final int CONSOLE_UPDATE_INTERVAL_MS = 100;
  private final JTextArea text;
  private final SynchedByteArrayOutputStream in;
  private final BooleanSupplier displayConsoleOnWriteSupplier;
  private final GenericConsole parentConsole;

  ThreadReader(
      final SynchedByteArrayOutputStream in,
      final JTextArea text,
      final BooleanSupplier displayConsoleOnWriteSupplier,
      final GenericConsole parentConsole) {
    this.in = in;
    this.text = text;
    this.displayConsoleOnWriteSupplier = displayConsoleOnWriteSupplier;
    this.parentConsole = parentConsole;
  }

  @Override
  public void run() {
    while (true) {
      text.append(in.readFully());
      if (displayConsoleOnWriteSupplier.getAsBoolean() && !parentConsole.isVisible()) {
        parentConsole.setVisible(true);
      }
      if (!ThreadUtil.sleep(CONSOLE_UPDATE_INTERVAL_MS)) {
        break;
      }
    }
  }
}
