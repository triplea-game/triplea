package games.strategy.debug;

import games.strategy.util.ThreadUtil;

import javax.swing.JTextArea;

class ThreadReader implements Runnable {
  private static final int CONSOLE_UPDATE_INTERVAL_MS = 100;
  private final JTextArea m_text;
  private final SynchedByteArrayOutputStream m_in;
  private final boolean m_displayConsoleOnWrite;
  private final GenericConsole parentConsole;

  ThreadReader(final SynchedByteArrayOutputStream in, final JTextArea text, final boolean displayConsoleOnWrite,
      GenericConsole parentConsole) {
    m_in = in;
    m_text = text;
    m_displayConsoleOnWrite = displayConsoleOnWrite;
    this.parentConsole = parentConsole;
  }

  @Override
  public void run() {
    while (true) {
      m_text.append(m_in.readFully());
      if (m_displayConsoleOnWrite && !parentConsole.isVisible()) {
        parentConsole.setVisible(true);
      }
      ThreadUtil.sleep(CONSOLE_UPDATE_INTERVAL_MS);
    }
  }
}
