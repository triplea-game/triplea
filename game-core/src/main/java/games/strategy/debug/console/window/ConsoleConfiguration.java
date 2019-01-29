package games.strategy.debug.console.window;

import java.util.logging.Handler;
import java.util.logging.LogManager;

import games.strategy.ui.SystemClipboard;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Constructs console window with dependencies.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsoleConfiguration {

  private static final String DEFAULT_LOGGER = "";

  public static void initialize() {
    final ConsoleModel model = ConsoleModel.builder()
        .clipboardAction(SystemClipboard::setClipboardContents)
        .build();

    final ConsoleWindow window = new ConsoleWindow(model);
    final Handler windowHandler = new ConsoleHandler(window);

    LogManager.getLogManager().getLogger(DEFAULT_LOGGER).addHandler(windowHandler);
  }
}
