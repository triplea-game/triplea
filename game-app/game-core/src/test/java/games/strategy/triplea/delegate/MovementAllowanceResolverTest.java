package games.strategy.triplea.delegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import org.junit.jupiter.api.Test;

class MovementAllowanceResolverTest {
  @Test
  void resolvesCombatAndRedeploymentMovementWithUnitBonus() {
    final Unit unit = mock(Unit.class);
    final UnitAttachment attachment = mock(UnitAttachment.class);
    final GamePlayer player = mock(GamePlayer.class);
    when(unit.getUnitAttachment()).thenReturn(attachment);
    when(unit.getOwner()).thenReturn(player);
    when(unit.getBonusMovement()).thenReturn(1);
    when(attachment.getCombatMovement(player)).thenReturn(2);
    when(attachment.getRedeploymentMovement(player)).thenReturn(5);

    assertThat(
            MovementAllowanceResolver.resolveMaximumMovement(
                unit, MovementAllowanceResolver.MovementPhase.COMBAT))
        .isEqualTo(3);
    assertThat(
            MovementAllowanceResolver.resolveMaximumMovement(
                unit, MovementAllowanceResolver.MovementPhase.REDEPLOYMENT))
        .isEqualTo(6);
  }

  @Test
  void usesLegacyMovementOutsideMovementPhases() {
    final Unit unit = mock(Unit.class);
    final UnitAttachment attachment = mock(UnitAttachment.class);
    final GamePlayer player = mock(GamePlayer.class);
    when(unit.getUnitAttachment()).thenReturn(attachment);
    when(unit.getOwner()).thenReturn(player);
    when(attachment.getMovement(player)).thenReturn(4);

    assertThat(
            MovementAllowanceResolver.resolveMaximumMovement(
                unit, MovementAllowanceResolver.MovementPhase.OTHER))
        .isEqualTo(4);
  }

  @Test
  void usesLegacyMovementBeforeTheGameSequenceIsInitialized() {
    final GameData data = mock(GameData.class);
    final GameSequence sequence = mock(GameSequence.class);
    final Unit unit = mock(Unit.class);
    final UnitAttachment attachment = mock(UnitAttachment.class);
    final GamePlayer player = mock(GamePlayer.class);
    when(data.getSequence()).thenReturn(sequence);
    when(sequence.size()).thenReturn(0);
    when(unit.getData()).thenReturn(data);
    when(unit.getUnitAttachment()).thenReturn(attachment);
    when(unit.getOwner()).thenReturn(player);
    when(attachment.getMovement(player)).thenReturn(4);

    assertThat(MovementAllowanceResolver.resolveMaximumMovement(unit)).isEqualTo(4);
  }

  @Test
  void phaseOverridesFallBackToLegacyMovementAndApplyTechBonus() {
    final GameData data = mock(GameData.class);
    final UnitType unitType = mock(UnitType.class);
    final GamePlayer player = mock(GamePlayer.class);
    final TechTracker techTracker = mock(TechTracker.class);
    when(data.getTechTracker()).thenReturn(techTracker);
    when(techTracker.getMovementBonus(player, unitType)).thenReturn(1);
    final UnitAttachment attachment =
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, unitType, data);
    attachment.setMovement(3);

    assertThat(attachment.getCombatMovement(player)).isEqualTo(4);
    assertThat(attachment.getRedeploymentMovement(player)).isEqualTo(4);

    attachment.setCombatMovement(1);
    attachment.setRedeploymentMovement(5);
    assertThat(attachment.getCombatMovement(player)).isEqualTo(2);
    assertThat(attachment.getRedeploymentMovement(player)).isEqualTo(6);
  }

  @Test
  void exposesOptionalOverridesAndRejectsNegativeValues() {
    final UnitAttachment attachment =
        new UnitAttachment(
            Constants.UNIT_ATTACHMENT_NAME, mock(UnitType.class), mock(GameData.class));

    assertThat(attachment.getCombatMovement()).isEmpty();
    assertThat(attachment.getRedeploymentMovement()).isEmpty();
    assertThat(attachment.getPropertyOrEmpty("combatMovement")).isPresent();
    assertThat(attachment.getPropertyOrEmpty("redeploymentMovement")).isPresent();

    attachment.setCombatMovement(2);
    attachment.setRedeploymentMovement(4);
    assertThat(attachment.getCombatMovement()).hasValue(2);
    assertThat(attachment.getRedeploymentMovement()).hasValue(4);
    assertThatThrownBy(() -> attachment.setCombatMovement(-1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> attachment.setRedeploymentMovement(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
