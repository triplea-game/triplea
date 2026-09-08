package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.CargoRule;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.DamageState;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportCategory;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.odds.calculator.context.reference.OolCasualtyOrder;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceRetreatPolicy;
import games.strategy.triplea.util.TuvCostsCalculator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * The anti-corruption layer, living outside the engine-free calc core — the one seam that imports
 * {@code games.strategy.engine.data.*} (design §9, invariant 4). Bakes a fully self-contained
 * {@link BattleScenario} in, and maps survivor counts back to representative units for the UI out.
 *
 * <p>Everything a downstream rule reads is baked here from GameData: combat stats (with territory
 * effects folded in), the {@code onHit()} damage chain, and the intrinsic combat flags. Nothing in
 * the produced scenario holds a reference back into {@code GameData}.
 */
public class GameDataBattleAdapter {

  /**
   * Bakes a self-contained {@link BattleScenario} without holding any {@code GameData} lock (design
   * §3). The retry-once guards the collection walks: a force or support collection mutating
   * mid-walk surfaces as a {@link ConcurrentModificationException} that a single retry against a
   * now-quiescent read clears; a second failure propagates rather than spinning. Scalar property
   * reads cannot raise a CME, so the guard does not cover them — {@link #bake} snapshots them once
   * up front to keep them out of the long iteration window instead.
   */
  public BattleScenario toScenario(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> effects,
      final BattleOptions options) {
    try {
      return bake(attacker, defender, location, attacking, defending, bombarding, effects, options);
    } catch (final ConcurrentModificationException retryable) {
      return bake(attacker, defender, location, attacking, defending, bombarding, effects, options);
    }
  }

  /**
   * The single-pass bake. Combat-fungible units fold into one {@link CombatProfile} bucket
   * (invariant 1); territory {@link TerritoryEffect} bonuses fold into the profile's
   * attack/defense; and {@code whenHitPointsDamagedChangesInto} becomes the {@code onHit()}
   * migration chain (invariant 2).
   *
   * <p>The caller's live force collections are snapshotted up front so every downstream walk
   * iterates a stable copy; the produced scenario holds only immutable collections so N simulator
   * threads can share it.
   */
  private BattleScenario bake(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> effects,
      final BattleOptions options) {
    final GameData data = location.getData();
    // Read the scalar game properties once, up front. Unlike the collection walks below, a racing
    // mutation of the HashMap-backed GameProperties would not throw a CME the retry could catch, so
    // snapshotting them here keeps their read out of the long iteration window.
    final int diceSides = data.getDiceSides();
    final boolean lowLuck = Properties.getLowLuck(data.getProperties());
    final RulesProfile rules = rulesProfile(data);
    final List<Unit> attackers = List.copyOf(attacking);
    final List<Unit> defenders = List.copyOf(defending);
    final List<Unit> bombarders = List.copyOf(bombarding);
    final SupportModel support = supportModel(data, attacker, defender);
    final boolean seaBattle = location.isWater();
    return new BattleScenario(
        toForce(
            attackers,
            attacker,
            Side.OFFENSE,
            effects,
            support,
            seaBattle,
            rules.lhtrHeavyBombers()),
        toForce(
            defenders,
            defender,
            Side.DEFENSE,
            effects,
            support,
            seaBattle,
            rules.lhtrHeavyBombers()),
        toForce(
            bombarders,
            attacker,
            Side.OFFENSE,
            effects,
            support,
            seaBattle,
            rules.lhtrHeavyBombers()),
        dependents(
            attackers,
            attacker,
            defenders,
            defender,
            seaBattle,
            effects,
            support,
            rules.lhtrHeavyBombers()),
        rules,
        support.rules(),
        costs(data, attacker, defender),
        anyAmphibious(attackers) || options.amphibious(),
        diceSides,
        lowLuck,
        // keepOneAttackingLandUnit rides in BattleOptions for interface parity but is not threaded
        // into the scenario — the simulator hardcodes it off (a deferred fidelity item).
        new ReferenceRetreatPolicy(
            options.retreatAfterRound(),
            options.retreatAfterXUnitsLeft(),
            options.retreatWhenOnlyAirLeft()),
        new ReferenceRetreatPolicy(
            options.retreatAfterRound(),
            options.retreatAfterXUnitsLeft(),
            options.retreatWhenOnlyAirLeft()),
        new OolCasualtyOrder(options.attackerOol()),
        new OolCasualtyOrder(options.defenderOol()));
  }

