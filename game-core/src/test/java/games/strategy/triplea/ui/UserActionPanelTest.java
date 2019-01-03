package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.util.IntegerMap;

@ExtendWith(MockitoExtension.class)
final class UserActionPanelTest {
  @Mock private GameData data;

  private Resource pus;

  private PlayerId newPlayer() {
    final PlayerId player = new PlayerId("player", data);
    player.getResources().addResource(pus, 42);
    return player;
  }

  private UserActionAttachment newUserActionWithCost(final int costInPUs) {
    final UserActionAttachment userAction =
        new UserActionAttachment("userAction", mock(Attachable.class), data);
    if (costInPUs > 0) {
      final IntegerMap<Resource> cost = new IntegerMap<>();
      cost.put(pus, costInPUs);
      userAction.setCostResources(cost);
    }
    return userAction;
  }

  @BeforeEach
  void setUp() {
    pus = new Resource(Constants.PUS, data);
  }

  @Nested
  final class CanPlayerAffordUserActionTest {
    @Test
    void shouldReturnFalseWhenUserActionCostGreaterThanPlayerPUs() {
      final PlayerId player = newPlayer();
      final UserActionAttachment userAction =
          newUserActionWithCost(player.getResources().getQuantity(pus) + 1);

      final boolean canAffordUserAction =
          UserActionPanel.canPlayerAffordUserAction(player, userAction);

      assertThat(canAffordUserAction, is(false));
    }

    @Test
    void shouldReturnTrueWhenUserActionCostEqualToPlayerPUs() {
      final PlayerId player = newPlayer();
      final UserActionAttachment userAction =
          newUserActionWithCost(player.getResources().getQuantity(pus));

      final boolean canAffordUserAction =
          UserActionPanel.canPlayerAffordUserAction(player, userAction);

      assertThat(canAffordUserAction, is(true));
    }

    @Test
    void shouldReturnTrueWhenUserActionCostLessThanPlayerPUs() {
      final PlayerId player = newPlayer();
      final UserActionAttachment userAction =
          newUserActionWithCost(player.getResources().getQuantity(pus) - 1);

      final boolean canAffordUserAction =
          UserActionPanel.canPlayerAffordUserAction(player, userAction);

      assertThat(canAffordUserAction, is(true));
    }

    @Test
    void shouldReturnTrueWhenUserActionCostIsZeroAndPlayerPUsIsZero() {
      final PlayerId player = newPlayer();
      final UserActionAttachment userAction = newUserActionWithCost(0);

      final boolean canAffordUserAction =
          UserActionPanel.canPlayerAffordUserAction(player, userAction);

      assertThat(canAffordUserAction, is(true));
    }
  }

  @Nested
  final class CanSpendResourcesOnUserActionsTest {
    @Test
    void shouldReturnFalseWhenNoUserActionsPresent() {
      final Collection<UserActionAttachment> userActions = Collections.emptyList();

      final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

      assertThat(canSpendResources, is(false));
    }

    @Test
    void shouldReturnFalseWhenNoUserActionHasCost() {
      final Collection<UserActionAttachment> userActions =
          Arrays.asList(newUserActionWithCost(0), newUserActionWithCost(0));

      final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

      assertThat(canSpendResources, is(false));
    }

    @Test
    void shouldReturnTrueWhenAtLeastOneUserActionHasCost() {
      final Collection<UserActionAttachment> userActions =
          Arrays.asList(newUserActionWithCost(0), newUserActionWithCost(5));

      final boolean canSpendResources = UserActionPanel.canSpendResourcesOnUserActions(userActions);

      assertThat(canSpendResources, is(true));
    }
  }
}
