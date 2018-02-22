package games.strategy.triplea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ModifiableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

/**
 * Extended unit for triplea games.
 *
 * <p>
 * As with all game data components, changes made to this unit must be made through a Change instance. Calling setters
 * on this directly will
 * not serialize the changes across the network.
 * </p>
 */
public class TripleAUnit extends Unit {
  // compatable with 0.9.2
  private static final long serialVersionUID = 8811372406957115036L;
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
  // the transport that is currently transporting us
  private TripleAUnit m_transportedBy = null;
  // the units we have unloaded this turn
  private List<Unit> m_unloaded = Collections.emptyList();
  // was this unit loaded this turn?
  private boolean m_wasLoadedThisTurn = false;
  // the territory this unit was unloaded to this turn
  private Territory m_unloadedTo = null;
  // was this unit unloaded in combat phase this turn?
  private boolean m_wasUnloadedInCombatPhase = false;
  // movement used this turn
  private int m_alreadyMoved = 0;
  // movement used this turn
  private int m_bonusMovement = 0;
  // amount of damage unit has sustained
  private int m_unitDamage = 0;
  // is this submarine submerged
  private boolean m_submerged = false;
  // original owner of this unit
  private PlayerID m_originalOwner = null;
  // Was this unit in combat
  private boolean m_wasInCombat = false;
  private boolean m_wasLoadedAfterCombat = false;
  private boolean m_wasAmphibious = false;
  // the territory this unit started in (for use with scrambling)
  private Territory m_originatedFrom = null;
  private boolean m_wasScrambled = false;
  private int m_maxScrambleCount = -1;
  private boolean m_wasInAirBattle = false;
  private boolean m_disabled = false;
  // the number of airborne units launched by this unit this turn
  private int m_launched = 0;
  // was this unit airborne and launched this turn
  private boolean m_airborne = false;

  public static TripleAUnit get(final Unit u) {
    return (TripleAUnit) u;
  }

  public TripleAUnit(final UnitType type, final PlayerID owner, final GameData data) {
    super(type, owner, data);
  }

