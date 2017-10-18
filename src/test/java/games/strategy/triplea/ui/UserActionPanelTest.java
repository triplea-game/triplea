package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UserActionAttachment;

@ExtendWith(MockitoExtension.class)
public final class UserActionPanelTest {
  @Mock
  private GameData data;

  private Resource pus;

  @BeforeEach
  public void setUp() {
    pus = new Resource(Constants.PUS, data);
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnFalseWhenUserActionCostGreaterThanPlayerPUs() {
    givenGameHasPuResource();
    final PlayerID player = createPlayer();
    final UserActionAttachment userAction = createUserActionWithCost(player.getResources().getQuantity(pus) + 1);

    final boolean canAffordUserAction = UserActionPanel.canPlayerAffordUserAction(player, userAction);

    assertThat(canAffordUserAction, is(false));
  }

  private void givenGameHasPuResource() {
    final ResourceList gameResources = mock(ResourceList.class);
    when(gameResources.getResource(pus.getName())).thenReturn(pus);
    when(data.getResourceList()).thenReturn(gameResources);
  }

  private PlayerID createPlayer() {
    final PlayerID player = new PlayerID("player", data);
    player.getResources().addResource(pus, 42);
    return player;
  }

  private UserActionAttachment createUserActionWithCost(final int costInPUs) {
    final UserActionAttachment userAction = new UserActionAttachment("userAction", mock(Attachable.class), data);
    userAction.setCostPU(costInPUs);
    return userAction;
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnTrueWhenUserActionCostEqualToPlayerPUs() {
    givenGameHasPuResource();
    final PlayerID player = createPlayer();
    final UserActionAttachment userAction = createUserActionWithCost(player.getResources().getQuantity(pus));

    final boolean canAffordUserAction = UserActionPanel.canPlayerAffordUserAction(player, userAction);

    assertThat(canAffordUserAction, is(true));
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnTrueWhenUserActionCostLessThanPlayerPUs() {
    givenGameHasPuResource();
    final PlayerID player = createPlayer();
    final UserActionAttachment userAction = createUserActionWithCost(player.getResources().getQuantity(pus) - 1);

    final boolean canAffordUserAction = UserActionPanel.canPlayerAffordUserAction(player, userAction);

    assertThat(canAffordUserAction, is(true));
  }

  @Test
  public void testCanPlayerAffordUserAction_ShouldReturnTrueWhenUserActionCostIsZeroAndPlayerPUsIsZero() {
    givenGameHasPuResource();
    final PlayerID player = createPlayer();
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
    final Collection<UserActionAttachment> userActions =
        Arrays.asList(createUserActionWithCost(0), createUserActionWithCost(0));

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(false));
  }

  @Test
  public void testCanSpendResourcesOnUserActions_ShouldReturnTrueWhenAtLeastOneUserActionHasCost() {
    final Collection<UserActionAttachment> userActions =
        Arrays.asList(createUserActionWithCost(0), createUserActionWithCost(5));

    final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

    assertThat(canSpendResources, is(true));
  }
}
