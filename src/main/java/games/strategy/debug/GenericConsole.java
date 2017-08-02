package games.strategy.debug;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
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

public abstract class GenericConsole extends JFrame {
  private static final long serialVersionUID = 5754914217052820386L;

  private final JTextArea m_text = new JTextArea(20, 50);

  public GenericConsole(final String title) {
    super(title);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    m_text.setLineWrap(true);
    m_text.setWrapStyleWord(true);
    final JScrollPane scroll = new JScrollPane(m_text);
    getContentPane().add(scroll, BorderLayout.CENTER);
    final JToolBar actions = new JToolBar(SwingConstants.HORIZONTAL);
    getContentPane().add(actions, BorderLayout.SOUTH);
    actions.setFloatable(false);
    actions.add(m_threadDiagnoseAction);
    final AbstractAction memoryAction = SwingAction.of("Memory", e -> append(DebugUtils.getMemory()));
    actions.add(memoryAction);
    final AbstractAction propertiesAction = SwingAction.of("Properties", e -> append(DebugUtils.getProperties()));
    actions.add(propertiesAction);
    final Action copyAction = SwingAction.of("Copy to clipboard", e -> {
      final String text = m_text.getText();
      final StringSelection select = new StringSelection(text);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
    });
    actions.add(copyAction);
    final AbstractAction clearAction = SwingAction.of("Clear", e -> clear());
    actions.add(clearAction);
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
   * Displays standard error to the console.
   */
  protected void displayStandardError() {
    final SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.err);
    final ThreadReader reader = new ThreadReader(out, m_text, true, getConsoleInstance());
    final Thread thread = new Thread(reader, "Console std err reader");
    thread.setDaemon(true);
    thread.start();
    final PrintStream print = new PrintStream(out);
    System.setErr(print);
  }

  protected void displayStandardOutput() {
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


