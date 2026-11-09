package games.strategy.triplea.ai.pro.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProLogUiTest {

  @AfterEach
  void restoreLogWindow() {
    ProLogUi.setLogSink(null);
    ProLogUi.setLogDisplay(null);
  }

  @Test
  void logSinkReceivesAiLogMessages() {
    final List<String> sink = new ArrayList<>();
    ProLogUi.setLogSink(sink::add);

    ProLogUi.notifyAiLogMessage("Starting combat move phase");

    assertThat(sink).containsExactly("Starting combat move phase");
  }

  @Test
  void clearingLogSinkStopsRoutingToIt() {
    final List<String> sink = new ArrayList<>();
    ProLogUi.setLogSink(sink::add);
    ProLogUi.setLogSink(null);

    ProLogUi.notifyAiLogMessage("Starting combat move phase");

    assertThat(sink).isEmpty();
  }

  @Test
  void attachedLogDisplayReceivesAiLogMessages() {
    final RecordingDisplay display = new RecordingDisplay();
    ProLogUi.setLogDisplay(display);

    ProLogUi.notifyAiLogMessage("Starting combat move phase");

    assertThat(display.messages).containsExactly("Starting combat move phase");
  }

  @Test
  void logSinkTakesAiLogMessagesFromTheLogDisplay() {
    final RecordingDisplay display = new RecordingDisplay();
    ProLogUi.setLogDisplay(display);
    final List<String> sink = new ArrayList<>();
    ProLogUi.setLogSink(sink::add);

    ProLogUi.notifyAiLogMessage("Starting combat move phase");

    assertThat(sink).containsExactly("Starting combat move phase");
    assertThat(display.messages).isEmpty();
  }

  @Test
  void logDisplayIsNotifiedOnceForEachNewRoundAndPlayer() {
    final RecordingDisplay display = new RecordingDisplay();
    ProLogUi.setLogDisplay(display);

    ProLogUi.notifyStartOfRound(7, "Germans");
    ProLogUi.notifyStartOfRound(7, "Germans");
    ProLogUi.notifyStartOfRound(7, "Russians");

    assertThat(display.rounds).containsExactly("7-Germans", "7-Russians");
  }

  private static final class RecordingDisplay implements ProLogDisplay {
    private final List<String> messages = new ArrayList<>();
    private final List<String> rounds = new ArrayList<>();

    @Override
    public void addMessage(final String message) {
      messages.add(message);
    }

    @Override
    public void notifyNewRound(final int round, final String playerName) {
      rounds.add(round + "-" + playerName);
    }
  }
}
