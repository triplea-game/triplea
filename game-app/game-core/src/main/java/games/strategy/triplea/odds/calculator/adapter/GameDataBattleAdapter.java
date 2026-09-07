package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
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
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.odds.calculator.context.reference.OolCasualtyOrder;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceRetreatPolicy;
import games.strategy.triplea.util.TuvCostsCalculator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
   * Bakes a self-contained {@link BattleScenario}. Combat-fungible units fold into one {@link
   * CombatProfile} bucket (invariant 1); territory {@link TerritoryEffect} bonuses fold into the
   * profile's attack/defense; and {@code whenHitPointsDamagedChangesInto} becomes the {@code
   * onHit()} migration chain (invariant 2).
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
    final GameData data = location.getData();
    return new BattleScenario(
        toForce(attacking, attacker, Side.OFFENSE, effects),
        toForce(defending, defender, Side.DEFENSE, effects),
        toForce(bombarding, attacker, Side.OFFENSE, effects),
        // TODO(adapter): transport/carrier cargo cascade — build from Unit#getTransporting once the
        // allocator's dependent handling is exercised by a test.
        new Dependents(Map.of()),
        rulesProfile(data),
        // TODO(adapter): SupportRules from UnitSupportAttachment, honoring AvailableSupports'
        // consume/sort order; deferred until a support scenario is under test (and paired with the
        // gives/receives categories left NONE on the profiles below).
        List.of(),
        costs(data, attacker, defender),
        anyAmphibious(attacking),
        // TODO(adapter): BattleOptions carries only retreatWhenOnlyAirLeft; the retreatAfterRound /
        // retreatAfterXUnitsLeft thresholds have no caller input yet, so they are disabled (-1).
        new ReferenceRetreatPolicy(-1, -1, options.retreatWhenOnlyAirLeft()),
        new ReferenceRetreatPolicy(-1, -1, options.retreatWhenOnlyAirLeft()),
        new OolCasualtyOrder(options.attackerOol()),
        new OolCasualtyOrder(options.defenderOol()));
  }

  /** Groups a side's units into merged {@code (profile, ACTIVE)} buckets with summed counts. */
  private static Force toForce(
      final Collection<Unit> units,
      final GamePlayer player,
      final Side side,
      final Collection<TerritoryEffect> effects) {
    final Map<Key, Integer> counts = new LinkedHashMap<>();
    for (final Unit unit : units) {
      final CombatProfile profile =
          profileFor(unit.getType(), player, side, effects, unit.getHits());
      final Key key = new Key(profile, Lifecycle.ACTIVE);
      counts.merge(key, 1, Integer::sum);
    }
    return new Force(counts);
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
      final int hits) {
    final UnitAttachment ua = type.getUnitAttachment();
    final int remaining = ua.getHitPoints() - hits;
    return new CombatProfile(
        new UnitTypeId(type.getName()),
        ua.getAttack(player) + TerritoryEffectHelper.getTerritoryCombatBonus(type, effects, false),
        ua.getDefense(player) + TerritoryEffectHelper.getTerritoryCombatBonus(type, effects, true),
        side == Side.OFFENSE ? ua.getAttackRolls(player) : ua.getDefenseRolls(player),
        remaining,
        domainOf(ua),
        new DamageState(hits),
        SupportCategory.NONE,
        SupportCategory.NONE,
        flagsOf(ua),
        successor(type, player, side, effects, hits, remaining));
  }

  private static CombatProfile successor(
      final UnitType type,
      final GamePlayer player,
      final Side side,
      final Collection<TerritoryEffect> effects,
      final int hits,
      final int remaining) {
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
    return profileFor(nextType, player, side, effects, nextHits);
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
   * so is relational (a blocking enemy destroyer) and computed downstream.
   */
  private static EnumSet<CombatFlag> flagsOf(final UnitAttachment ua) {
    final EnumSet<CombatFlag> flags = EnumSet.noneOf(CombatFlag.class);
    if (ua.getIsFirstStrike()) {
      flags.add(CombatFlag.FIRST_STRIKE);
    }
    if (ua.isAaForCombatOnly()) {
      flags.add(CombatFlag.IS_AA);
    }
    if (ua.getChooseBestRoll()) {
      flags.add(CombatFlag.CHOOSE_BEST_ROLL);
    }
    if (ua.getCanEvade()) {
      flags.add(CombatFlag.CAN_SUBMERGE);
    }
    if (ua.isDestroyer()) {
      flags.add(CombatFlag.IS_DESTROYER);
    }
    return flags;
  }

  private static boolean anyAmphibious(final Collection<Unit> attacking) {
    return attacking.stream().anyMatch(Unit::getWasAmphibious);
  }

  // TODO(adapter): a curated slice of the ~60-70 combat flags (design §3.1). The full port is
  // enumerated as the differential harness lights up rules that read them.
  private static RulesProfile rulesProfile(final GameData data) {
    final var properties = data.getProperties();
    final Map<String, Boolean> flags = new LinkedHashMap<>();
    flags.put("lowLuck", Properties.getLowLuck(properties));
    flags.put("ww2v2", Properties.getWW2V2(properties));
    flags.put("defendingSubsSneakAttack", Properties.getDefendingSubsSneakAttack(properties));
    flags.put(
        "transportCasualtiesRestricted", Properties.getTransportCasualtiesRestricted(properties));
    return new RulesProfile(flags);
  }

  /** Per-unit-type PU cost for caller-side TUV, merged across both players' cost schedules. */
  private static Map<UnitTypeId, Integer> costs(
      final GameData data, final GamePlayer attacker, final GamePlayer defender) {
    final TuvCostsCalculator calculator = new TuvCostsCalculator();
    final Map<UnitTypeId, Integer> cost = new LinkedHashMap<>();
    addCosts(cost, calculator.getCostsForTuv(attacker));
    addCosts(cost, calculator.getCostsForTuv(defender));
    return cost;
  }

  private static void addCosts(
      final Map<UnitTypeId, Integer> cost, final IntegerMap<UnitType> schedule) {
    for (final UnitType type : schedule.keySet()) {
      cost.putIfAbsent(new UnitTypeId(type.getName()), schedule.getInt(type));
    }
  }

  /**
   * Survivor counts back to concrete units for the UI's "average units remaining". Fungible by
   * profile incl. damage, so any units matching the counts will do; dead buckets are dropped.
   *
   * <p>The {@link Force} carries no owner, so representatives are created under the game's null
   * player — adequate for a display count, but a caller that needs owned units must remap. TODO
   * (adapter): thread the surviving side's {@link GamePlayer} through if owned representatives are
   * ever required.
   */
  public Collection<Unit> toRepresentativeUnits(final Force survivors, final GameData data) {
    final GamePlayer owner = data.getPlayerList().getNullPlayer();
    final List<Unit> units = new ArrayList<>();
    for (final Map.Entry<Key, Integer> entry : survivors.counts().entrySet()) {
      if (entry.getKey().state() == Lifecycle.DEAD) {
        continue;
      }
      final CombatProfile profile = entry.getKey().profile();
      final UnitType type = data.getUnitTypeList().getUnitTypeOrThrow(profile.type().name());
      units.addAll(type.create(entry.getValue(), owner, true, profile.damage().hitsTaken(), 0));
    }
    return units;
  }
}