  /**
   * Groups a side's units into merged {@code (profile, ACTIVE)} buckets with summed counts. In a
   * sea battle a side's land units are non-combatant cargo, marked {@code IS_DEPENDENT} so they
   * neither fire nor are targeted and leave only through the carrier cascade — but only when the
   * side actually fields a carrier to tie them to, so a stray land unit with no transport is not
   * left an un-killable dependent.
   */
  private static Force toForce(
      final Collection<Unit> units,
      final GamePlayer player,
      final Side side,
      final Collection<TerritoryEffect> effects,
      final SupportModel support,
      final boolean seaBattle,
      final boolean lhtrHeavyBombers) {
    final boolean hasCarrier = seaBattle && anyCarrier(units);
    final Map<Key, Integer> counts = new LinkedHashMap<>();
    for (final Unit unit : units) {
      final boolean dependent =
          hasCarrier && domainOf(unit.getType().getUnitAttachment()) == Domain.LAND;
      final CombatProfile profile =
          profileFor(
              unit.getType(),
              player,
              side,
              effects,
              unit.getHits(),
              support,
              dependent,
              lhtrHeavyBombers);
      final Key key = new Key(profile, Lifecycle.ACTIVE);
      counts.merge(key, 1, Integer::sum);
    }
    return new Force(Map.copyOf(counts));
  }

  private static boolean anyCarrier(final Collection<Unit> units) {
    return units.stream()
        .anyMatch(unit -> unit.getType().getUnitAttachment().getTransportCapacity() > 0);
  }

  /**
   * Builds the {@link CombatProfile} for a unit type at a given cumulative damage level, recursing
   * along {@code whenHitPointsDamagedChangesInto} to produce the {@code onHit()} chain.
   *
   * <p>{@code hitPoints} counts down remaining hits (max HP minus hits taken); a profile with one
   * remaining HP has no successor and dies on its next hit. The damage total drives the countdown,
   * not the successor type's own HP: when the next hit crosses a {@code
   * whenHitPointsDamagedChangesInto} threshold the unit takes on that type's stats but keeps its
   * cumulative hit count, since the engine copies damage across the transform ({@code
   * TransformDamagedUnitsHistoryChange} keys the map on the unit's hit count and {@code
   * UnitUtils#translateAttributesToOtherUnits} carries the hits over).
   */
  private static CombatProfile profileFor(
      final UnitType type,
      final GamePlayer player,
      final Side side,
      final Collection<TerritoryEffect> effects,
      final int hits,
      final SupportModel support,
      final boolean dependent,
      final boolean lhtrHeavyBombers) {
    final UnitAttachment ua = type.getUnitAttachment();
    final int remaining = ua.getHitPoints() - hits;
    // AA fire draws from a stat family disjoint from normal combat: offensive AA from
    // 'getOffensiveAttackAa', defensive AA from 'getAttackAa', dice from 'getMaxAaAttacks' — never
    // the 'getAttack'/'getAttackRolls' family a pure gun would read as near-zero. Both AA strength
    // getters already fold the tech bonus and clamp to the AA die-sides, and no AA territory-combat
    // bonus exists, so the normal territory bonus is omitted. Standard AA leaves *AaMaxDieSides at
    // the game diceSides the single-scalar roller divides by, so strength/diceSides matches the
    // engine's probability; a map-set non-standard denominator is an unrepresentable, deferred gap.
    // One-profile-per-unit limit: an AA unit takes the AA family for its whole profile, so a
    // dual-role unit that is both AA and a real combatant loses its normal main-phase fire here. No
    // stock unit is both (stock guns are attack-0 infrastructure), so this is an accepted gap.
    final boolean aa = ua.isAaForCombatOnly();
    final int attack =
        aa
            ? ua.getOffensiveAttackAa(player)
            : ua.getAttack(player)
                + TerritoryEffectHelper.getTerritoryCombatBonus(type, effects, false);
    final int defense =
        aa
            ? ua.getAttackAa(player)
            : ua.getDefense(player)
                + TerritoryEffectHelper.getTerritoryCombatBonus(type, effects, true);
    // 'maxAaAttacks' defaults to -1 (infinite), not a representable static die count, so both -1 and
    // 0 collapse to 1; a map-set finite value (eg 3) is preserved.
    final int rolls =
        aa
            ? Math.max(1, ua.getMaxAaAttacks())
            : side == Side.OFFENSE ? ua.getAttackRolls(player) : ua.getDefenseRolls(player);
    return new CombatProfile(
        new UnitTypeId(type.getName()),
        attack,
        defense,
        rolls,
        // AA fires only through 'maxRoundsAa' (default 1 = round 1 only); -1 for non-AA units, which
        // the resolver never reads. Gates the temporal AA re-fire the engine bounds per round.
        aa ? ua.getMaxRoundsAa() : -1,
        remaining,
        domainOf(ua),
        new DamageState(hits),
        support.gives().getOrDefault(type.getName(), SupportCategory.NONE),
        support.receives().getOrDefault(type.getName(), SupportCategory.NONE),
        flagsOf(ua, dependent, lhtrHeavyBombers),
        successor(
            type, player, side, effects, hits, remaining, support, dependent, lhtrHeavyBombers));
  }

