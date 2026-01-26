package org.triplea.game.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class HeadlessServerSetupTest {
  private final ServerModel model = mock(ServerModel.class);
  private final GameSelectorModel gameSelectorModel = mock(GameSelectorModel.class);
  private final HeadlessServerSetup headlessServerSetup =
      new HeadlessServerSetup(model, gameSelectorModel);

  @Test
  void verifyCancelDoesMakeWaitNonBlocking() throws Exception {
    headlessServerSetup.cancel();
    final var future = CompletableFuture.supplyAsync(headlessServerSetup::waitUntilStart);
    assertThat(future.get(1, TimeUnit.SECONDS), is(false));
  }

  @Test
  void verifyCancelDoesWakeUpThread() throws Exception {
    final var countDownLatch = new CountDownLatch(1);
    final var future =
        CompletableFuture.supplyAsync(
            () -> {
              countDownLatch.countDown();
              return headlessServerSetup.waitUntilStart();
            });
    countDownLatch.await();
    headlessServerSetup.cancel();
    assertThat(future.get(1, TimeUnit.SECONDS), is(false));
  }

  private void fulfillCondition() {
    when(gameSelectorModel.getGameData()).thenReturn(mock(GameData.class));
    // we can't use Map.of() here because containsValue(null) would throw an NPE
    final Map<String, String> map = new HashMap<>();
    map.put("Test", "Test");
    when(model.getPlayersToNodeListing()).thenReturn(map);
    when(model.getPlayersEnabledListing()).thenReturn(Map.of("Test", Boolean.TRUE));
  }

  @Test
  void verifyFulfilledConditionDoesMakeWaitNonBlocking() throws Exception {
    fulfillCondition();
    final var future = CompletableFuture.supplyAsync(headlessServerSetup::waitUntilStart);
    assertThat(future.get(1, TimeUnit.SECONDS), is(true));
  }

  @Test
  void verifyWaitWaitsUntilConditionIsFulfilled_playerListChanged() throws Exception {
    fulfillCondition();
    final var countDownLatch = new CountDownLatch(1);
    final var future =
        CompletableFuture.supplyAsync(
            () -> {
              countDownLatch.countDown();
              return headlessServerSetup.waitUntilStart();
            });
    countDownLatch.await();
    headlessServerSetup.playerListChanged();
    assertThat(future.get(1, TimeUnit.SECONDS), is(true));
  }

  @Test
  void verifyWaitWaitsUntilConditionIsFulfilled_playersTakenChanged() throws Exception {
    fulfillCondition();
    final var countDownLatch = new CountDownLatch(1);
    final var future =
        CompletableFuture.supplyAsync(
            () -> {
              countDownLatch.countDown();
              return headlessServerSetup.waitUntilStart();
            });
    countDownLatch.await();
    headlessServerSetup.playersTakenChanged();
    assertThat(future.get(1, TimeUnit.SECONDS), is(true));
  }

  @Test
  void verifyInterruptsReturnFalse() throws Exception {
    final var thread = new AtomicReference<Thread>();
    final var countDownLatch = new CountDownLatch(1);
    final var future =
        CompletableFuture.supplyAsync(
            () -> {
              thread.set(Thread.currentThread());
              countDownLatch.countDown();
              return headlessServerSetup.waitUntilStart();
            });
    countDownLatch.await();
    thread.get().interrupt();
    assertThat(future.get(1, TimeUnit.SECONDS), is(false));
  }

  @Test
  void verifyCancelOperationIsFinal() throws Exception {
    headlessServerSetup.cancel();
    headlessServerSetup.playersTakenChanged();
    headlessServerSetup.playerListChanged();
    final var future = CompletableFuture.supplyAsync(headlessServerSetup::waitUntilStart);
    assertThat(future.get(1, TimeUnit.SECONDS), is(false));
  }
}
