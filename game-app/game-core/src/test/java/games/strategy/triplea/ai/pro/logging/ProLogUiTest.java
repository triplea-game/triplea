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
}
