package games.strategy.triplea.odds.calculator.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link GameDataBattleAdapter} — the sole seam that imports {@code
 * games.strategy.engine.data.*} (design §7, invariant 4). Runs against the real Total World War
 * test map ({@link TestMapGameData#TWW}), the one fixture in the test tree that carries both a
 * populated territory-effect table and a {@code whenHitPointsDamagedChangesInto} unit
 * (germanBattleship). Every assertion pins a real {@code GameData} value, not a hand-picked literal
 * — this is what keeps the adapter honest as the map data evolves.
 */
class GameDataBattleAdapterTest {

  private final GameData data = TestMapGameData.TWW.getGameData();
  private final GamePlayer germany = GameDataTestUtil.germany(data);
  private final GamePlayer britain = GameDataTestUtil.britain(data);
  private final Territory location = GameDataTestUtil.territory("Southwestern US", data);
  private final GameDataBattleAdapter adapter = new GameDataBattleAdapter();

  /**
   * Invariant 1 (design §6) — the merge key. Two germanArtillery units with identical combat stats
   * must fold into one {@link CombatProfile} bucket with count 2, not two count-1 buckets; the
   * vectorized state model is only sound if combat-fungible units actually merge.
   */
  @Test
  void identicalUnitsMergeIntoOneCombatProfileWithSummedCount() {
    final Collection<Unit> attacking = GameDataTestUtil.germanArtillery(data).create(2, germany);
    final Collection<Unit> defending = GameDataTestUtil.britishInfantry(data).create(1, britain);

    final BattleScenario scenario =
        adapter.toScenario(
            germany,
            britain,
            location,
            attacking,
            defending,
            List.of(),
            List.of(),
            new BattleOptions(false, List.of(), List.of()));

    final Map<Key, Integer> attackers = scenario.attackers().counts();
    assertThat(attackers).hasSize(1);
    final Map.Entry<Key, Integer> merged = attackers.entrySet().iterator().next();
    assertThat(merged.getValue()).isEqualTo(2);
    assertThat(merged.getKey().state()).isEqualTo(Lifecycle.ACTIVE);
    assertThat(merged.getKey().profile().type()).isEqualTo(new UnitTypeId("germanArtillery"));
    assertThat(merged.getKey().profile().attack()).isEqualTo(3);
    assertThat(merged.getKey().profile().defense()).isEqualTo(3);
  }

  /**
   * TWW's Hills {@link TerritoryEffect} gives germanArtillery +0 offense / +1 defense. The adapter
   * must bake that bonus directly into the produced {@link CombatProfile#defense()} — nothing
   * downstream reaches back into a territory reference (design §7).
   */
  @Test
  void territoryEffectBakesIntoTheDefenseStatWithNoTerritoryReferenceLeaking() {
    final TerritoryEffect hills = data.getTerritoryEffectList().get("Hills");
    final Collection<Unit> attacking = GameDataTestUtil.britishInfantry(data).create(1, britain);
    final Collection<Unit> defending = GameDataTestUtil.germanArtillery(data).create(1, germany);

    final BattleScenario scenario =
        adapter.toScenario(
            britain,
            germany,
            location,
            attacking,
            defending,
            List.of(),
            List.of(hills),
            new BattleOptions(false, List.of(), List.of()));

    final Map<Key, Integer> defenders = scenario.defenders().counts();
    assertThat(defenders).hasSize(1);
    final CombatProfile artillery = defenders.keySet().iterator().next().profile();
    assertThat(artillery.attack()).as("Hills' germanArtillery offense modifier is +0").isEqualTo(3);
    assertThat(artillery.defense())
        .as("base defense 3 + Hills' germanArtillery defense modifier +1")
        .isEqualTo(4);
  }

  /**
   * Damage is a migration, not a flag (design §6 invariant 2). A hit on an undamaged
   * germanBattleship must migrate it to the germanBattleship-damaged stats and type ({@code
   * whenHitPointsDamagedChangesInto}, TWW map data) via {@link CombatProfile#onHit()}; a second hit
   * on the damaged profile has no further changesInto rule, so it must return {@link
   * Optional#empty()} (dies).
   */
  @Test
  void multiHitPointUnitMigratesStatsAndTypeThenDiesOnTheSecondHit() {
    final Collection<Unit> attacking = GameDataTestUtil.germanBattleship(data).create(1, germany);
    final Collection<Unit> defending = GameDataTestUtil.britishInfantry(data).create(1, britain);

    final BattleScenario scenario =
        adapter.toScenario(
            germany,
            britain,
            location,
            attacking,
            defending,
            List.of(),
            List.of(),
            new BattleOptions(false, List.of(), List.of()));

    final CombatProfile undamaged =
        scenario.attackers().counts().keySet().iterator().next().profile();
    assertThat(undamaged.type()).isEqualTo(new UnitTypeId("germanBattleship"));
    assertThat(undamaged.attack()).isEqualTo(7);
    assertThat(undamaged.defense()).isEqualTo(8);
    assertThat(undamaged.domain()).isEqualTo(Domain.SEA);
    assertThat(undamaged.hitPoints())
        .as("hitPoints counts down remaining hits — a 2-HP battleship starts at 2")
        .isEqualTo(2);

    final Optional<CombatProfile> damaged = undamaged.onHit();
    assertThat(damaged).isPresent();
    assertThat(damaged.get().type()).isEqualTo(new UnitTypeId("germanBattleship-damaged"));
    assertThat(damaged.get().attack()).isEqualTo(6);
    assertThat(damaged.get().defense()).isEqualTo(5);
    assertThat(damaged.get().hitPoints())
        .as("one hit taken, so the damaged profile has one hit-point of countdown left")
        .isEqualTo(1);
    assertThat(damaged.get().onHit())
        .as("germanBattleship-damaged has no further whenHitPointsDamagedChangesInto — it dies")
        .isEmpty();
  }

  /**
   * The other half of invariant 2: a unit with no {@code whenHitPointsDamagedChangesInto} option
   * (britishInfantry, 1 hit point) dies on its first hit — {@link CombatProfile#onHit()} is empty
   * from the start, with no intermediate damaged bucket.
   */
  @Test
  void singleHitPointUnitHasNoOnHitMigrationAndDiesImmediately() {
    final Collection<Unit> attacking = GameDataTestUtil.germanArtillery(data).create(1, germany);
    final Collection<Unit> defending = GameDataTestUtil.britishInfantry(data).create(1, britain);

    final BattleScenario scenario =
        adapter.toScenario(
            germany,
            britain,
            location,
            attacking,
            defending,
            List.of(),
            List.of(),
            new BattleOptions(false, List.of(), List.of()));

    final CombatProfile infantry =
        scenario.defenders().counts().keySet().iterator().next().profile();
    assertThat(infantry.hitPoints()).isEqualTo(1);
    assertThat(infantry.onHit()).isEmpty();
  }
}
