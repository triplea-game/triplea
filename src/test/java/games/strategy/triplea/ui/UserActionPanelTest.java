package games.strategy.triplea.ui;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.ResourceList;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.util.IntegerMap;

@RunWith(MockitoJUnitRunner.class)
public final class UserActionPanelTest {
  @Mock
  private GameData data;

  private Resource pus;

  private Resource techTokens;

  private Resource vps;

  @Before
  public void setUp() {
    pus = createResource(Constants.PUS);
    techTokens = createResource(Constants.TECH_TOKENS);
    vps = createResource(Constants.VPS);
    setGameResources(pus, techTokens, vps);
  }

  private Resource createResource(final String name) {
    return new Resource(name, data);
  }

  private void setGameResources(final Resource... resources) {
    final ResourceList gameResources = mock(ResourceList.class);
    for (final Resource resource : resources) {
      when(gameResources.getResource(resource.getName())).thenReturn(resource);
    }

    when(data.getResourceList()).thenReturn(gameResources);
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnFalseWhenUserActionCostGreaterThanPlayerPUs() {
    final PlayerID player = createPlayerWithResources(pus);
    final UserActionAttachment userAction = createUserActionWithCost(player.getResources().getQuantity(pus) + 1);

    final boolean canAffordUserAction = UserActionPanel.canPlayerAffordUserAction(player, userAction);

    assertThat(canAffordUserAction, is(false));
  }

  private PlayerID createPlayerWithResources(final Resource... resources) {
    final PlayerID player = new PlayerID("player", data);
    player.getResources().add(new IntegerMap<>(Arrays.stream(resources).collect(toList()), 42));
    return player;
  }

  private UserActionAttachment createUserActionWithCost(final int costInPUs) {
    final UserActionAttachment userAction = new UserActionAttachment("userAction", mock(Attachable.class), data);
    userAction.setCostPU(costInPUs);
    return userAction;
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnTrueWhenUserActionCostEqualToPlayerPUs() {
    final PlayerID player = createPlayerWithResources(pus);
    final UserActionAttachment userAction = createUserActionWithCost(player.getResources().getQuantity(pus));

    final boolean canAffordUserAction = UserActionPanel.canPlayerAffordUserAction(player, userAction);

    assertThat(canAffordUserAction, is(true));
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnTrueWhenUserActionCostLessThanPlayerPUs() {
    final PlayerID player = createPlayerWithResources(pus);
    final UserActionAttachment userAction = createUserActionWithCost(player.getResources().getQuantity(pus) - 1);

    final boolean canAffordUserAction = UserActionPanel.canPlayerAffordUserAction(player, userAction);

    assertThat(canAffordUserAction, is(true));
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnTrueWhenUserActionCostIsZeroAndPlayerPUsIsZero() {
    final PlayerID player = createPlayerWithResources();
    final UserActionAttachment userAction = createUserActionWithCost(0);

    final boolean canAffordUserAction = UserActionPanel.canPlayerAffordUserAction(player, userAction);

    assertThat(canAffordUserAction, is(true));
  }

  @Test
  public void testCanSpendResourcesOnUserActions_ShouldReturnFalseWhenNoUserActionsPresent() {
    final Collection<UserActionAttachment> userActions = Collections.emptyList();

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(false));
  }

  @Test
  public void testCanSpendResourcesOnUserActions_ShouldReturnFalseWhenNoUserActionHasCost() {
    final Collection<UserActionAttachment> userActions = Arrays.asList(
        createUserActionWithCost(0),
        createUserActionWithCost(0));

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(false));
  }

  @Test
  public void testCanSpendResourcesOnUserActions_ShouldReturnTrueWhenAtLeastOneUserActionHasCost() {
    final Collection<UserActionAttachment> userActions = Arrays.asList(
        createUserActionWithCost(0),
        createUserActionWithCost(5));

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(true));
  }

  @Test
  public void testGetResourcesSpendableOnUserActionsForPlayer_ShouldExcludeTechTokensAndVPs() {
    final Resource gold = createResource("gold");
    setGameResources(gold, pus, techTokens, vps);
    final PlayerID player = createPlayerWithResources(gold, pus, techTokens, vps);

    final ResourceCollection spendableResources = UserActionPanel.getResourcesSpendableOnUserActionsForPlayer(player);

    assertThat(spendableResources.getQuantity(gold), is(player.getResources().getQuantity(gold)));
    assertThat(spendableResources.getQuantity(pus), is(player.getResources().getQuantity(pus)));
    assertThat(spendableResources.getQuantity(techTokens), is(0));
    assertThat(spendableResources.getQuantity(vps), is(0));
  }
}
