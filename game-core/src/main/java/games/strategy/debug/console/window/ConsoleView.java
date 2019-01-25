package games.strategy.debug.console.window;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import games.strategy.debug.ErrorMessageHandler;
import games.strategy.triplea.settings.ClientSetting;

/**
 * Defines the contract between console window and its view-model.
 */
public interface ConsoleView {
  String readText();

  void setText(String text);

  void setVisible(boolean visible);

  void append(final String text);

  void addWindowClosedListener(Runnable closeListener);

  static void initialize() {
    final ConsoleWindow console = new ConsoleWindow();
    final Logger defaultLogger = LogManager.getLogManager().getLogger("");
    defaultLogger.addHandler(new ErrorMessageHandler());
    defaultLogger.addHandler(new ConsoleHandler(console));

    if (ClientSetting.showConsole.getValueOrThrow()) {
      SwingUtilities.invokeLater(() -> console.setVisible(true));
    }
  }
}
