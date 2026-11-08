package games.strategy.triplea.ai.pro.logging;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;

/** Routes Pro AI log output to a log sink or to the attached {@link ProLogDisplay}, if any. */
@UtilityClass
public final class ProLogUi {
  private static String currentName = "";
  private static int currentRound = 0;
  private static volatile Consumer<String> logSink = null;
  private static volatile ProLogDisplay logDisplay = null;

  /** Sends AI log messages to {@code sink} instead of the log display; null restores the display. */
  public static void setLogSink(final @Nullable Consumer<String> sink) {
    logSink = sink;
  }

  /** Attaches the display that shows AI log output; null detaches it. */
  public static void setLogDisplay(final @Nullable ProLogDisplay display) {
    logDisplay = display;
  }

  static boolean hasReceiver() {
    return logSink != null || logDisplay != null;
  }

  static void notifyAiLogMessage(final String message) {
    final Consumer<String> sink = logSink;
    if (sink != null) {
      sink.accept(message);
      return;
    }
    final ProLogDisplay display = logDisplay;
    if (display != null) {
      display.addMessage(message);
    }
  }

  public static void notifyStartOfRound(final int round, final String name) {
    final ProLogDisplay display = logDisplay;
    if (display == null) {
      return;
    }
    if (round != currentRound || !name.equals(currentName)) {
      currentRound = round;
      currentName = name;
      display.notifyNewRound(round, name);
    }
  }
}
