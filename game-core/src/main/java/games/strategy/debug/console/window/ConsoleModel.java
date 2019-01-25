package games.strategy.debug.console.window;

import java.util.Collection;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.SystemClipboard;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;

@Log
class ConsoleModel {

  private final ConsoleView consoleView;

  ConsoleModel(final ConsoleView consoleView) {
    this.consoleView = consoleView;

    // Show console window automatically when the user setting is toggled on.
    ClientSetting.showConsole.addListener(gameSetting -> {
      if (gameSetting.getValueOrThrow()) {
        consoleView.setVisible(true);
      }
    });

    // if the console is closed by user manually, keep it closed and do not show it again on startup.
    consoleView.addWindowClosedListener(() -> ClientSetting.showConsole.setValueAndFlush(false));
  }

  static String getCurrentLogLevel() {
    return ClientSetting.loggingVerbosity.getValue()
        .map(Level::parse)
        .map(LogLevelItem::fromLevel)
        .orElse(LogLevelItem.NORMAL.label);
  }

  static Collection<String> getLogLevelOptions() {
    return Stream.of(LogLevelItem.values())
        .map(logLevelSelection -> logLevelSelection.label)
        .collect(Collectors.toList());
  }

  void memoryAction() {
    consoleView.append(DebugUtils.getMemory());
  }

  void propertiesAction() {
    consoleView.append(DebugUtils.getProperties());
  }

  void copyToClipboardAction() {
    SystemClipboard.setClipboardContents(consoleView.readText());
  }

  void clearAction() {
    consoleView.setText("");
  }

  void enumerateThreadsAction() {
    consoleView.append(DebugUtils.getThreadDumps());
  }

  @VisibleForTesting
  @AllArgsConstructor
  @ToString
  @Getter(AccessLevel.PACKAGE)
  static class LogLevelItem {
    static final LogLevelItem DEBUG = new LogLevelItem("Debug Logging", Level.ALL);

    static final LogLevelItem NORMAL = new LogLevelItem("Normal Logging", Level.INFO);

    private final String label;
    private final Level level;

    static LogLevelItem[] values() {
      return new LogLevelItem[] {DEBUG, NORMAL};
    }

    static Level fromLabel(final String label) {
      return Stream.of(values())
          .filter(item -> item.label.equalsIgnoreCase(label))
          .findAny()
          .orElseThrow(() -> new IllegalStateException("Could not find LogLevel label: " + label)).level;
    }

    static String fromLevel(final Level level) {
      if (level.equals(DEBUG.level)) {
        return DEBUG.label;
      }
      return NORMAL.label;
    }
  }

  static void setLogLevel(final String selectedLevel) {
    final Level level = LogLevelItem.fromLabel(selectedLevel);
    ClientSetting.loggingVerbosity.setValueAndFlush(level.getName());
    log.info("Log level set to: " + level);
  }
}
