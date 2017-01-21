package games.strategy.triplea.ai.proAI.logging;

import java.util.logging.Level;

import games.strategy.triplea.ai.proAI.ProAI;

/**
 * Class to log messages to log window and console.
 */
public class ProLogger {

  public static void warn(final String message) {
    log(Level.WARNING, message);
  }

  public static void info(final String message) {
    log(Level.FINE, message);
  }

  public static void debug(final String message) {
    log(Level.FINER, message);
  }

  public static void trace(final String message) {
    log(Level.FINEST, message);
  }

  private static void log(final Level level, final String message) {
    log(level, message, null);
  }

  /**
   * Some notes on using the Pro AI logger:
   * First, to make the logs easily readable even when there are hundreds of lines, I want every considerable step down
   * in the call stack to
   * mean more log message indentation.
   * For example, the base logs in the Pro AI class have no indentation before them, but the base logs in the
   * DoCombatMove class will have
   * two spaces inserted at the start, and the level below that, four spaces.
   * In this way, when you're reading the log, you can skip over unimportant areas with speed because of the
   * indentation.
   * Second, I generally want the Fine logs to be messages that run less than 10 times each round, including almost all
   * messages in the Pro
   * AI class,
   * Finest for messages showing details within a method that, for example, returns a value.
   * (So, for example, the NCM_Task method IsTaskWorthwhile() would primarily use finest, as it just returns a boolean,
   * and the logs within
   * it are just for details)
   * Finer for just about everything else. (There's also the SERVER, INFO, etc. levels)
   * Just keep these things in mind while adding new logging code.
   */
  public static void log(final Level level, final String message, final Throwable t) {

    // We always log to the AI logger, though it only shows up if the developer has the logger enabled in
    // logging.properties
    if (t == null) {
      ProAI.getLogger().log(level, addIndentationCompensation(message, level));
    } else {
      ProAI.getLogger().log(level, addIndentationCompensation(message, level), t);
    }
    if (!ProLogSettings.loadSettings().EnableAILogging) {
      return; // Skip displaying to settings window if settings window option is turned off
    }
    final Level logDepth = ProLogSettings.loadSettings().AILoggingDepth;
    if (logDepth.equals(Level.FINE) && (level.equals(Level.FINER) || level.equals(Level.FINEST))) {
      return; // If the settings window log depth is a higher level than this messages, skip
    }
    if (logDepth.equals(Level.FINER) && level.equals(Level.FINEST)) {
      return;
    }
    ProLogUI.notifyAILogMessage(level, addIndentationCompensation(message, level));
  }

  /**
   * Adds extra spaces to get logs to lineup correctly. (Adds two spaces to fine, one to finer, none to finest, etc.)
   */
  private static String addIndentationCompensation(final String message, final Level level) {
    final StringBuilder builder = new StringBuilder();
    final int compensateLength = (level.toString().length() - 4) * 2;
    if (compensateLength == 0) {
      return message;
    }
    for (int i = 0; i < compensateLength; i++) {
      builder.append(" ");
    }
    builder.append(message);
    return builder.toString();
  }
}