  private static CombatProfile successor(
      final UnitType type,
      final GamePlayer player,
      final Side side,
      final Collection<TerritoryEffect> effects,
      final int hits,
      final int remaining,
      final SupportModel support,
      final boolean dependent,
      final boolean lhtrHeavyBombers) {
    if (remaining <= 1) {
      return null;
    }
    final int nextHits = hits + 1;
    final Map<Integer, Tuple<Boolean, UnitType>> changesInto =
        type.getUnitAttachment().getWhenHitPointsDamagedChangesInto();
    // Damage carries across a transform, so the successor keeps the cumulative hit count and only
    // its type and stats change.
    final UnitType nextType =
        changesInto.containsKey(nextHits) ? changesInto.get(nextHits).getSecond() : type;
    return profileFor(
        nextType, player, side, effects, nextHits, support, dependent, lhtrHeavyBombers);
  }

  private static Domain domainOf(final UnitAttachment ua) {
    if (ua.isSea()) {
      return Domain.SEA;
    }
    if (ua.isAir()) {
      return Domain.AIR;
    }
    return Domain.LAND;
  }

  /**
   * The intrinsic combat abilities baked from GameData, never string-matched in the core. {@code
   * CAN_SUBMERGE} tracks {@code canEvade} — the eligibility to submerge; whether it may actually do
   * so is relational (a blocking enemy destroyer) and computed downstream. {@code
   * CANNOT_BE_TARGETED_BY_ALL} tracks {@code canNotBeTargetedBy} separately: a Revised sub evades
   * but is still air-targetable, so air-immunity and the submerge-vs-air trigger cannot ride on
   * {@code canEvade} — see {@code AirVsNonSubsStep#airWillMissSubs} and {@code
   * DummyPlayer#retreatQuery}.
   */
  private static EnumSet<CombatFlag> flagsOf(
      final UnitAttachment ua, final boolean dependent, final boolean lhtrHeavyBombers) {
    final EnumSet<CombatFlag> flags = EnumSet.noneOf(CombatFlag.class);
    if (ua.getIsFirstStrike()) {
      flags.add(CombatFlag.FIRST_STRIKE);
    }
    if (ua.isAaForCombatOnly()) {
      flags.add(CombatFlag.IS_AA);
    }
    // LHTR heavy bombers make a multi-roll unit take its best die, so the flag rides the map
    // property as well as the unit's own attribute — mirroring
    // MainOffenseCombatValue#chooseBestRoll.
    // AA fire is exempt: AaOffenseCombatValue and AaDefenseCombatValue force chooseBestRoll off, so
    // an AA gun rolls all its dice; the flag is baked for non-AA units only.
    if ((lhtrHeavyBombers || ua.getChooseBestRoll()) && !ua.isAaForCombatOnly()) {
      flags.add(CombatFlag.CHOOSE_BEST_ROLL);
    }
    if (ua.getCanEvade()) {
      flags.add(CombatFlag.CAN_SUBMERGE);
    }
    if (!ua.getCanNotBeTargetedBy().isEmpty()) {
      flags.add(CombatFlag.CANNOT_BE_TARGETED_BY_ALL);
    }
    if (ua.isDestroyer()) {
      flags.add(CombatFlag.IS_DESTROYER);
    }
    // Mirrors Matches.unitIsSeaTransportButNotCombatSeaTransport: transportCapacity != -1 (a carrier
    // uses carrierCapacity and stays at -1) and not a combat transport, which fights and so is never
    // the protected casualty class. Distinct from the anyCarrier '> 0' cargo-cascade predicate.
    if (ua.getTransportCapacity() != -1 && ua.isSea() && !ua.isCombatTransport()) {
      flags.add(CombatFlag.IS_TRANSPORT);
    }
    if (dependent) {
      flags.add(CombatFlag.IS_DEPENDENT);
    }
    return flags;
  }

