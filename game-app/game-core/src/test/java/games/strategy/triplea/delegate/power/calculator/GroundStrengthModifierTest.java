package games.strategy.triplea.delegate.power.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroundStrengthModifierTest {
  @Test
  void offenseModifierAppliesOnlyToLandUnits() {
    final GamePlayer player = mock(GamePlayer.class);
    final Unit land = unit(player, false, false);
    final Unit air = unit(player, true, false);
    final Unit sea = unit(player, false, true);
    final MainOffenseCombatValue.MainOffenseStrength strength =
        new MainOffenseCombatValue.MainOffenseStrength(
            6, List.of(), 1, AvailableSupports.EMPTY_RESULT, AvailableSupports.EMPTY_RESULT);

    assertThat(strength.getStrength(land).getValue()).isEqualTo(3);
    assertThat(strength.getStrength(air).getValue()).isEqualTo(2);
    assertThat(strength.getStrength(sea).getValue()).isEqualTo(2);
  }

  private static Unit unit(final GamePlayer player, final boolean air, final boolean sea) {
    final Unit unit = mock(Unit.class);
    final UnitAttachment attachment = mock(UnitAttachment.class);
    when(unit.getOwner()).thenReturn(player);
    when(unit.getUnitAttachment()).thenReturn(attachment);
    when(attachment.getAttack(player)).thenReturn(2);
    when(attachment.isAir()).thenReturn(air);
    when(attachment.isSea()).thenReturn(sea);
    return unit;
  }
}
