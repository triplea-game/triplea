package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.collections.CollectionUtils;

/**
 * Extended unit for triplea games.
 *
 * <p>As with all game data components, changes made to this unit must be made through a Change
 * instance. Calling setters on this directly will not serialize the changes across the network.
 */
@Slf4j
@Getter
@EqualsAndHashCode(of = "id", callSuper = false)
public class Unit extends GameDataComponent implements DynamicallyModifiable {
  public static final class PropertyName {
    @NonNls public static final String TRANSPORTED_BY = "transportedBy";
    @NonNls public static final String UNLOADED = "unloaded";
    @NonNls public static final String LOADED_THIS_TURN = "wasLoadedThisTurn";
    @NonNls public static final String UNLOADED_TO = "unloadedTo";
    @NonNls public static final String UNLOADED_IN_COMBAT_PHASE = "wasUnloadedInCombatPhase";
    @NonNls public static final String ALREADY_MOVED = "alreadyMoved";
    @NonNls public static final String BONUS_MOVEMENT = "bonusMovement";
    @NonNls public static final String SUBMERGED = "submerged";
    @NonNls public static final String WAS_IN_COMBAT = "wasInCombat";
    @NonNls public static final String LOADED_AFTER_COMBAT = "wasLoadedAfterCombat";
    @NonNls public static final String UNLOADED_AMPHIBIOUS = "wasAmphibious";
    @NonNls public static final String ORIGINATED_FROM = "originatedFrom";
    @NonNls public static final String WAS_SCRAMBLED = "wasScrambled";
    @NonNls public static final String MAX_SCRAMBLE_COUNT = "maxScrambleCount";
    @NonNls public static final String WAS_IN_AIR_BATTLE = "wasInAirBattle";
    @NonNls public static final String LAUNCHED = "launched";
    @NonNls public static final String AIRBORNE = "airborne";
    @NonNls public static final String CHARGED_FLAT_FUEL_COST = "chargedFlatFuelCost";

    private PropertyName() {
      throw new IllegalStateException("Utility class constructor should not be called");
    }
  }

  private static final long serialVersionUID = -79061939642779999L;

  private GamePlayer owner;
  private final UUID id;
  @Setter private int hits = 0;
  private final UnitType type;

  // the transport that is currently transporting us
  private Unit transportedBy = null;
  // the units we have unloaded this turn
  @Getter private List<Unit> unloaded = List.of();
  // was this unit loaded this turn?
  private boolean wasLoadedThisTurn = false;
  // the territory this unit was unloaded to this turn
  @Getter private Territory unloadedTo = null;
  // was this unit unloaded in combat phase this turn?
  private boolean wasUnloadedInCombatPhase = false;
  // movement used this turn
  @Getter private BigDecimal alreadyMoved = BigDecimal.ZERO;
  // movement used this turn
  @Getter private int bonusMovement = 0;
  // amount of damage unit has sustained
  @Getter private int unitDamage = 0;
  // is this submarine submerged
  private boolean submerged = false;
  // original owner of this unit
  @Getter private GamePlayer originalOwner = null;
  // Was this unit in combat
  private boolean wasInCombat = false;
  private boolean wasLoadedAfterCombat = false;
  private boolean wasAmphibious = false;
  // the territory this unit started in (for use with scrambling)
  @Getter private Territory originatedFrom = null;
  private boolean wasScrambled = false;
  @Getter private int maxScrambleCount = -1;
  private boolean wasInAirBattle = false;
  private boolean disabled = false;
  // the number of airborne units launched by this unit this turn
  @Getter private int launched = 0;
  // was this unit airborne and launched this turn
  private boolean airborne = false;
  // was charged flat fuel cost already this turn
  private boolean chargedFlatFuelCost = false;

  /** Creates new Unit. Owner can be null. */
  public Unit(final UnitType type, @Nullable final GamePlayer owner, final GameData data) {
    super(data);
    this.type = checkNotNull(type);
    this.id = UUID.randomUUID();

    setOwner(owner);
  }

  public Unit(final UUID uuid, final UnitType type, final GamePlayer owner, final GameData data) {
    super(data);
    this.id = uuid;
    this.type = checkNotNull(type);
    setOwner(owner);
  }

  public UnitAttachment getUnitAttachment() {
    return type.getUnitAttachment();
  }

  public void setOwner(final @Nullable GamePlayer player) {
    owner = Optional.ofNullable(player).orElse(getData().getPlayerList().getNullPlayer());
  }

