package games.strategy.triplea.odds.calculator.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  /**
   * The support port (design §7): Revised's artillery lends +1 attack to infantry on offense (its
   * old-artillery rule, synthesized into a {@code UnitSupportAttachment}). The adapter must bake
   * that into a {@link SupportRule} and set the matching give/receive categories on the two
   * profiles, so the resolver's category match finds artillery as the giver and infantry as the
   * receiver. Uses Revised rather than TWW because its artillery support is the canonical
   * single-giver/single-receiver shape the v1 model represents exactly.
   */
  @Test
  void artillerySupportBakesIntoARuleAndMatchingGiveReceiveCategories() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final Collection<Unit> attacking = GameDataTestUtil.artillery(revised).create(1, russians);
    attacking.addAll(GameDataTestUtil.infantry(revised).create(1, russians));
    final Collection<Unit> defending = GameDataTestUtil.infantry(revised).create(1, germans);

    final BattleScenario scenario =
        adapter.toScenario(
            russians,
            germans,
            germany,
            attacking,
            defending,
            List.of(),
            List.of(),
            new BattleOptions(false, List.of(), List.of()));

    final CombatProfile artilleryProfile = profileOf(scenario.attackers(), "artillery");
    final CombatProfile infantryProfile = profileOf(scenario.attackers(), "infantry");
    assertThat(artilleryProfile.gives()).isNotEmpty();
    assertThat(artilleryProfile.receives()).isEmpty();
    assertThat(infantryProfile.gives()).isEmpty();
    assertThat(infantryProfile.receives()).isNotEmpty();

    final SupportRule rule =
        scenario.support().stream()
            .filter(r -> artilleryProfile.gives().contains(r.from()))
            .findFirst()
            .orElseThrow();
    assertThat(infantryProfile.receives()).contains(rule.to());
    assertThat(rule.side()).isEqualTo(Side.OFFENSE);
    assertThat(rule.appliesToStrength()).as("classic artillery boosts attack strength").isTrue();
    assertThat(rule.bonus()).isEqualTo(1);
    assertThat(rule.usesPerGiver()).as("one artillery supports one infantry").isEqualTo(1);
    assertThat(rule.firstRoundOnly()).isFalse();
  }

  /**
   * A {@link UnitSupportAttachment} whose dice is both roll and strength ({@code
   * dice="roll:strength"}) feeds the engine's strength pool and its roll pool independently —
   * {@code CombatValueBuilder} builds one {@code SupportCalculator} and filters it by {@code
   * getStrength()} and by {@code getRoll()}. The adapter mirrors that by baking two {@link
   * SupportRule}s from the one attachment, one per partition; the older adapter filed it under a
   * single {@code appliesToStrength} flag and dropped the roll half. Injected onto Revised (no
   * bundled map declares a dual support) as russian armour lending infantry both attack and rolls.
   */
  @Test
  void dualStrengthAndRollAttachmentEmitsARuleForEachPartition() throws GameParseException {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final UnitType armour = GameDataTestUtil.armour(revised);
    injectDualSupport(revised, armour, GameDataTestUtil.infantry(revised), russians, 2);

    final Collection<Unit> attacking = armour.create(1, russians);
    attacking.addAll(GameDataTestUtil.infantry(revised).create(1, russians));
    final Collection<Unit> defending = GameDataTestUtil.infantry(revised).create(1, germans);

    final BattleScenario scenario =
        adapter.toScenario(
            russians,
            germans,
            germany,
            attacking,
            defending,
            List.of(),
            List.of(),
            new BattleOptions(false, List.of(), List.of()));

    final CombatProfile armourProfile = profileOf(scenario.attackers(), "armour");
    final List<SupportRule> emitted =
        scenario.support().stream()
            .filter(rule -> armourProfile.gives().contains(rule.from()))
            .toList();
    assertThat(emitted)
        .as("one attachment that is both strength and roll bakes into two rules")
        .hasSize(2);
    assertThat(emitted)
        .extracting(SupportRule::appliesToStrength)
        .containsExactlyInAnyOrder(true, false);
    assertThat(emitted)
        .allSatisfy(
            rule -> {
              assertThat(rule.bonus()).isEqualTo(2);
              assertThat(rule.side()).isEqualTo(Side.OFFENSE);
              assertThat(rule.fromEnemy()).isFalse();
            });
  }

  /**
   * {@code rulesProfile()} bakes each of the six {@link Properties} rule-flag getters onto the
   * matching {@link RulesProfile} field — a straight field-for-field map, pinned here at the
   * adapter altitude since nothing exercised it directly before. Every flag is flipped off its
   * REVISED default so each case proves the getter is actually read, not that a default happens to
   * agree: {@code WW2V2} and {@code Submersible Subs} default {@code true} (both {@code
   * editable="false"}, so flipped through the editable store), the other four default {@code
   * false}.
   */
  @Test
  void bakesWw2v2FromProperties() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    setBooleanProperty(revised, Constants.WW2V2, false);
    assertThat(scenarioOn(revised).rules().ww2v2()).isFalse();
  }

  @Test
  void bakesDefendingSubsSneakAttackFromProperties() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    revised.getProperties().set(Constants.DEFENDING_SUBS_SNEAK_ATTACK, true);
    assertThat(scenarioOn(revised).rules().defendingSubsSneakAttack()).isTrue();
  }

  @Test
  void bakesTransportCasualtiesRestrictedFromProperties() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    revised.getProperties().set(Constants.TRANSPORT_CASUALTIES_RESTRICTED, true);
    assertThat(scenarioOn(revised).rules().transportCasualtiesRestricted()).isTrue();
  }

  @Test
  void bakesSubmersibleSubsFromProperties() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    setBooleanProperty(revised, Constants.SUBMERSIBLE_SUBS, false);
    assertThat(scenarioOn(revised).rules().submersibleSubs()).isFalse();
  }

  @Test
  void bakesSubmarinesDefendingMaySubmergeOrRetreatFromProperties() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    revised.getProperties().set(Constants.SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT, true);
    assertThat(scenarioOn(revised).rules().submarinesDefendingMaySubmergeOrRetreat()).isTrue();
  }

  @Test
  void bakesLhtrHeavyBombersFromProperties() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    enableLhtrHeavyBombers(revised);
    assertThat(scenarioOn(revised).rules().lhtrHeavyBombers()).isTrue();
  }

  private BattleScenario scenarioOn(final GameData revised) {
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final Collection<Unit> attacking = GameDataTestUtil.infantry(revised).create(1, russians);
    final Collection<Unit> defending = GameDataTestUtil.infantry(revised).create(1, germans);
    return adapter.toScenario(
        russians,
        germans,
        germany,
        attacking,
        defending,
        List.of(),
        List.of(),
        new BattleOptions(false, List.of(), List.of()));
  }

  /**
   * The LHTR heavy-bombers property is a map-level rule, not a unit attribute, so the adapter bakes
   * {@code CHOOSE_BEST_ROLL} onto a non-AA profile from the property — mirroring {@code
   * MainOffenseCombatValue#chooseBestRoll}. An AA gun is exempt: {@code AaOffenseCombatValue} and
   * {@code AaDefenseCombatValue} force chooseBestRoll off, so it must not receive the flag even
   * under LHTR. Pinned on plain infantry (no own {@code chooseBestRoll}) so the flag's only source
   * is the property.
   */
  @Test
  void lhtrHeavyBombersBakesChooseBestRollOntoNonAaProfilesButNotAaGuns() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final Collection<Unit> attacking = GameDataTestUtil.infantry(revised).create(1, russians);
    final Collection<Unit> defending = GameDataTestUtil.infantry(revised).create(1, germans);
    defending.addAll(GameDataTestUtil.aaGun(revised).create(1, germans));

    final CombatProfile infantryWithoutLhtr =
        profileOf(
            scenarioFor(russians, germans, germany, attacking, defending).attackers(), "infantry");
    assertThat(infantryWithoutLhtr.flags()).doesNotContain(CombatFlag.CHOOSE_BEST_ROLL);

    enableLhtrHeavyBombers(revised);
    final BattleScenario withLhtr = scenarioFor(russians, germans, germany, attacking, defending);
    assertThat(profileOf(withLhtr.attackers(), "infantry").flags())
        .contains(CombatFlag.CHOOSE_BEST_ROLL);
    assertThat(profileOf(withLhtr.defenders(), "aaGun").flags())
        .as("AA fire never takes its best roll, so it stays off even under LHTR")
        .doesNotContain(CombatFlag.CHOOSE_BEST_ROLL);
  }

  /**
   * Bug 1 of the AA stat-source gap (scope §2a): {@code profileFor} bakes every unit's offensive
   * strength from the normal {@code getAttack} family, so an {@code isAaForCombatOnly} gun gets its
   * near-zero normal attack rather than its AA firepower. The engine draws offensive AA from a
   * disjoint getter, {@code getOffensiveAttackAa}. A pure aaGun has {@code offensiveAttackAa == 0},
   * indistinguishable from its {@code attack == 0}, so the fixture raises the AA value to 2 — the
   * two sources then disagree and the baked {@code attack()} must track the AA source, not the
   * normal one.
   *
   * <p>{@link CombatProfile} carries no die-sides field, so the {@code *MaxDieSides} half of the AA
   * stat family is not representable today and is not asserted here — only the strength source is
   * pinned.
   */
  @Test
  void aaUnitProfileUsesAaAttackNotNormalAttack() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final UnitType aaGun = GameDataTestUtil.aaGun(revised);
    final UnitAttachment aa = aaGun.getUnitAttachment();
    // Give the gun a real offensive-AA value so the AA source and the 0 normal attack disagree — a
    // same-stats gun could not tell which getter the adapter read.
    aa.setOffensiveAttackAa(2);
    final Collection<Unit> attacking = aaGun.create(1, russians);
    final Collection<Unit> defending = GameDataTestUtil.infantry(revised).create(1, germans);

    final BattleScenario scenario =
        adapter.toScenario(
            russians,
            germans,
            germany,
            attacking,
            defending,
            List.of(),
            List.of(),
            new BattleOptions(false, List.of(), List.of()));

    assertThat(aa.getAttack(russians))
        .as("fixture guard: the normal attack is 0, distinct from the AA value")
        .isEqualTo(0);
    assertThat(profileOf(scenario.attackers(), "aaGun").attack())
        .as("offensive AA firepower from getOffensiveAttackAa, not the 0 normal attack")
        .isEqualTo(aa.getOffensiveAttackAa(russians));
  }

  /**
   * The defensive half of scope §2a bug 1: a defending aaGun's baked {@code defense()} must come
   * from {@code getAttackAa} (its value against attacking air, 1 for a standard gun), not its 0
   * normal defense. {@link CombatProfile} carries no die-sides field, so only the strength source
   * is pinned; the AA-specific {@code maxDieSides} denominator is unrepresentable and out of scope.
   */
  @Test
  void aaUnitDefenseProfileUsesAttackAaNotNormalDefense() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final UnitType aaGun = GameDataTestUtil.aaGun(revised);
    final UnitAttachment aa = aaGun.getUnitAttachment();
    final Collection<Unit> attacking = GameDataTestUtil.infantry(revised).create(1, russians);
    final Collection<Unit> defending = aaGun.create(1, germans);

    final BattleScenario scenario =
        adapter.toScenario(
            russians,
            germans,
            germany,
            attacking,
            defending,
            List.of(),
            List.of(),
            new BattleOptions(false, List.of(), List.of()));

    assertThat(aa.getDefense(germans))
        .as("fixture guard: the normal defense is 0, distinct from the AA value")
        .isEqualTo(0);
    assertThat(profileOf(scenario.defenders(), "aaGun").defense())
        .as("defensive AA firepower from getAttackAa, not the 0 normal defense")
        .isEqualTo(aa.getAttackAa(germans));
  }

  /**
   * The roll-family twin of bug 1: an AA profile's {@code rolls()} must come from {@code
   * getMaxAaAttacks}, not the normal {@code getAttackRolls} family. A default gun's {@code
   * maxAaAttacks} is {@code -1} (infinite), which is not a representable static die count and so
   * collapses to 1; a map-set finite value is preserved verbatim. Only strength is pinned by the
   * two reds above — this pins the roll source, which coincides with normal {@code attackRolls} at
   * 1 for a stock gun and would otherwise go uncovered.
   */
  @Test
  void aaUnitProfileRollsComeFromMaxAaAttacksNotAttackRolls() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final UnitType aaGun = GameDataTestUtil.aaGun(revised);
    final UnitAttachment aa = aaGun.getUnitAttachment();
    final Collection<Unit> defending = GameDataTestUtil.infantry(revised).create(1, germans);

    assertThat(aa.getMaxAaAttacks())
        .as("fixture guard: a stock gun's maxAaAttacks is -1 (infinite)")
        .isEqualTo(-1);
    assertThat(
            profileOf(
                    scenarioFor(russians, germans, germany, aaGun.create(1, russians), defending)
                        .attackers(),
                    "aaGun")
                .rolls())
        .as("infinite maxAaAttacks collapses to a single static die")
        .isEqualTo(1);

    aa.setMaxAaAttacks(3);
    assertThat(
            profileOf(
                    scenarioFor(russians, germans, germany, aaGun.create(1, russians), defending)
                        .attackers(),
                    "aaGun")
                .rolls())
        .as("a map-set finite maxAaAttacks is baked verbatim as the AA roll count")
        .isEqualTo(3);
  }

  /**
   * The per-round AA dice cap needs the raw per-gun {@code getMaxAaAttacks} (-1 = infinite), which
   * {@code rolls} discards by collapsing -1 to 1; the profile carries it verbatim so the fire-time
   * cap can distinguish an infinite gun from a one-shot gun. A map-set finite value is baked as-is.
   */
  @Test
  void aaUnitProfileCarriesRawMaxAaAttacksForThePerRoundCap() {
    final GameData revised = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = GameDataTestUtil.russians(revised);
    final GamePlayer germans = GameDataTestUtil.germans(revised);
    final Territory germany = GameDataTestUtil.territory("Germany", revised);
    final UnitType aaGun = GameDataTestUtil.aaGun(revised);
    final UnitAttachment aa = aaGun.getUnitAttachment();
    final Collection<Unit> defending = GameDataTestUtil.infantry(revised).create(1, germans);

    assertThat(
            profileOf(
                    scenarioFor(russians, germans, germany, aaGun.create(1, russians), defending)
                        .attackers(),
                    "aaGun")
                .maxAaAttacks())
        .as("a stock infinite gun keeps its raw -1, not the roll-count's collapsed 1")
        .isEqualTo(-1);

    aa.setMaxAaAttacks(3);
    assertThat(
            profileOf(
                    scenarioFor(russians, germans, germany, aaGun.create(1, russians), defending)
                        .attackers(),
                    "aaGun")
                .maxAaAttacks())
        .as("a map-set finite per-gun cap is baked verbatim")
        .isEqualTo(3);
  }

  private BattleScenario scenarioFor(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending) {
    return adapter.toScenario(
        attacker,
        defender,
        location,
        attacking,
        defending,
        List.of(),
        List.of(),
        new BattleOptions(false, List.of(), List.of()));
  }

  // 'LHTR Heavy Bombers' is an editable property; get() reads editable entries before the map that
  // set(String, Object) writes, so it must be flipped on the editable property itself.
  private static void enableLhtrHeavyBombers(final GameData data) {
    for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(Constants.LHTR_HEAVY_BOMBERS)) {
        ((BooleanProperty) property).setValue(true);
        return;
      }
    }
    throw new IllegalStateException("LHTR Heavy Bombers property not found");
  }

  // An editable rule flag is read from the editable-property store before the map that set(String,
  // Object) writes, so it must be flipped in place; a non-editable flag lives only in that map, so
  // it falls through to set(). This override handles either kind.
  private static void setBooleanProperty(
      final GameData data, final String propertyName, final boolean value) {
    for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(propertyName)) {
        ((BooleanProperty) property).setValue(value);
        return;
      }
    }
    data.getProperties().set(propertyName, value);
  }

  /**
   * Attaches an allied support that is both strength and roll ({@code dice="roll:strength"}):
   * {@code giver} lends {@code target} {@code bonus} attack and {@code bonus} rolls when attacking.
   * Injected before the support list is cached so the adapter reads it, the way Revised's
   * old-artillery support is synthesized.
   */
  private static void injectDualSupport(
      final GameData data,
      final UnitType giver,
      final UnitType target,
      final GamePlayer giverOwner,
      final int bonus)
      throws GameParseException {
    final UnitSupportAttachment rule =
        new UnitSupportAttachment(Constants.SUPPORT_ATTACHMENT_PREFIX + "DualTest", giver, data);
    rule.setDice("roll:strength");
    rule.setFaction("allied");
    rule.setSide("offence");
    rule.setBonus(bonus);
    rule.setBonusType("dualTest");
    rule.setNumber(2);
    rule.setUnitType(Set.of(target));
    rule.setPlayers(List.of(giverOwner));
    giver.addAttachment(rule.getName(), rule);
  }

  private static CombatProfile profileOf(final Force force, final String typeName) {
    return force.counts().keySet().stream()
        .map(Key::profile)
        .filter(profile -> profile.type().equals(new UnitTypeId(typeName)))
        .findFirst()
        .orElseThrow();
  }
}
