package games.strategy.debug;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import lombok.extern.java.Log;
import swinglib.JComboBoxBuilder;

/**
 * A 'console' window to display log messages to users.
 */
@Log
public class Console {
  private final JTextArea textArea = new JTextArea(20, 50);

  private final JFrame frame = new JFrame("TripleA Console");

  public Console() {
    final Level logLevel = ClientSetting.LOGGING_VERBOSITY.value().equals(Level.ALL.getName())
        ? Level.INFO
        : Level.WARNING;
    LogManager.getLogManager().getLogger("").setLevel(logLevel);

    ClientSetting.SHOW_CONSOLE.addSaveListener(newValue -> {
      if (newValue.equals(String.valueOf(true))) {
        SwingUtilities.invokeLater(() -> setVisible(true));
      }
    });

    SwingComponents.addWindowClosedListener(frame, () -> ClientSetting.SHOW_CONSOLE.saveAndFlush(false));
    LookAndFeelSwingFrameListener.register(frame);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
    final JToolBar actions = new JToolBar(SwingConstants.HORIZONTAL);
    frame.getContentPane().add(actions, BorderLayout.SOUTH);
    actions.setFloatable(false);
    final Action threadDiagnoseAction =
        SwingAction.of("Enumerate Threads", e -> log.info(DebugUtils.getThreadDumps()));
    actions.add(threadDiagnoseAction);
    actions.add(SwingAction.of("Memory", e -> append(DebugUtils.getMemory())));
    actions.add(SwingAction.of("Properties", e -> append(DebugUtils.getProperties())));
    actions.add(SwingAction.of("Copy to clipboard", e -> {
      final String text = textArea.getText();
      final StringSelection select = new StringSelection(text);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
    }));
    actions.add(SwingAction.of("Clear", e -> textArea.setText("")));
    actions.add(
        JComboBoxBuilder.builder()
            .menuOption(Level.WARNING.getName())
            .menuOption(Level.ALL.getName())
            .useLastSelectionAsFutureDefault(ClientSetting.LOGGING_VERBOSITY)
            .itemListener(this::reportLogLevel)
            .toolTip("Sets logging verbosity, whether to display all messages or just errors and warnings")
            .build());
    SwingUtilities.invokeLater(frame::pack);

    if (ClientSetting.SHOW_CONSOLE.booleanValue()) {
      SwingUtilities.invokeLater(() -> setVisible(true));
    }
  }

  private void reportLogLevel(final String level) {
    LogManager.getLogManager().getLogger("")
        .setLevel(level.equals(Level.ALL.getName()) ? Level.INFO : Level.WARNING);
    appendLn("Log level updated to: " + level);
  }

  public void setVisible(final boolean visible) {
    frame.setVisible(visible);
  }

  public void append(final String s) {
    SwingUtilities.invokeLater(() -> textArea.append(s));
  }

  public void appendLn(final String s) {
    append(s + "\n");
  }

}