  private static boolean anyAmphibious(final Collection<Unit> attacking) {
    return attacking.stream().anyMatch(Unit::getWasAmphibious);
  }

  /**
   * The cargo cascade for a sea battle: each side's transports become {@link CargoRule}s keyed by
   * the transport's own {@link CombatProfile}, so a sunk transport sheds the land units it carried.
   * A land battle carries no sea cargo, so it gets an empty map.
   *
   * <p>Compact loading: capacity is a transport's full load, and a side's land cargo is one pool
   * the greedy cascade empties transport-load by transport-load — early transport losses take a
   * full load rather than one unit spread across the fleet.
   *
   * <p>v1 scope: one cargo type per side (the first land type present), keyed to undamaged
   * transport profiles. Mixed loads, per-unit cargo linkage, and damaged transports are unmodeled
   * follow-ups.
   */
  private static Dependents dependents(
      final Collection<Unit> attacking,
      final GamePlayer attacker,
      final Collection<Unit> defending,
      final GamePlayer defender,
      final boolean seaBattle,
      final Collection<TerritoryEffect> effects,
      final SupportModel support,
      final boolean lhtrHeavyBombers) {
    if (!seaBattle) {
      return new Dependents(Map.of());
    }
    final Map<CombatProfile, CargoRule> rules = new LinkedHashMap<>();
    addCarrierRules(rules, attacking, attacker, Side.OFFENSE, effects, support, lhtrHeavyBombers);
    addCarrierRules(rules, defending, defender, Side.DEFENSE, effects, support, lhtrHeavyBombers);
    return new Dependents(Map.copyOf(rules));
  }

  private static void addCarrierRules(
      final Map<CombatProfile, CargoRule> rules,
      final Collection<Unit> units,
      final GamePlayer player,
      final Side side,
      final Collection<TerritoryEffect> effects,
      final SupportModel support,
      final boolean lhtrHeavyBombers) {
    UnitType cargoType = null;
    for (final Unit unit : units) {
      if (domainOf(unit.getType().getUnitAttachment()) == Domain.LAND) {
        cargoType = unit.getType();
        break;
      }
    }
    if (cargoType == null) {
      return;
    }
    final int cargoCost = Math.max(1, cargoType.getUnitAttachment().getTransportCost());
    final UnitTypeId cargoId = new UnitTypeId(cargoType.getName());
    final Set<UnitType> carrierTypes = new LinkedHashSet<>();
    for (final Unit unit : units) {
      if (unit.getType().getUnitAttachment().getTransportCapacity() > 0) {
        carrierTypes.add(unit.getType());
      }
    }
    for (final UnitType carrierType : carrierTypes) {
      final int capacity =
          Math.max(1, carrierType.getUnitAttachment().getTransportCapacity() / cargoCost);
      final CombatProfile carrier =
          profileFor(carrierType, player, side, effects, 0, support, false, lhtrHeavyBombers);
      rules.put(carrier, new CargoRule(cargoId, capacity));
    }
  }

