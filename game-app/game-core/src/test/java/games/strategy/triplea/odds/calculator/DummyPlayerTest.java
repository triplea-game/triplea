package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("InnerClassMayBeStatic")
class DummyPlayerTest {

  @Nested
  class CasualtyTest {

    private final List<Unit> unitPool =
        List.of(
            mock(Unit.class),
            mock(Unit.class),
            mock(Unit.class),
            mock(Unit.class),
            mock(Unit.class));

    private final List<Unit> orderOfLosses =
        List.of(
            unitPool.get(2), unitPool.get(0), unitPool.get(1), unitPool.get(4), unitPool.get(3));

    private final List<Unit> damaged = List.of(unitPool.get(0), unitPool.get(2));
    private final List<Unit> killed = List.of(unitPool.get(1), unitPool.get(0));

    private final CasualtyList defaultCasualties = mock(CasualtyList.class);

    @BeforeEach
    void setUp() {
      when(defaultCasualties.getDamaged()).thenReturn(damaged);
      when(defaultCasualties.getKilled()).thenReturn(killed);
    }

    private void assertCommonCasualtyDetails(final CasualtyDetails details) {
      assertThat(details.getDamaged()).isEqualTo(damaged);
      assertThat(details.getAutoCalculated()).isFalse();
    }

    @Nested
    class OneLand {
      @BeforeEach
      void setUp() {
        for (final var unit : unitPool) {
          final var unitType = mock(UnitType.class);
          when(unit.getType()).thenReturn(unitType);
          final var unitAttachment = mock(UnitAttachment.class);
          when(unitType.getAttachment(any())).thenReturn(unitAttachment);
          when(unit.getUnitAttachment()).thenReturn(unitAttachment);
          when(unitAttachment.isAir()).thenReturn(!unit.equals(unitPool.get(0)));
          final var gameData = givenGameData().build();
          when(unit.getData()).thenReturn(gameData);
          final var playerId = mock(GamePlayer.class);
          when(unit.getOwner()).thenReturn(playerId);
        }
      }

      /**
       * This test checks if units are correctly selected keeping in mind that some units might not
       * be able to capture the target territory on their own. (Like air units for example.) This
       * only works if no orderOfLosses is present, otherwise the orderOfLosses overrules this
       * consideration.
       */
      @Test
      void selectCasualtiesWithNoLossOrder() {
        final DummyPlayer player = new DummyPlayer(null, false, "", null, true, 0, 0, false);
        final CasualtyDetails details =
            player.selectCasualties(
                unitPool,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                defaultCasualties,
                null,
                null,
                false);
        assertThat(details.getDamaged()).isEqualTo(damaged);
        assertThat(details.getKilled()).isEqualTo(List.of(unitPool.get(1), unitPool.get(2)));
        assertThat(details.getAutoCalculated()).isFalse();
      }

      /**
       * This test checks if a present non-empty orderOfLosses argument correctly overrules any
       * "keepAtLeastOneLand" considerations. The result should be the same as in {@link
       * #selectCasualtiesWithNoLand}.
       */
      @Test
      void selectCasualties() {
        final DummyPlayer player =
            new DummyPlayer(null, false, "", orderOfLosses, true, 0, 0, false);
        final CasualtyDetails details =
            player.selectCasualties(
                unitPool,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                defaultCasualties,
                null,
                null,
                false);
        assertCommonCasualtyDetails(details);
        assertThat(details.getKilled()).isEqualTo(List.of(unitPool.get(2), unitPool.get(0)));
      }
    }

    /**
     * This test checks if the arguments will be returned unaltered, when orderOfLosses is not
     * present and keepAtLeastOneLand is false.
     */
    @Test
    void selectCasualtiesWithNoLandAndNoLossOrder() {
      final DummyPlayer player = new DummyPlayer(null, false, "", null, false, 0, 0, false);
      final CasualtyDetails details =
          player.selectCasualties(
              unitPool,
              null,
              0,
              null,
              null,
              null,
              null,
              null,
              false,
              null,
              defaultCasualties,
              null,
              null,
              false);
      assertCommonCasualtyDetails(details);
      assertThat(details.getKilled()).isEqualTo(killed);
    }

    /**
     * This test checks if units are correctly selected based on the loss order, ignoring that some
     * units might not be able to capture the target territory on their own. (Like air units for
     * example.)
     */
    @Test
    void selectCasualtiesWithNoLand() {
      final DummyPlayer player =
          new DummyPlayer(null, false, "", orderOfLosses, false, 0, 0, false);
      final CasualtyDetails details =
          player.selectCasualties(
              unitPool,
              null,
              0,
              null,
              null,
              null,
              null,
              null,
              false,
              null,
              defaultCasualties,
              null,
              null,
              false);
      assertCommonCasualtyDetails(details);
      assertThat(details.getKilled()).isEqualTo(List.of(unitPool.get(2), unitPool.get(0)));
    }

    /**
     * This test checks if the case is handled correctly, where there are fewer entries in
     * orderOfLosses than there are Units in the selectFrom parameter of {@link
     * DummyPlayer#selectCasualties}.
     */
    @Test
    void selectCasualtiesWithNoLandAndFewEntriesInLossOrder() {
      final DummyPlayer player =
          new DummyPlayer(null, false, "", List.of(unitPool.get(0)), false, 0, 0, false);
      final CasualtyDetails details =
          player.selectCasualties(
              unitPool,
              null,
              0,
              null,
              null,
              null,
              null,
              null,
              false,
              null,
              defaultCasualties,
              null,
              null,
              false);
      assertCommonCasualtyDetails(details);
      assertThat(details.getKilled()).isEqualTo(List.of(unitPool.get(0), unitPool.get(1)));
    }
  }

  @Nested
  class GetUnits {

    private final DummyDelegateBridge bridge = mock(DummyDelegateBridge.class);
    private final MustFightBattle battle = mock(MustFightBattle.class);

    private DummyPlayer attacker;
    private DummyPlayer defender;

    private final List<Unit> attackers =
        List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class));
    private final List<Unit> defenders =
        List.of(mock(Unit.class), mock(Unit.class), mock(Unit.class));

    @BeforeEach
    void setUp() {
      attacker = new DummyPlayer(bridge, true, "", null, false, 0, 0, false);
      defender = new DummyPlayer(bridge, false, "", null, false, 0, 0, false);

      when(battle.getAttackingUnits()).thenReturn(attackers);
      when(battle.getDefendingUnits()).thenReturn(defenders);
    }

    @Test
    void getOurUnits() {
      assertThat(attacker.getOurUnits()).isNull();
      assertThat(defender.getOurUnits()).isNull();

      when(bridge.getBattle()).thenReturn(battle);

      assertThat(attacker.getOurUnits()).isEqualTo(attackers);
      assertThat(defender.getOurUnits()).isEqualTo(defenders);

      // Test if we get a copy
      assertThat(attacker.getOurUnits()).isNotSameAs(attackers);
      assertThat(defender.getOurUnits()).isNotSameAs(defenders);
    }

    @Test
    void getEnemyUnits() {
      assertThat(attacker.getEnemyUnits()).isNull();
      assertThat(defender.getEnemyUnits()).isNull();

      when(bridge.getBattle()).thenReturn(battle);

      assertThat(attacker.getEnemyUnits()).isEqualTo(defenders);
      assertThat(defender.getEnemyUnits()).isEqualTo(attackers);

      // Test if we get a copy
      assertThat(attacker.getEnemyUnits()).isNotSameAs(defenders);
      assertThat(defender.getEnemyUnits()).isNotSameAs(attackers);
    }
  }
}
