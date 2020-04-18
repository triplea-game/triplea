package games.strategy.triplea;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Extended unit for triplea games.
 *
 * <p>As with all game data components, changes made to this unit must be made through a Change
 * instance. Calling setters on this directly will not serialize the changes across the network.
 */
public class TripleAUnit extends Unit {
  public static final String TRANSPORTED_BY = "transportedBy";
  public static final String UNLOADED = "unloaded";
  public static final String LOADED_THIS_TURN = "wasLoadedThisTurn";
  public static final String UNLOADED_TO = "unloadedTo";
  public static final String UNLOADED_IN_COMBAT_PHASE = "wasUnloadedInCombatPhase";
  public static final String ALREADY_MOVED = "alreadyMoved";
  public static final String BONUS_MOVEMENT = "bonusMovement";
  public static final String SUBMERGED = "submerged";
  public static final String WAS_IN_COMBAT = "wasInCombat";
  public static final String LOADED_AFTER_COMBAT = "wasLoadedAfterCombat";
  public static final String UNLOADED_AMPHIBIOUS = "wasAmphibious";
  public static final String ORIGINATED_FROM = "originatedFrom";
  public static final String WAS_SCRAMBLED = "wasScrambled";
  public static final String MAX_SCRAMBLE_COUNT = "maxScrambleCount";
  public static final String WAS_IN_AIR_BATTLE = "wasInAirBattle";
  public static final String LAUNCHED = "launched";
  public static final String AIRBORNE = "airborne";
  public static final String CHARGED_FLAT_FUEL_COST = "chargedFlatFuelCost";
  private static final long serialVersionUID = 8811372406957115036L;

  // the transport that is currently transporting us
  private TripleAUnit transportedBy = null;
  // the units we have unloaded this turn
  private List<Unit> unloaded = List.of();
  // was this unit loaded this turn?
  private boolean wasLoadedThisTurn = false;
  // the territory this unit was unloaded to this turn
  private Territory unloadedTo = null;
  // was this unit unloaded in combat phase this turn?
  private boolean wasUnloadedInCombatPhase = false;
  // movement used this turn
  private BigDecimal alreadyMoved = BigDecimal.ZERO;
  // movement used this turn
  private int bonusMovement = 0;
  // amount of damage unit has sustained
  private int unitDamage = 0;
  // is this submarine submerged
  private boolean submerged = false;
  // original owner of this unit
  private GamePlayer originalOwner = null;
  // Was this unit in combat
  private boolean wasInCombat = false;
  private boolean wasLoadedAfterCombat = false;
  private boolean wasAmphibious = false;
  // the territory this unit started in (for use with scrambling)
  private Territory originatedFrom = null;
  private boolean wasScrambled = false;
  private int maxScrambleCount = -1;
  private boolean wasInAirBattle = false;
  private boolean disabled = false;
  // the number of airborne units launched by this unit this turn
  private int launched = 0;
  // was this unit airborne and launched this turn
  private boolean airborne = false;
  // was charged flat fuel cost already this turn
  private boolean chargedFlatFuelCost = false;

  public TripleAUnit(final UnitType type, final GamePlayer owner, final GameData data) {
    super(type, owner, data);
  }

  /**
   * Returns a tuple whose first element indicates the minimum movement remaining for the specified
   * collection of units, and whose second element indicates the maximum movement remaining for the
   * specified collection of units.
   */
  public static Tuple<BigDecimal, BigDecimal> getMinAndMaxMovementLeft(
      final Collection<Unit> units) {
    BigDecimal min = new BigDecimal(100000);
    BigDecimal max = BigDecimal.ZERO;
    for (final Unit u : units) {
      final BigDecimal left = ((TripleAUnit) u).getMovementLeft();
      if (left.compareTo(max) > 0) {
        max = left;
      }
      if (left.compareTo(max) < 0) {
        min = left;
      }
    }
    if (max.compareTo(min) < 0) {
      min = max;
    }
    return Tuple.of(min, max);
  }

  public static int getProductionPotentialOfTerritory(
      final Collection<Unit> unitsAtStartOfStepInTerritory,
      final Territory producer,
      final GamePlayer player,
      final GameData data,
      final boolean accountForDamage,
      final boolean mathMaxZero) {
    return getHowMuchCanUnitProduce(
        getBiggestProducer(unitsAtStartOfStepInTerritory, producer, player, data, accountForDamage),
        producer,
        player,
        data,
        accountForDamage,
        mathMaxZero);
  }

  /**
   * Returns the unit from the specified collection that has the largest production capacity within
   * the specified territory.
   *
   * @param accountForDamage {@code true} if the production capacity should account for unit damage;
   *     otherwise {@code false}.
   */
  public static Unit getBiggestProducer(
      final Collection<Unit> units,
      final Territory producer,
      final GamePlayer player,
      final GameData data,
      final boolean accountForDamage) {
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
            .and(Matches.unitIsBeingTransported().negate())
            .and(producer.isWater() ? Matches.unitIsLand().negate() : Matches.unitIsSea().negate());
    final Collection<Unit> factories = CollectionUtils.getMatches(units, factoryMatch);
    if (factories.isEmpty()) {
      return null;
    }
    final IntegerMap<Unit> productionPotential = new IntegerMap<>();
    Unit highestUnit = factories.iterator().next();
    int highestCapacity = Integer.MIN_VALUE;
    for (final Unit u : factories) {
      final int capacity =
          getHowMuchCanUnitProduce(u, producer, player, data, accountForDamage, false);
      productionPotential.put(u, capacity);
      if (capacity > highestCapacity) {
        highestCapacity = capacity;
        highestUnit = u;
      }
    }
    return highestUnit;
  }

