package games.strategy.debug;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import games.strategy.ui.SwingAction;
import games.strategy.util.ThreadUtil;

public abstract class GenericConsole extends JFrame {
  private static final long serialVersionUID = 5754914217052820386L;

  private final JTextArea m_text = new JTextArea(20, 50);
  private final JToolBar m_actions = new JToolBar(SwingConstants.HORIZONTAL);

  public GenericConsole(String title) {
    super(title);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    m_text.setLineWrap(true);
    m_text.setWrapStyleWord(true);
    final JScrollPane scroll = new JScrollPane(m_text);
    getContentPane().add(scroll, BorderLayout.CENTER);
    getContentPane().add(m_actions, BorderLayout.SOUTH);
    m_actions.setFloatable(false);
    m_actions.add(m_threadDiagnoseAction);
    m_actions.add(m_memoryAction);
    m_actions.add(m_propertiesAction);
    m_actions.add(m_copyAction);
    m_actions.add(m_clearAction);
    SwingUtilities.invokeLater(() -> pack());
  }

  public abstract GenericConsole getConsoleInstance();

  public void append(final String s) {
    m_text.append(s);
  }

  public void clear() {
    m_text.setText("");
  }

  public void dumpStacks() {
    m_threadDiagnoseAction.actionPerformed(null);
  }

  public String getText() {
    return m_text.getText();
  }

  /**
   * Displays standard error to the console
   */
  public void displayStandardError() {
    final SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.err);
    final ThreadReader reader = new ThreadReader(out, m_text, true, getConsoleInstance());
    final Thread thread = new Thread(reader, "Console std err reader");
    thread.setDaemon(true);
    thread.start();
    final PrintStream print = new PrintStream(out);
    System.setErr(print);
  }

  public void displayStandardOutput() {
    final SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.out);
    final ThreadReader reader = new ThreadReader(out, m_text, false, getConsoleInstance());
    final Thread thread = new Thread(reader, "Console std out reader");
    thread.setDaemon(true);
    thread.start();
    final PrintStream print = new PrintStream(out);
    System.setOut(print);
  }

  private final Action m_copyAction = SwingAction.of("Copy to clipboard", e -> {
    final String text = m_text.getText();
    final StringSelection select = new StringSelection(text);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
  });

  private final AbstractAction m_threadDiagnoseAction =
      SwingAction.of("Enumerate Threads", e -> System.out.println(DebugUtils.getThreadDumps()));
  private final AbstractAction m_memoryAction = SwingAction.of("Memory", e -> append(DebugUtils.getMemory()));
  private final AbstractAction m_propertiesAction =
      SwingAction.of("Properties", e -> append(DebugUtils.getProperties()));
  private final AbstractAction m_clearAction = SwingAction.of("Clear", e -> clear());
}


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


/**
 * Allows data written to a byte output stream to be read
 * safely friom a seperate thread.
 * Only readFully() is currently threadSafe for reading.
 */
class SynchedByteArrayOutputStream extends ByteArrayOutputStream {
  private final Object lock = new Object();
  private final PrintStream m_mirror;

  SynchedByteArrayOutputStream(final PrintStream mirror) {
    m_mirror = mirror;
  }

  public void write(final byte b) throws IOException {
    synchronized (lock) {
      m_mirror.write(b);
      super.write(b);
      lock.notifyAll();
    }
  }

  @Override
  public void write(final byte[] b, final int off, final int len) {
    synchronized (lock) {
      super.write(b, off, len);
      m_mirror.write(b, off, len);
      lock.notifyAll();
    }
  }

  /**
   * Read all data written to the stream.
   * Blocks until data is available.
   * This is currently the only threadsafe method for reading.
   */
  public String readFully() {
    synchronized (lock) {
      if (super.size() == 0) {
        try {
          lock.wait();
        } catch (final InterruptedException ie) {
        }
      }
      final String s = toString();
      reset();
      return s;
    }
  }
}
