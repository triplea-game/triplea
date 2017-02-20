package games.strategy.triplea.ui;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.util.IntegerMap;

public final class UserActionPanelTest {
  @Test
  public void testCanSpendResourcesOnUserActions_ShouldReturnFalseWhenNoUserActionsPresent() {
    final Collection<UserActionAttachment> userActions = Collections.emptyList();

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(false));
  }

  @Test
  public void testCanSpendResourcesOnUserActions_ShouldReturnFalseWhenNoUserActionHasCost() {
    final Collection<UserActionAttachment> userActions = Arrays.asList(
        createUserActionWithCost("userAction1", 0),
        createUserActionWithCost("userAction2", 0));

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(false));
  }

  private static UserActionAttachment createUserActionWithCost(final String name, final int costInPUs) {
    final UserActionAttachment userAction =
        new UserActionAttachment(name, mock(Attachable.class), mock(GameData.class));
    userAction.setCostPU(costInPUs);
    return userAction;
  }

  @Test
  public void testCanSpendResourcesOnUserActions_ShouldReturnTrueWhenAtLeastOneUserActionHasCost() {
    final Collection<UserActionAttachment> userActions = Arrays.asList(
        createUserActionWithCost("userAction1", 0),
        createUserActionWithCost("userAction2", 5));

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(true));
  }

  @Test
  public void testGetResourcesSpendableOnUserActionsForPlayer_ShouldReturnOnlyPUResources() throws Exception {
    final GameData data = parseGameFromResource("test-get-resources-spendable-on-user-actions-for-player.xml");
    final Resource gold = data.getResourceList().getResource("gold");
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final Resource silver = data.getResourceList().getResource("silver");
    final PlayerID player = new PlayerID("player", data);
    player.getResources().add(new IntegerMap<>(Arrays.asList(gold, pus, silver), 42));

    final ResourceCollection resources = UserActionPanel.getResourcesSpendableOnUserActionsForPlayer(player);

    assertThat(resources.getQuantity(gold), is(0));
    assertThat(resources.getQuantity(pus), is(42));
    assertThat(resources.getQuantity(silver), is(0));
  }

  private GameData parseGameFromResource(final String name) throws Exception {
    final URL url = getClass().getResource(name);
    if (url == null) {
      throw new Exception(String.format("game resource not found: %s", name));
    }

    try (final InputStream is = url.openStream()) {
      final GameParser gameParser = new GameParser(url.toString());
      return gameParser.parse(is, new AtomicReference<>(), false);
    }
  }
}
