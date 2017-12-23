package games.strategy.debug;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.BooleanSupplier;

import javax.swing.JTextArea;

final class LogReader {
  private final JTextArea text;
  private final ListeningByteArrayOutputStream src = new ListeningByteArrayOutputStream();
  private final BooleanSupplier displayConsoleOnWriteSupplier;
  private final GenericConsole parentConsole;
  private final PrintStream out;

  LogReader(
      final PrintStream out,
      final JTextArea text,
      final BooleanSupplier displayConsoleOnWriteSupplier,
      final GenericConsole parentConsole) {
    this.out = out;
    this.text = text;
    this.displayConsoleOnWriteSupplier = displayConsoleOnWriteSupplier;
    this.parentConsole = parentConsole;
  }

  /**
   * Returns an OutputStream that triggers the Console (when enabled) when something writes to it.
   */
  OutputStream getStream() {
    return src;
  }

  private void addConsoleText() {
    text.append(new String(src.toByteArray()));
    if (displayConsoleOnWriteSupplier.getAsBoolean()) {
      parentConsole.setVisible(true);
    }
    src.reset();
  }

  private class ListeningByteArrayOutputStream extends ByteArrayOutputStream {

    @Override
    public synchronized void write(final int value) {
      super.write(value);
      out.write(value);
      addConsoleText();
    }

    @Override
    public synchronized void write(byte byteArray[], int off, int len) {
      super.write(byteArray, off, len);
      out.write(byteArray, off, len);
      addConsoleText();
    }
  }
}
