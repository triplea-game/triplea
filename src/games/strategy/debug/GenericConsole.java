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

  public GenericConsole(String title) {
    super(title);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    m_text.setLineWrap(true);
    m_text.setWrapStyleWord(true);
    final JScrollPane scroll = new JScrollPane(m_text);
    getContentPane().add(scroll, BorderLayout.CENTER);
    JToolBar m_actions = new JToolBar(SwingConstants.HORIZONTAL);
    getContentPane().add(m_actions, BorderLayout.SOUTH);
    m_actions.setFloatable(false);
    m_actions.add(m_threadDiagnoseAction);
    AbstractAction m_memoryAction = SwingAction.of("Memory", e -> append(DebugUtils.getMemory()));
    m_actions.add(m_memoryAction);
    AbstractAction m_propertiesAction = SwingAction.of("Properties", e -> append(DebugUtils.getProperties()));
    m_actions.add(m_propertiesAction);
    Action m_copyAction = SwingAction.of("Copy to clipboard", e -> {
      final String text = m_text.getText();
      final StringSelection select = new StringSelection(text);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
    });
    m_actions.add(m_copyAction);
    AbstractAction m_clearAction = SwingAction.of("Clear", e -> clear());
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

  private final AbstractAction m_threadDiagnoseAction =
      SwingAction.of("Enumerate Threads", e -> System.out.println(DebugUtils.getThreadDumps()));
}