  /**
   * Returns the production capacity for the specified unit within the specified territory.
   *
   * @param accountForDamage {@code true} if the production capacity should account for unit damage;
   *     otherwise {@code false}.
   * @param mathMaxZero {@code true} if a negative production capacity should be rounded to zero;
   *     {@code false} to allow a negative production capacity.
   */
  public static int getHowMuchCanUnitProduce(
      final Unit u,
      final Territory producer,
      final GamePlayer player,
      final GameData data,
      final boolean accountForDamage,
      final boolean mathMaxZero) {
    if (u == null) {
      return 0;
    }
    if (!Matches.unitCanProduceUnits().test(u)) {
      return 0;
    }
    final UnitAttachment ua = UnitAttachment.get(u.getType());
    final TripleAUnit taUnit = (TripleAUnit) u;
    final TerritoryAttachment ta = TerritoryAttachment.get(producer);
    int territoryProduction = 0;
    int territoryUnitProduction = 0;
    if (ta != null) {
      territoryProduction = ta.getProduction();
      territoryUnitProduction = ta.getUnitProduction();
    }
    int productionCapacity;
    if (accountForDamage) {
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        if (ua.getCanProduceXUnits() < 0) {
          // we could use territoryUnitProduction OR
          // territoryProduction if we wanted to, however we should
          // change damage to be based on whichever we choose.
          productionCapacity = territoryUnitProduction - taUnit.getUnitDamage();
        } else {
          productionCapacity = ua.getCanProduceXUnits() - taUnit.getUnitDamage();
        }
      } else {
        productionCapacity = territoryProduction;
        if (productionCapacity < 1) {
          productionCapacity = (Properties.getWW2V2(data) || Properties.getWW2V3(data)) ? 0 : 1;
        }
      }
    } else {
      if (ua.getCanProduceXUnits() < 0
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        productionCapacity = territoryProduction;
      } else if (ua.getCanProduceXUnits() < 0
          && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        productionCapacity = territoryUnitProduction;
      } else {
        productionCapacity = ua.getCanProduceXUnits();
      }
      if (productionCapacity < 1
          && !Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        productionCapacity = (Properties.getWW2V2(data) || Properties.getWW2V3(data)) ? 0 : 1;
      }
    }
    // Increase production if have industrial technology
    if (territoryProduction
        >= TechAbilityAttachment.getMinimumTerritoryValueForProductionBonus(player, data)) {
      productionCapacity += TechAbilityAttachment.getProductionBonus(u.getType(), player, data);
    }
    return mathMaxZero ? Math.max(0, productionCapacity) : productionCapacity;
  }

  /**
   * Currently made for translating unit damage from one unit to another unit. Will adjust damage to
   * be within max damage for the new units.
   *
   * @return change for unit's properties
   */
  public static Change translateAttributesToOtherUnits(
      final Unit unitGivingAttributes,
      final Collection<Unit> unitsThatWillGetAttributes,
      final Territory t) {
    final CompositeChange changes = new CompositeChange();
    // must look for hits, unitDamage,
    final TripleAUnit taUnit = (TripleAUnit) unitGivingAttributes;
    final int combatDamage = taUnit.getHits();
    final IntegerMap<Unit> hits = new IntegerMap<>();
    if (combatDamage > 0) {
      for (final Unit u : unitsThatWillGetAttributes) {
        final int maxHitPoints = UnitAttachment.get(u.getType()).getHitPoints();
        final int transferDamage = Math.min(combatDamage, maxHitPoints - 1);
        if (transferDamage <= 0) {
          continue;
        }
        hits.put(u, transferDamage);
      }
    }
    if (!hits.isEmpty()) {
      changes.add(ChangeFactory.unitsHit(hits, List.of(t)));
    }
    final int unitDamage = taUnit.getUnitDamage();
    final IntegerMap<Unit> damageMap = new IntegerMap<>();
    if (unitDamage > 0) {
      for (final Unit u : unitsThatWillGetAttributes) {
        final TripleAUnit taNew = (TripleAUnit) u;
        final int maxDamage = taNew.getHowMuchDamageCanThisUnitTakeTotal(u, t);
        final int transferDamage = Math.max(0, Math.min(unitDamage, maxDamage));
        if (transferDamage <= 0) {
          continue;
        }
        damageMap.put(u, transferDamage);
      }
    }
    if (!damageMap.isEmpty()) {
      changes.add(ChangeFactory.bombingUnitDamage(damageMap));
    }
    return changes;
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .build();
  }
}
