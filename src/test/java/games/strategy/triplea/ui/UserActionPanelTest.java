package games.strategy.triplea.ui;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.ResourceList;
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
  public void testGetResourcesSpendableOnUserActionsForPlayer_ShouldExcludeTechTokensAndVPs() {
    final GameData data = mock(GameData.class);
    final Resource gold = new Resource("gold", data);
    final Resource pus = new Resource(Constants.PUS, data);
    final Resource techTokens = new Resource(Constants.TECH_TOKENS, data);
    final Resource vps = new Resource(Constants.VPS, data);
    final ResourceList gameResources = createGameResources(gold, pus, techTokens, vps);
    when(data.getResourceList()).thenReturn(gameResources);
    final PlayerID player = new PlayerID("player", data);
    player.getResources().add(new IntegerMap<>(Arrays.asList(gold, pus, techTokens, vps), 42));

    final ResourceCollection playerResources = UserActionPanel.getResourcesSpendableOnUserActionsForPlayer(player);

    assertThat(playerResources.getQuantity(gold), is(42));
    assertThat(playerResources.getQuantity(pus), is(42));
    assertThat(playerResources.getQuantity(techTokens), is(0));
    assertThat(playerResources.getQuantity(vps), is(0));
  }

  private static ResourceList createGameResources(final Resource... resources) {
    final ResourceList gameResources = mock(ResourceList.class);
    for (final Resource resource : resources) {
      when(gameResources.getResource(resource.getName())).thenReturn(resource);
    }
    return gameResources;
  }
}
