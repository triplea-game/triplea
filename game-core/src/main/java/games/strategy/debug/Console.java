package games.strategy.debug;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

import games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/**
 * A 'console' window to display log messages to users.
 */
@Log
public final class Console {
  private static final ImmutableCollection<LogLevelItem> LOG_LEVEL_ITEMS = ImmutableList.of(
      new LogLevelItem("Errors and Warnings", Level.WARNING),
      new LogLevelItem("All Messages", Level.ALL));

  private final JTextArea textArea = new JTextArea(20, 50);
  private final JFrame frame = new JFrame("TripleA Console");
  private Level logLevel = Level.WARNING;

  public Console() {
    setLogLevel(getDefaultLogLevel());

    ClientSetting.showConsole.addSaveListener(newValue -> {
      if (newValue.equals(String.valueOf(true))) {
        SwingUtilities.invokeLater(() -> setVisible(true));
      }
    });

    SwingComponents.addWindowClosedListener(frame, () -> ClientSetting.showConsole.saveAndFlush(false));
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
    actions.add(newLogLevelButton());
    SwingUtilities.invokeLater(frame::pack);

    if (ClientSetting.showConsole.booleanValue()) {
      SwingUtilities.invokeLater(() -> setVisible(true));
    }
  }

  private static Level getDefaultLogLevel() {
    final String logLevelName = ClientSetting.loggingVerbosity.value();
    try {
      return Level.parse(logLevelName);
    } catch (final IllegalArgumentException e) {
      log.warning("Client setting " + ClientSetting.loggingVerbosity + " contains malformed log level ("
          + logLevelName + "); defaulting to WARNING");
      return Level.WARNING;
    }
  }

  private void setLogLevel(final Level level) {
    logLevel = level;
    LogManager.getLogManager().getLogger("").setLevel(attenuateLogLevel(level));
  }

  private static Level attenuateLogLevel(final Level level) {
    return (level.intValue() < Level.INFO.intValue()) ? Level.INFO : level;
  }

  private AbstractButton newLogLevelButton() {
    final JToggleButton button = new JToggleButton("Log Level ▼");
    button.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        createAndShowLogLevelMenu((JComponent) e.getSource(), button);
      }
    });
    return button;
  }

  private void createAndShowLogLevelMenu(final JComponent component, final AbstractButton button) {
    final JPopupMenu menu = new JPopupMenu();
    menu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {}

      @Override
      public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
        button.setSelected(false);
      }

      @Override
      public void popupMenuCanceled(final PopupMenuEvent e) {
        button.setSelected(false);
      }
    });
    LOG_LEVEL_ITEMS.forEach(item -> {
      final JMenuItem menuItem = new JRadioButtonMenuItem(item.label, item.level.equals(logLevel));
      menuItem.addActionListener(e -> {
        setLogLevel(item.level);
        setDefaultLogLevel(item.level);
        appendLn("Log level updated to: " + item.level);
      });
      menu.add(menuItem);
    });
    menu.show(component, 0, component.getHeight());
  }

  private static void setDefaultLogLevel(final Level level) {
    ClientSetting.loggingVerbosity.saveAndFlush(level.getName());
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

  @AllArgsConstructor
  private static final class LogLevelItem {
    final String label;
    final Level level;
  }
}