  /**
   * Bakes the map's friendly strength/roll support into the calc's {@link SupportRule} model plus
   * the per-unit-type give/receive categories the resolver keys on. Each {@link
   * UnitSupportAttachment} becomes one rule per battle side it applies to, gated to the side whose
   * owner ({@code attacker} or {@code defender}) the attachment lists — mirroring the engine's
   * {@code SupportCalculator} owner match.
   *
   * <p>v1 scope, bounded by the singular {@link CombatProfile#gives()}/{@link
   * CombatProfile#receives()} fields: only friendly ({@code allied}) strength/roll support is
   * modeled. Deliberately unmodeled, each needing a multi-category profile the design defers: enemy
   * ({@code enemy}) debuff support, per-{@code bonusType} stacking caps, a unit that gives or
   * receives more than one distinct support (first attachment wins), Improved-Artillery tech
   * doubling, allied support beyond the two calc players, a rule that is both strength and roll
   * (strength wins), and AA support.
   */
  private static SupportModel supportModel(
      final GameData data, final GamePlayer attacker, final GamePlayer defender) {
    final List<SupportRule> rules = new ArrayList<>();
    final Map<String, SupportCategory> gives = new LinkedHashMap<>();
    final Map<String, SupportCategory> receives = new LinkedHashMap<>();
    for (final UnitSupportAttachment attachment :
        Set.copyOf(data.getUnitTypeList().getSupportRules())) {
      if (!attachment.getAllied()) {
        continue;
      }
      final UnitType giver = (UnitType) attachment.getAttachedTo();
      final SupportCategory category =
          new SupportCategory("support:" + giver.getName() + "/" + attachment.getName());
      final boolean appliesToStrength = attachment.getStrength();
      if (attachment.getOffence() && attachment.getPlayers().contains(attacker)) {
        rules.add(supportRule(attachment, category, appliesToStrength, Side.OFFENSE));
        register(gives, receives, giver, attachment, category);
      }
      if (attachment.getDefence() && attachment.getPlayers().contains(defender)) {
        rules.add(supportRule(attachment, category, appliesToStrength, Side.DEFENSE));
        register(gives, receives, giver, attachment, category);
      }
    }
    return new SupportModel(List.copyOf(rules), gives, receives);
  }

  private static SupportRule supportRule(
      final UnitSupportAttachment attachment,
      final SupportCategory category,
      final boolean appliesToStrength,
      final Side side) {
    // firstRoundOnly is always false: the engine's support model has no first-round-only flag.
    return new SupportRule(
        category,
        category,
        attachment.getBonus(),
        appliesToStrength,
        attachment.getNumber(),
        side,
        false);
  }

  /** Records the giver's emitted category and each receiver type's consumed one (first wins). */
  private static void register(
      final Map<String, SupportCategory> gives,
      final Map<String, SupportCategory> receives,
      final UnitType giver,
      final UnitSupportAttachment attachment,
      final SupportCategory category) {
    gives.putIfAbsent(giver.getName(), category);
    for (final UnitType receiver : attachment.getUnitType()) {
      receives.putIfAbsent(receiver.getName(), category);
    }
  }

  /** The baked support: the rules plus the per-unit-type give/receive categories they key on. */
  private record SupportModel(
      List<SupportRule> rules,
      Map<String, SupportCategory> gives,
      Map<String, SupportCategory> receives) {}

  private static RulesProfile rulesProfile(final GameData data) {
    final var properties = data.getProperties();
    return new RulesProfile(
        Properties.getWW2V2(properties),
        Properties.getDefendingSubsSneakAttack(properties),
        Properties.getTransportCasualtiesRestricted(properties),
        Properties.getSubmersibleSubs(properties),
        Properties.getSubmarinesDefendingMaySubmergeOrRetreat(properties),
        Properties.getLhtrHeavyBombers(properties));
  }

  /** Per-unit-type PU cost for caller-side TUV, merged across both players' cost schedules. */
  private static Map<UnitTypeId, Integer> costs(
      final GameData data, final GamePlayer attacker, final GamePlayer defender) {
    final TuvCostsCalculator calculator = new TuvCostsCalculator();
    final Map<UnitTypeId, Integer> cost = new LinkedHashMap<>();
    addCosts(cost, calculator.getCostsForTuv(attacker));
    addCosts(cost, calculator.getCostsForTuv(defender));
    return Map.copyOf(cost);
  }

  private static void addCosts(
      final Map<UnitTypeId, Integer> cost, final IntegerMap<UnitType> schedule) {
    for (final UnitType type : schedule.keySet()) {
      cost.putIfAbsent(new UnitTypeId(type.getName()), schedule.getInt(type));
    }
  }
}
