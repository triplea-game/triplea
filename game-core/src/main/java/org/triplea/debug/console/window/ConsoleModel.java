package org.triplea.debug.console.window;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;
import org.triplea.swing.Toast;

/** View-model for console window. */
@Log
@Builder
class ConsoleModel {
  private final Consumer<String> clipboardAction;

  void copyToClipboardAction(final ConsoleView consoleView) {
    clipboardAction.accept(consoleView.readText());
  }

  static void setVisibility(final ConsoleView consoleView) {
    // Show console window automatically when the user setting is toggled on.
    ClientSetting.showConsole.addListener(
        gameSetting -> {
          if (gameSetting.getValueOrThrow()) {
            consoleView.setVisible();
          }
        });

    if (ClientSetting.showConsole.getValue().orElse(false)) {
      consoleView.setVisible();
    }

    // if the console is closed by user manually, keep it closed and do not show it again on
    // startup.
    consoleView.addWindowClosedListener(() -> ClientSetting.showConsole.setValueAndFlush(false));
  }

  static String getCurrentLogLevel() {
    return ClientSetting.loggingVerbosity
        .getValue()
        .map(Level::parse)
        .map(LogLevelItem::fromLevel)
        .orElse(LogLevelItem.NORMAL.label);
  }

  static Collection<String> getLogLevelOptions() {
    return Stream.of(LogLevelItem.values())
        .map(logLevelSelection -> logLevelSelection.label)
        .collect(Collectors.toList());
  }

  static void memoryAction(final ConsoleView consoleView) {
    consoleView.append(DebugUtils.getMemory());
  }

  static void propertiesAction(final ConsoleView consoleView) {
    consoleView.append(DebugUtils.getProperties());
  }

  static void clearAction(final ConsoleView consoleView) {
    consoleView.setText("");
  }

  static void enumerateThreadsAction(final ConsoleView consoleView) {
    consoleView.append(DebugUtils.getThreadDumps());
  }

  /** Represents the mapping between user-friendly label and log level. */
  @VisibleForTesting
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @ToString
  @Getter(AccessLevel.PACKAGE)
  static final class LogLevelItem {
    static final LogLevelItem DEBUG = new LogLevelItem("Debug Logging", Level.ALL);

    static final LogLevelItem NORMAL = new LogLevelItem("Normal Logging", Level.INFO);

    private final String label;
    private final Level level;

    static LogLevelItem[] values() {
      return new LogLevelItem[] {DEBUG, NORMAL};
    }

    static Level fromLabel(final String label) {
      return label.equals(DEBUG.label) ? DEBUG.level : NORMAL.level;
    }

    static String fromLevel(final Level level) {
      return level.equals(DEBUG.level) ? DEBUG.label : NORMAL.label;
    }
  }

  static void setLogLevel(final String selectedLevel) {
    final Level level = LogLevelItem.fromLabel(selectedLevel);
    ClientSetting.loggingVerbosity.setValueAndFlush(level.getName());
    Toast.showToast("Log level set to: " + level.getName());
  }
}
