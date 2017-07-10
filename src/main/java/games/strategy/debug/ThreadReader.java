package games.strategy.debug;

import javax.swing.JTextArea;

import games.strategy.util.ThreadUtil;

class ThreadReader implements Runnable {
  private static final int CONSOLE_UPDATE_INTERVAL_MS = 100;
  private final JTextArea m_text;
  private final SynchedByteArrayOutputStream m_in;
  private final boolean m_displayConsoleOnWrite;
  private final GenericConsole parentConsole;

  ThreadReader(final SynchedByteArrayOutputStream in, final JTextArea text, final boolean displayConsoleOnWrite,
      final GenericConsole parentConsole) {
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
      if (!ThreadUtil.sleep(CONSOLE_UPDATE_INTERVAL_MS)) {
        break;
      }
    }
  }
}