  public final boolean isOwnedBy(final GamePlayer player) {
    // Use getOwner() to allow test mocks to override that method.
    return getOwner().equals(player);
  }

  public boolean isEquivalent(final Unit unit) {
    return type != null
        && type.equals(unit.getType())
        && owner != null
        && owner.equals(unit.getOwner())
        && hits == unit.getHits();
  }

  public int getHowMuchCanThisUnitBeRepaired(final Territory t) {
    return Math.max(
        0, (getHowMuchDamageCanThisUnitTakeTotal(t) - getHowMuchMoreDamageCanThisUnitTake(t)));
  }

  /**
   * How much more damage can this unit take? Will return 0 if the unit cannot be damaged, or is at
   * max damage.
   */
  public int getHowMuchMoreDamageCanThisUnitTake(final Territory t) {
    if (!Matches.unitCanBeDamaged().test(this)) {
      return 0;
    }
    return Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData().getProperties())
        ? Math.max(0, getHowMuchDamageCanThisUnitTakeTotal(t) - getUnitDamage())
        : Integer.MAX_VALUE;
  }

  /**
   * How much damage is the max this unit can take, accounting for territory, etc. Will return -1 if
   * the unit is of the type that cannot be damaged
   */
  public int getHowMuchDamageCanThisUnitTakeTotal(final Territory t) {
    if (!Matches.unitCanBeDamaged().test(this)) {
      return -1;
    }
    final UnitAttachment ua = getType().getUnitAttachment();
    final int territoryUnitProduction = TerritoryAttachment.getUnitProduction(t);
    if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData().getProperties())) {
      if (ua.getMaxDamage() <= 0) {
        // factories may or may not have max damage set, so we must still determine here
        // assume that if maxDamage <= 0, then the max damage must be based on the territory value
        // can use "production" or "unitProduction"
        return territoryUnitProduction * 2;
      }

      if (Matches.unitCanProduceUnits().test(this)) {
        // can use "production" or "unitProduction"
        return (ua.getCanProduceXUnits() < 0)
            ? territoryUnitProduction * ua.getMaxDamage()
            : ua.getMaxDamage();
      }

      return ua.getMaxDamage();
    }

    return Integer.MAX_VALUE;
  }

  @Override
  public String toString() {
    // TODO: none of these should happen,... except that they did a couple times.
    if (type == null || owner == null || id == null || this.getData() == null) {
      final String text =
          "Unit.toString() -> Possible java de-serialization error: "
              + (type == null ? "Unit of UNKNOWN TYPE" : type.getName())
              + " owned by "
              + (owner == null ? "UNKNOWN OWNER" : owner.getName())
              + " with id: "
              + getId();
      UnitDeserializationErrorLazyMessage.printError(text);
      return text;
    }
    return type.getName() + " owned by " + owner.getName();
  }

  public String toStringNoOwner() {
    return type.getName();
  }

  /**
   * Until this error gets fixed, lets not scare the crap out of our users, as the problem doesn't
   * seem to be causing any serious issues. TODO: fix the root cause of this deserialization issue
   * (probably a circular dependency somewhere)
   */
  public static final class UnitDeserializationErrorLazyMessage {
    private static boolean shownError = false;

    private UnitDeserializationErrorLazyMessage() {}

    // false positive
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static void printError(final String errorMessage) {
      if (!shownError) {
        shownError = true;
        log.error(errorMessage);
      }
    }
  }

  @Override
  public Optional<MutableProperty<?>> getPropertyOrEmpty(@NonNls String propertyName) {
    return switch (propertyName) {
      case "owner" -> Optional.of(MutableProperty.ofSimple(this::setOwner, this::getOwner));
      case "uid" -> Optional.of(MutableProperty.ofReadOnlySimple(this::getId));
      case "hits" -> Optional.of(MutableProperty.ofSimple(this::setHits, this::getHits));
      case "type" -> Optional.of(MutableProperty.ofReadOnlySimple(this::getType));
      case PropertyName.TRANSPORTED_BY ->
          Optional.of(MutableProperty.ofSimple(this::setTransportedBy, this::getTransportedBy));
      case PropertyName.UNLOADED ->
          Optional.of(MutableProperty.ofSimple(this::setUnloaded, this::getUnloaded));
      case PropertyName.LOADED_THIS_TURN ->
          Optional.of(
              MutableProperty.ofSimple(this::setWasLoadedThisTurn, this::getWasLoadedThisTurn));
      case PropertyName.UNLOADED_TO ->
          Optional.of(MutableProperty.ofSimple(this::setUnloadedTo, this::getUnloadedTo));
      case PropertyName.UNLOADED_IN_COMBAT_PHASE ->
          Optional.of(
              MutableProperty.ofSimple(
                  this::setWasUnloadedInCombatPhase, this::getWasUnloadedInCombatPhase));
      case PropertyName.ALREADY_MOVED ->
          Optional.of(MutableProperty.ofSimple(this::setAlreadyMoved, this::getAlreadyMoved));
      case PropertyName.BONUS_MOVEMENT ->
          Optional.of(MutableProperty.ofSimple(this::setBonusMovement, this::getBonusMovement));
      case "unitDamage" ->
          Optional.of(MutableProperty.ofSimple(this::setUnitDamage, this::getUnitDamage));
      case PropertyName.SUBMERGED ->
          Optional.of(MutableProperty.ofSimple(this::setSubmerged, this::getSubmerged));
      case Constants.ORIGINAL_OWNER ->
          Optional.of(MutableProperty.ofSimple(this::setOriginalOwner, this::getOriginalOwner));
      case PropertyName.WAS_IN_COMBAT ->
          Optional.of(MutableProperty.ofSimple(this::setWasInCombat, this::getWasInCombat));
      case PropertyName.LOADED_AFTER_COMBAT ->
          Optional.of(
              MutableProperty.ofSimple(
                  this::setWasLoadedAfterCombat, this::getWasLoadedAfterCombat));
      case PropertyName.UNLOADED_AMPHIBIOUS ->
          Optional.of(MutableProperty.ofSimple(this::setWasAmphibious, this::getWasAmphibious));
      case PropertyName.ORIGINATED_FROM ->
          Optional.of(MutableProperty.ofSimple(this::setOriginatedFrom, this::getOriginatedFrom));
      case PropertyName.WAS_SCRAMBLED ->
          Optional.of(MutableProperty.ofSimple(this::setWasScrambled, this::getWasScrambled));
      case PropertyName.MAX_SCRAMBLE_COUNT ->
          Optional.of(
              MutableProperty.ofSimple(this::setMaxScrambleCount, this::getMaxScrambleCount));
      case PropertyName.WAS_IN_AIR_BATTLE ->
          Optional.of(MutableProperty.ofSimple(this::setWasInAirBattle, this::getWasInAirBattle));
      case "disabled" ->
          Optional.of(MutableProperty.ofSimple(this::setDisabled, this::getDisabled));
      case PropertyName.LAUNCHED ->
          Optional.of(MutableProperty.ofSimple(this::setLaunched, this::getLaunched));
      case PropertyName.AIRBORNE ->
          Optional.of(MutableProperty.ofSimple(this::setAirborne, this::getAirborne));
      case PropertyName.CHARGED_FLAT_FUEL_COST ->
          Optional.of(
              MutableProperty.ofSimple(this::setChargedFlatFuelCost, this::getChargedFlatFuelCost));
      default -> Optional.empty();
    };
  }

  public void setUnitDamage(final int unitDamage) {
    this.unitDamage = unitDamage;
  }

  public boolean getSubmerged() {
    return submerged;
  }

  public void setSubmerged(final boolean submerged) {
    this.submerged = submerged;
  }

  private void setOriginalOwner(final GamePlayer originalOwner) {
    this.originalOwner = originalOwner;
  }

  public boolean getWasInCombat() {
    return wasInCombat;
  }

  private void setWasInCombat(final boolean value) {
    wasInCombat = value;
  }

  public boolean getWasScrambled() {
    return wasScrambled;
  }

  private void setWasScrambled(final boolean value) {
    wasScrambled = value;
  }

  private void setMaxScrambleCount(final int value) {
    maxScrambleCount = value;
  }

  private void setLaunched(final int value) {
    launched = value;
  }

  public boolean getAirborne() {
    return airborne;
  }

  private void setAirborne(final boolean value) {
    airborne = value;
  }

  public boolean getChargedFlatFuelCost() {
    return chargedFlatFuelCost;
  }

  private void setChargedFlatFuelCost(final boolean value) {
    chargedFlatFuelCost = value;
  }

  private void setWasInAirBattle(final boolean value) {
    wasInAirBattle = value;
  }

  public boolean getWasInAirBattle() {
    return wasInAirBattle;
  }

  public boolean getWasLoadedAfterCombat() {
    return wasLoadedAfterCombat;
  }

  private void setWasLoadedAfterCombat(final boolean value) {
    wasLoadedAfterCombat = value;
  }

  public boolean getWasAmphibious() {
    return wasAmphibious;
  }

  @VisibleForTesting
  public Unit setWasAmphibious(final boolean value) {
    wasAmphibious = value;
    return this;
  }

  public boolean getDisabled() {
    return disabled;
  }

  private void setDisabled(final boolean value) {
    disabled = value;
  }

  @VisibleForTesting
  public void setTransportedBy(final Unit transportedBy) {
    this.transportedBy = transportedBy;
  }

  /**
   * Try not to use this method if possible.
   *
   * @return Unmodifiable collection of units that this unit is transporting in the same territory
   *     it is located in
   * @deprecated This is a very slow method because it checks all territories on the map.
   */
  @Deprecated
  public List<Unit> getTransporting() {
    if (Matches.unitCanTransport().test(this) || Matches.unitIsCarrier().test(this)) {
      // we don't store the units we are transporting
      // rather we look at the transported by property of units
      for (final Territory t : getData().getMap()) {
        // find the territory this transport is in
        if (t.getUnitCollection().contains(this)) {
          return getTransporting(t);
        }
      }
    }
    return List.of();
  }

  /**
   * @return Unmodifiable collection of units in the territory that this unit is transporting
   */
  public List<Unit> getTransporting(final Territory territory) {
    return getTransporting(territory.getUnitCollection());
  }

  /**
   * @return Unmodifiable collection of a subset of the units that this unit is transporting
   */
  public List<Unit> getTransporting(final Collection<Unit> transportedUnitsPossible) {
    // we don't store the units we are transporting
    // rather we look at the transported by property of units
    return Collections.unmodifiableList(
        CollectionUtils.getMatches(transportedUnitsPossible, o -> equals(o.getTransportedBy())));
  }

  @VisibleForTesting
  public void setUnloaded(final List<Unit> unloaded) {
    if (unloaded == null || unloaded.isEmpty()) {
      this.unloaded = List.of();
    } else {
      this.unloaded = ImmutableList.copyOf(unloaded);
    }
  }

  public boolean getWasLoadedThisTurn() {
    return wasLoadedThisTurn;
  }

  private void setWasLoadedThisTurn(final boolean value) {
    wasLoadedThisTurn = value;
  }

  private void setUnloadedTo(final Territory unloadedTo) {
    this.unloadedTo = unloadedTo;
  }

  private void setOriginatedFrom(final Territory t) {
    originatedFrom = t;
  }

  public boolean getWasUnloadedInCombatPhase() {
    return wasUnloadedInCombatPhase;
  }

  private void setWasUnloadedInCombatPhase(final boolean value) {
    wasUnloadedInCombatPhase = value;
  }

  public void setAlreadyMoved(final BigDecimal alreadyMoved) {
    this.alreadyMoved = alreadyMoved;
  }

  private void setBonusMovement(final int bonusMovement) {
    this.bonusMovement = bonusMovement;
  }

  /** Does not account for any movement already made. Generally equal to UnitType movement */
  public int getMaxMovementAllowed() {
    return Math.max(0, bonusMovement + getType().getUnitAttachment().getMovement(getOwner()));
  }

  public BigDecimal getMovementLeft() {
    return new BigDecimal(getType().getUnitAttachment().getMovement(getOwner()))
        .add(new BigDecimal(bonusMovement))
        .subtract(alreadyMoved);
  }

  public boolean hasMovementLeft() {
    return getMovementLeft().compareTo(BigDecimal.ZERO) > 0;
  }

  public boolean isDamaged() {
    return unitDamage > 0 && hits > 0;
  }

  public boolean hasMoved() {
    return alreadyMoved.compareTo(BigDecimal.ZERO) > 0;
  }

  public int hitsUnitCanTakeHitWithoutBeingKilled() {
    return getUnitAttachment().getHitPoints() - 1 - hits;
  }

  public boolean canTakeHitWithoutBeingKilled() {
    return hitsUnitCanTakeHitWithoutBeingKilled() > 0;
  }

  /**
   * Avoid calling this method, it checks every territory on the map. To avoid deprecation we should
   * optimize this to halt on the first territory we have found with a transporting unit, or
   * otherwise optimize this to not check every territory.
   *
   * @deprecated Avoid calling this method, it calls {@link #getTransporting()} which is slow and
   *     needs optimization.
   */
  @Deprecated
  public boolean isTransporting() {
    return !getTransporting().isEmpty();
  }

  public boolean isTransporting(final Territory territory) {
    return !getTransporting(territory).isEmpty();
  }
}