  public TripleAUnit getTransportedBy() {
    return m_transportedBy;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setTransportedBy(final TripleAUnit transportedBy) {
    m_transportedBy = transportedBy;
  }

  /**
   * This is a very slow method because it checks all territories on the map. Try not to use this method if possible.
   */
  public List<Unit> getTransporting() {
    // we don't store the units we are transporting
    // rather we look at the transported by property of units
    for (final Territory t : getData().getMap()) {
      // find the territory this transport is in
      if (t.getUnits().getUnits().contains(this)) {
        return t.getUnits().getMatches(o -> TripleAUnit.get(o).getTransportedBy() == TripleAUnit.this);
      }
    }
    return Collections.emptyList();
  }

  public List<Unit> getTransporting(final Collection<Unit> transportedUnitsPossible) {
    // we don't store the units we are transporting
    // rather we look at the transported by property of units
    return CollectionUtils.getMatches(transportedUnitsPossible,
        o -> TripleAUnit.get(o).getTransportedBy() == TripleAUnit.this);
  }

  public List<Unit> getUnloaded() {
    return m_unloaded;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setUnloaded(final List<Unit> unloaded) {
    if (unloaded == null || unloaded.isEmpty()) {
      m_unloaded = Collections.emptyList();
    } else {
      m_unloaded = new ArrayList<>(unloaded);
    }
  }

  public boolean getWasLoadedThisTurn() {
    return m_wasLoadedThisTurn;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setWasLoadedThisTurn(final boolean value) {
    m_wasLoadedThisTurn = value;
  }

  public Territory getUnloadedTo() {
    return m_unloadedTo;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setUnloadedTo(final Territory unloadedTo) {
    m_unloadedTo = unloadedTo;
  }

  public Territory getOriginatedFrom() {
    return m_originatedFrom;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setOriginatedFrom(final Territory t) {
    m_originatedFrom = t;
  }

  public boolean getWasUnloadedInCombatPhase() {
    return m_wasUnloadedInCombatPhase;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setWasUnloadedInCombatPhase(final boolean value) {
    m_wasUnloadedInCombatPhase = value;
  }

  public int getAlreadyMoved() {
    return m_alreadyMoved;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setAlreadyMoved(final int alreadyMoved) {
    m_alreadyMoved = alreadyMoved;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setBonusMovement(final int bonusMovement) {
    m_bonusMovement = bonusMovement;
  }

  public int getBonusMovement() {
    return m_bonusMovement;
  }

  /**
   * Does not account for any movement already made. Generally equal to UnitType movement
   */
  public int getMaxMovementAllowed() {
    return Math.max(0, m_bonusMovement + UnitAttachment.get(getType()).getMovement(getOwner()));
  }

  public int getMovementLeft() {
    return Math.max(0, UnitAttachment.get(getType()).getMovement(getOwner()) + m_bonusMovement - m_alreadyMoved);
  }

  public static Tuple<Integer, Integer> getMinAndMaxMovementLeft(final Collection<Unit> units) {
    int min = 100000;
    int max = 0;
    for (final Unit u : units) {
      final int left = ((TripleAUnit) u).getMovementLeft();
      if (left > max) {
        max = left;
      }
      if (left < min) {
        min = left;
      }
    }
    if (max < min) {
      min = max;
    }
    return Tuple.of(min, max);
  }

  public int getUnitDamage() {
    return m_unitDamage;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setUnitDamage(final int unitDamage) {
    m_unitDamage = unitDamage;
  }

  public boolean getSubmerged() {
    return m_submerged;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setSubmerged(final boolean submerged) {
    m_submerged = submerged;
  }

  public PlayerID getOriginalOwner() {
    return m_originalOwner;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setOriginalOwner(final PlayerID originalOwner) {
    m_originalOwner = originalOwner;
  }

  public boolean getWasInCombat() {
    return m_wasInCombat;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setWasInCombat(final boolean value) {
    m_wasInCombat = value;
  }

  public boolean getWasScrambled() {
    return m_wasScrambled;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setWasScrambled(final boolean value) {
    m_wasScrambled = value;
  }

  public int getMaxScrambleCount() {
    return m_maxScrambleCount;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setMaxScrambleCount(final int value) {
    m_maxScrambleCount = value;
  }

  public int getLaunched() {
    return m_launched;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setLaunched(final int value) {
    m_launched = value;
  }

  public boolean getAirborne() {
    return m_airborne;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setAirborne(final boolean value) {
    m_airborne = value;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setWasInAirBattle(final boolean value) {
    m_wasInAirBattle = value;
  }

  public boolean getWasInAirBattle() {
    return m_wasInAirBattle;
  }

  public boolean getWasLoadedAfterCombat() {
    return m_wasLoadedAfterCombat;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setWasLoadedAfterCombat(final boolean value) {
    m_wasLoadedAfterCombat = value;
  }

  public List<Unit> getDependents() {
    return getTransporting();
  }

  public boolean getWasAmphibious() {
    return m_wasAmphibious;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setWasAmphibious(final boolean value) {
    m_wasAmphibious = value;
  }

  public boolean getDisabled() {
    return m_disabled;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setDisabled(final boolean value) {
    m_disabled = value;
  }

  /**
   * How much more damage can this unit take?
   * Will return 0 if the unit cannot be damaged, or is at max damage.
   */
  public int getHowMuchMoreDamageCanThisUnitTake(final Unit u, final Territory t) {
    if (!Matches.unitCanBeDamaged().test(u)) {
      return 0;
    }
    final TripleAUnit taUnit = (TripleAUnit) u;
    return Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(u.getData())
        ? Math.max(0, getHowMuchDamageCanThisUnitTakeTotal(u, t) - taUnit.getUnitDamage())
        : Integer.MAX_VALUE;
  }

  /**
   * How much damage is the max this unit can take, accounting for territory, etc.
   * Will return -1 if the unit is of the type that cannot be damaged
   */
  public int getHowMuchDamageCanThisUnitTakeTotal(final Unit u, final Territory t) {
    if (!Matches.unitCanBeDamaged().test(u)) {
      return -1;
    }
    final UnitAttachment ua = UnitAttachment.get(u.getType());
    final int territoryUnitProduction = TerritoryAttachment.getUnitProduction(t);
    if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(u.getData())) {
      if (ua.getMaxDamage() <= 0) {
        // factories may or may not have max damage set, so we must still determine here
        // assume that if maxDamage <= 0, then the max damage must be based on the territory value
        // can use "production" or "unitProduction"
        return territoryUnitProduction * 2;
      }

      if (Matches.unitCanProduceUnits().test(u)) {
        // can use "production" or "unitProduction"
        return (ua.getCanProduceXUnits() < 0) ? territoryUnitProduction * ua.getMaxDamage() : ua.getMaxDamage();
      }

      return ua.getMaxDamage();
    }

    return Integer.MAX_VALUE;
  }

  public int getHowMuchCanThisUnitBeRepaired(final Unit u, final Territory t) {
    return Math.max(0,
        (this.getHowMuchDamageCanThisUnitTakeTotal(u, t) - this.getHowMuchMoreDamageCanThisUnitTake(u, t)));
  }

  public static int getProductionPotentialOfTerritory(final Collection<Unit> unitsAtStartOfStepInTerritory,
      final Territory producer, final PlayerID player, final GameData data, final boolean accountForDamage,
      final boolean mathMaxZero) {
    return getHowMuchCanUnitProduce(
        getBiggestProducer(unitsAtStartOfStepInTerritory, producer, player, data, accountForDamage), producer, player,
        data, accountForDamage, mathMaxZero);
  }

  public static Unit getBiggestProducer(final Collection<Unit> units, final Territory producer, final PlayerID player,
      final GameData data, final boolean accountForDamage) {
    final Predicate<Unit> factoryMatch = Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
        .and(Matches.unitIsBeingTransported().negate())
        .and(producer.isWater()
            ? Matches.unitIsLand().negate()
            : Matches.unitIsSea().negate());
    final Collection<Unit> factories = CollectionUtils.getMatches(units, factoryMatch);
    if (factories.isEmpty()) {
      return null;
    }
    final IntegerMap<Unit> productionPotential = new IntegerMap<>();
    Unit highestUnit = factories.iterator().next();
    int highestCapacity = Integer.MIN_VALUE;
    for (final Unit u : factories) {
      final int capacity = getHowMuchCanUnitProduce(u, producer, player, data, accountForDamage, false);
      productionPotential.put(u, capacity);
      if (capacity > highestCapacity) {
        highestCapacity = capacity;
        highestUnit = u;
      }
    }
    return highestUnit;
  }

  public static int getHowMuchCanUnitProduce(final Unit u, final Territory producer, final PlayerID player,
      final GameData data, final boolean accountForDamage, final boolean mathMaxZero) {
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
          productionCapacity =
              (Properties.getWW2V2(data) || Properties.getWW2V3(data)) ? 0
                  : 1;
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
        productionCapacity =
            (Properties.getWW2V2(data) || Properties.getWW2V3(data)) ? 0
                : 1;
      }
    }
    // Increase production if have industrial technology
    if (territoryProduction >= TechAbilityAttachment.getMinimumTerritoryValueForProductionBonus(player, data)) {
      productionCapacity += TechAbilityAttachment.getProductionBonus(u.getType(), player, data);
    }
    return mathMaxZero ? Math.max(0, productionCapacity) : productionCapacity;
  }

  /**
   * Currently made for translating unit damage from one unit to another unit. Will adjust damage to be within max
   * damage for the new units.
   *
   * @return change for unit's properties
   */
  public static Change translateAttributesToOtherUnits(final Unit unitGivingAttributes,
      final Collection<Unit> unitsThatWillGetAttributes, final Territory t) {
    final CompositeChange changes = new CompositeChange();
    // must look for m_hits, m_unitDamage,
    final TripleAUnit taUnit = (TripleAUnit) unitGivingAttributes;
    final int combatDamage = taUnit.getHits();
    final IntegerMap<Unit> hits = new IntegerMap<>();
    if (combatDamage > 0) {
      for (final Unit u : unitsThatWillGetAttributes) {
        hits.put(u, combatDamage);
      }
    }
    if (hits.size() > 0) {
      changes.add(ChangeFactory.unitsHit(hits));
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
    changes.add(ChangeFactory.bombingUnitDamage(damageMap));
    return changes;
  }

  @Override
  public Map<String, ModifiableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, ModifiableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("transportedBy",
            ModifiableProperty.ofSimple(
                this::setTransportedBy,
                this::getTransportedBy))
        .put("unloaded",
            ModifiableProperty.ofSimple(
                this::setUnloaded,
                this::getUnloaded))
        .put("wasLoadedThisTurn",
            ModifiableProperty.ofSimple(
                this::setWasLoadedThisTurn,
                this::getWasLoadedThisTurn))
        .put("unloadedTo",
            ModifiableProperty.ofSimple(
                this::setUnloadedTo,
                this::getUnloadedTo))
        .put("wasUnloadedInCombatPhase",
            ModifiableProperty.ofSimple(
                this::setWasUnloadedInCombatPhase,
                this::getWasUnloadedInCombatPhase))
        .put("alreadyMoved",
            ModifiableProperty.ofSimple(
                this::setAlreadyMoved,
                this::getAlreadyMoved))
        .put("bonusMovement",
            ModifiableProperty.ofSimple(
                this::setBonusMovement,
                this::getBonusMovement))
        .put("unitDamage",
            ModifiableProperty.ofSimple(
                this::setUnitDamage,
                this::getUnitDamage))
        .put("submerged",
            ModifiableProperty.ofSimple(
                this::setSubmerged,
                this::getSubmerged))
        .put("originalOwner",
            ModifiableProperty.ofSimple(
                this::setOriginalOwner,
                this::getOriginalOwner))
        .put("wasInCombat",
            ModifiableProperty.ofSimple(
                this::setWasInCombat,
                this::getWasInCombat))
        .put("wasLoadedAfterCombat",
            ModifiableProperty.ofSimple(
                this::setWasLoadedAfterCombat,
                this::getWasLoadedAfterCombat))
        .put("wasAmphibious",
            ModifiableProperty.ofSimple(
                this::setWasAmphibious,
                this::getWasAmphibious))
        .put("originatedFrom",
            ModifiableProperty.ofSimple(
                this::setOriginatedFrom,
                this::getOriginatedFrom))
        .put("wasScrambled",
            ModifiableProperty.ofSimple(
                this::setWasScrambled,
                this::getWasScrambled))
        .put("maxScrambleCount",
            ModifiableProperty.ofSimple(
                this::setMaxScrambleCount,
                this::getMaxScrambleCount))
        .put("wasInAirBattle",
            ModifiableProperty.ofSimple(
                this::setWasInAirBattle,
                this::getWasInAirBattle))
        .put("disabled",
            ModifiableProperty.ofSimple(
                this::setDisabled,
                this::getDisabled))
        .put("launched",
            ModifiableProperty.ofSimple(
                this::setLaunched,
                this::getLaunched))
        .put("airborne",
            ModifiableProperty.ofSimple(
                this::setAirborne,
                this::getAirborne))
        .build();
  }
}
