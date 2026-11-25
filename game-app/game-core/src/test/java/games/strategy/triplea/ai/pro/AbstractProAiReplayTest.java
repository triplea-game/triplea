package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ai.AiRandom;
import games.strategy.triplea.ai.fast.FastAi;
import games.strategy.triplea.ai.pro.logging.ProLogUi;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

class AbstractProAiReplayTest {

  @AfterEach
  void restoreDefaults() {
    Unit.setIdSource(null);
    AiRandom.setSource(null);
    ProLogUi.setLogSink(null);
  }

  @Test
  void fixedSeedsReplayThePurchasePhaseIdentically() {
    final Run first = playGermanPurchase(42);
    final Run second = playGermanPurchase(42);

    assumeFalse(first.log().isEmpty(), "Pro AI logging is turned off in this user's preferences");
    assertThat(first.purchased()).isNotEmpty();
    assertThat(second).isEqualTo(first);
  }

  private record Run(List<String> purchased, List<String> log) {}

  /**
   * Plays with FastAi, the Pro AI on an estimator instead of the battle calculator, because the
   * calculator's simulated dice are not among the seams.
   */
  private static Run playGermanPurchase(final long seed) {
    final Random unitIds = new Random(seed);
    Unit.setIdSource(() -> new UUID(unitIds.nextLong(), unitIds.nextLong()));
    final Random aiRandom = new Random(seed + 1);
    AiRandom.setSource(() -> aiRandom);
    final List<String> log = new ArrayList<>();
    ProLogUi.setLogSink(log::add);
    ClientSetting.setPreferences(new MemoryPreferences());
    final GameData data = TestMapGameData.WW2V3_1941.getGameData();
    final GamePlayer germans = data.getPlayerList().getPlayerId("Germans");
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "germanPurchase");
    final PurchaseDelegate purchaseDelegate = (PurchaseDelegate) data.getDelegate("purchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(bridge);
    final PlayerBridge playerBridge = mock(PlayerBridge.class);
    when(playerBridge.getGameData()).thenReturn(data);
    final FastAi ai = new FastAi("Germans", "Fast AI");
    ai.initialize(playerBridge, germans);

    ai.purchase(
        false, germans.getResources().getQuantity(Constants.PUS), purchaseDelegate, data, germans);

    log.removeIf(line -> line.contains(" time for purchase="));
    final List<String> purchased =
        germans.getUnits().stream().map(u -> u.getType().getName() + " " + u.getId()).toList();
    return new Run(purchased, log);
  }
}
