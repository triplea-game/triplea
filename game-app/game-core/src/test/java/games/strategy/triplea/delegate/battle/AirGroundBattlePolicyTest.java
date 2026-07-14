package games.strategy.triplea.delegate.battle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.IBattle.BattleDomain;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class AirGroundBattlePolicyTest {
  @Test
  void battleTypesExposeExplicitDomains() {
    assertThat(BattleType.NORMAL.getDomain()).isEqualTo(BattleDomain.GROUND);
    assertThat(BattleType.AIR_BATTLE.getDomain()).isEqualTo(BattleDomain.AIR);
    assertThat(BattleType.AIR_RAID.getDomain()).isEqualTo(BattleDomain.RAID);
    assertThat(BattleType.BOMBING_RAID.getDomain()).isEqualTo(BattleDomain.RAID);
  }

  @Test
  void separatedCombatIsOptIn() {
    final GameState gameState = mock(GameState.class);
    final GameProperties properties = mock(GameProperties.class);
    when(gameState.getProperties()).thenReturn(properties);
    when(properties.get(AirGroundBattlePolicy.SEPARATE_AIR_AND_GROUND_COMBAT, false))
        .thenReturn(true);

    assertThat(AirGroundBattlePolicy.isSeparatedCombatEnabled(gameState)).isTrue();
    assertThat(AirGroundBattlePolicy.isSeparatedCombatEnabled(null)).isFalse();
  }

  @Test
  void partitionsUnitsWithoutDuplicatingAircraftInGroundCombat() {
    final Unit fighter = unit(true);
    final Unit infantry = unit(false);

    assertThat(AirGroundBattlePolicy.unitsForDomain(List.of(fighter, infantry), BattleDomain.AIR))
        .containsExactly(fighter);
    assertThat(
            AirGroundBattlePolicy.unitsForDomain(List.of(fighter, infantry), BattleDomain.GROUND))
        .containsExactly(infantry);
  }

  @Test
  void raidsResolveBeforeAirCombatAndAirCombatBeforeGroundCombat() {
    assertThat(
            AirGroundBattlePolicy.orderForResolution(
                List.of(BattleType.NORMAL, BattleType.AIR_BATTLE, BattleType.BOMBING_RAID)))
        .containsExactly(BattleType.BOMBING_RAID, BattleType.AIR_BATTLE, BattleType.NORMAL);
    assertThat(AirGroundBattlePolicy.mustPrecede(BattleType.AIR_BATTLE, BattleType.NORMAL))
        .isTrue();
    assertThat(AirGroundBattlePolicy.mustPrecede(BattleType.NORMAL, BattleType.AIR_BATTLE))
        .isFalse();
  }

  private static Unit unit(final boolean air) {
    final Unit unit = mock(Unit.class);
    final UnitAttachment attachment = mock(UnitAttachment.class);
    when(unit.getUnitAttachment()).thenReturn(attachment);
    when(attachment.isAir()).thenReturn(air);
    return unit;
  }
}
