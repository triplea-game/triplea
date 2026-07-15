package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.collections.IntegerMap;

/**
 * An attachment for instances of {@link TerritoryEffect}. Note: Empty collection fields default to
 * null to minimize memory use and serialization size.
 */
public class TerritoryEffectAttachment extends DefaultAttachment {

  @NonNls public static final String COMBAT_OFFENSE_EFFECT = "combatOffenseEffect";
  @NonNls public static final String COMBAT_DEFENSE_EFFECT = "combatDefenseEffect";
  @NonNls public static final String MAX_GROUND_BATTLE_ROUNDS = "maxGroundBattleRounds";
  @NonNls public static final String MAX_AIR_BATTLE_ROUNDS = "maxAirBattleRounds";
  @NonNls public static final String STACK_CAPACITY = "stackCapacity";

  private static final long serialVersionUID = 6379810228136325991L;

  private @Nullable IntegerMap<UnitType> combatDefenseEffect = null;
  private @Nullable IntegerMap<UnitType> combatOffenseEffect = null;
  private @Nullable Map<UnitType, BigDecimal> movementCostModifier = null;
  private @Nullable List<UnitType> noBlitz = null;
  private @Nullable List<UnitType> unitsNotAllowed = null;
  private @Nullable Integer maxGroundBattleRounds = null;
  private @Nullable Integer maxAirBattleRounds = null;
  private @Nullable Integer stackCapacity = null;

  public TerritoryEffectAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static TerritoryEffectAttachment get(final TerritoryEffect te) {
    return get(te, Constants.TERRITORYEFFECT_ATTACHMENT_NAME);
  }

  static TerritoryEffectAttachment get(final TerritoryEffect te, final String nameOfAttachment) {
    return getAttachment(te, nameOfAttachment, TerritoryEffectAttachment.class);
  }

  private void setCombatDefenseEffect(final String combatDefenseEffect) throws GameParseException {
    setCombatEffect(combatDefenseEffect, true);
  }

  @VisibleForTesting
  public TerritoryEffectAttachment setCombatDefenseEffect(final IntegerMap<UnitType> value) {
    combatDefenseEffect = value;
    return this;
  }

  private IntegerMap<UnitType> getCombatDefenseEffect() {
    return getIntegerMapProperty(combatDefenseEffect);
  }

  private void resetCombatDefenseEffect() {
    combatDefenseEffect = null;
  }

  private void setCombatOffenseEffect(final String combatOffenseEffect) throws GameParseException {
    setCombatEffect(combatOffenseEffect, false);
  }

  @VisibleForTesting
  public TerritoryEffectAttachment setCombatOffenseEffect(final IntegerMap<UnitType> value) {
    combatOffenseEffect = value;
    return this;
  }

  private IntegerMap<UnitType> getCombatOffenseEffect() {
    return getIntegerMapProperty(combatOffenseEffect);
  }

  private void resetCombatOffenseEffect() {
    combatOffenseEffect = null;
  }

  private void setCombatEffect(final String combatEffect, final boolean defending)
      throws GameParseException {
    final String[] s = splitOnColon(combatEffect);
    if (s.length < 2) {
      throw new GameParseException(
          "combatDefenseEffect and combatOffenseEffect must have a count and at least one unitType"
              + thisErrorMsg());
    }
    final Iterator<String> iter = List.of(s).iterator();
    final int effect = getInt(iter.next());
    while (iter.hasNext()) {
      final UnitType ut = getUnitTypeOrThrow(iter.next());
      if (defending) {
        if (combatDefenseEffect == null) {
          combatDefenseEffect = new IntegerMap<>();
        }
        combatDefenseEffect.put(ut, effect);
      } else {
        if (combatOffenseEffect == null) {
          combatOffenseEffect = new IntegerMap<>();
        }
        combatOffenseEffect.put(ut, effect);
      }
    }
  }

  public int getCombatEffect(final UnitType type, final boolean defending) {
    return defending
        ? getCombatDefenseEffect().getInt(type)
        : getCombatOffenseEffect().getInt(type);
  }

  private void setMaxGroundBattleRounds(final String value) {
    setMaxGroundBattleRounds(getInt(value));
  }

  @VisibleForTesting
  public TerritoryEffectAttachment setMaxGroundBattleRounds(final Integer value) {
    maxGroundBattleRounds = validateBattleRounds(value, MAX_GROUND_BATTLE_ROUNDS);
    return this;
  }

  public OptionalInt getMaxGroundBattleRounds() {
    return maxGroundBattleRounds == null
        ? OptionalInt.empty()
        : OptionalInt.of(maxGroundBattleRounds);
  }

  private @Nullable Integer getMaxGroundBattleRoundsProperty() {
    return maxGroundBattleRounds;
  }

  private void resetMaxGroundBattleRounds() {
    maxGroundBattleRounds = null;
  }

  private void setMaxAirBattleRounds(final String value) {
    setMaxAirBattleRounds(getInt(value));
  }

  @VisibleForTesting
  public TerritoryEffectAttachment setMaxAirBattleRounds(final Integer value) {
    maxAirBattleRounds = validateBattleRounds(value, MAX_AIR_BATTLE_ROUNDS);
    return this;
  }

  public OptionalInt getMaxAirBattleRounds() {
    return maxAirBattleRounds == null ? OptionalInt.empty() : OptionalInt.of(maxAirBattleRounds);
  }

  private @Nullable Integer getMaxAirBattleRoundsProperty() {
    return maxAirBattleRounds;
  }

  private void resetMaxAirBattleRounds() {
    maxAirBattleRounds = null;
  }

  private void setStackCapacity(final String value) {
    setStackCapacity(getInt(value));
  }

  @VisibleForTesting
  public TerritoryEffectAttachment setStackCapacity(final Integer value) {
    final int capacity = value;
    if (capacity < -1) {
      throw new IllegalArgumentException(STACK_CAPACITY + " must be -1 or a non-negative integer");
    }
    stackCapacity = capacity;
    return this;
  }

  public OptionalInt getStackCapacity() {
    return stackCapacity == null ? OptionalInt.empty() : OptionalInt.of(stackCapacity);
  }

  private @Nullable Integer getStackCapacityProperty() {
    return stackCapacity;
  }

  private void resetStackCapacity() {
    stackCapacity = null;
  }

  private static int validateBattleRounds(final Integer value, final String propertyName) {
    final int rounds = Objects.requireNonNull(value);
    if (rounds == 0 || rounds < -1) {
      throw new IllegalArgumentException(propertyName + " must be -1 or a positive integer");
    }
    return rounds;
  }

  private void setMovementCostModifier(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 2) {
      throw new GameParseException(
          "movementCostModifier must have a count and at least one unitType" + thisErrorMsg());
    }
    final Iterator<String> iter = List.of(s).iterator();
    final BigDecimal effect;
    try {
      effect = new BigDecimal(iter.next());
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(
          "Attachments: " + s[0] + " is not a valid decimal value", e);
    }
    while (iter.hasNext()) {
      if (movementCostModifier == null) {
        movementCostModifier = new HashMap<>();
      }
      movementCostModifier.put(getUnitTypeOrThrow(iter.next()), effect);
    }
  }

  private void setMovementCostModifier(final Map<UnitType, BigDecimal> value) {
    movementCostModifier = value;
  }

  public Map<UnitType, BigDecimal> getMovementCostModifier() {
    return getMapProperty(movementCostModifier);
  }

  private void resetMovementCostModifier() {
    movementCostModifier = null;
  }

  private void setNoBlitz(final String noBlitzUnitTypes) throws GameParseException {
    final String[] s = splitOnColon(noBlitzUnitTypes);
    if (s.length < 1) {
      throw new GameParseException("noBlitz must have at least one unitType" + thisErrorMsg());
    }
    for (final String unitTypeName : s) {
      if (noBlitz == null) {
        noBlitz = new ArrayList<>();
      }
      noBlitz.add(getUnitTypeOrThrow(unitTypeName));
    }
  }

  private void setNoBlitz(final List<UnitType> value) {
    noBlitz = value;
  }

  public List<UnitType> getNoBlitz() {
    return getListProperty(noBlitz);
  }

  private void resetNoBlitz() {
    noBlitz = null;
  }

  private void setUnitsNotAllowed(final String unitsNotAllowedUnitTypes) throws GameParseException {
    final String[] s = splitOnColon(unitsNotAllowedUnitTypes);
    if (s.length < 1) {
      throw new GameParseException(
          "unitsNotAllowed must have at least one unitType" + thisErrorMsg());
    }
    for (final String unitTypeName : s) {
      if (unitsNotAllowed == null) {
        unitsNotAllowed = new ArrayList<>();
      }
      unitsNotAllowed.add(getUnitTypeOrThrow(unitTypeName));
    }
  }

  private void setUnitsNotAllowed(final List<UnitType> value) {
    unitsNotAllowed = value;
  }

  public List<UnitType> getUnitsNotAllowed() {
    return getListProperty(unitsNotAllowed);
  }

  private void resetUnitsNotAllowed() {
    unitsNotAllowed = null;
  }

  @Override
  public void validate(final GameState data) {}

  @Override
  public Optional<MutableProperty<?>> getPropertyOrEmpty(final @NonNls String propertyName) {
    return switch (propertyName) {
      case COMBAT_DEFENSE_EFFECT ->
          Optional.of(
              MutableProperty.of(
                  this::setCombatDefenseEffect,
                  this::setCombatDefenseEffect,
                  this::getCombatDefenseEffect,
                  this::resetCombatDefenseEffect));
      case COMBAT_OFFENSE_EFFECT ->
          Optional.of(
              MutableProperty.of(
                  this::setCombatOffenseEffect,
                  this::setCombatOffenseEffect,
                  this::getCombatOffenseEffect,
                  this::resetCombatOffenseEffect));
      case MAX_GROUND_BATTLE_ROUNDS ->
          Optional.of(
              MutableProperty.of(
                  this::setMaxGroundBattleRounds,
                  this::setMaxGroundBattleRounds,
                  this::getMaxGroundBattleRoundsProperty,
                  this::resetMaxGroundBattleRounds));
      case MAX_AIR_BATTLE_ROUNDS ->
          Optional.of(
              MutableProperty.of(
                  this::setMaxAirBattleRounds,
                  this::setMaxAirBattleRounds,
                  this::getMaxAirBattleRoundsProperty,
                  this::resetMaxAirBattleRounds));
      case STACK_CAPACITY ->
          Optional.of(
              MutableProperty.of(
                  this::setStackCapacity,
                  this::setStackCapacity,
                  this::getStackCapacityProperty,
                  this::resetStackCapacity));
      case "movementCostModifier" ->
          Optional.of(
              MutableProperty.of(
                  this::setMovementCostModifier,
                  this::setMovementCostModifier,
                  this::getMovementCostModifier,
                  this::resetMovementCostModifier));
      case "noBlitz" ->
          Optional.of(
              MutableProperty.of(
                  this::setNoBlitz, this::setNoBlitz, this::getNoBlitz, this::resetNoBlitz));
      case "unitsNotAllowed" ->
          Optional.of(
              MutableProperty.of(
                  this::setUnitsNotAllowed,
                  this::setUnitsNotAllowed,
                  this::getUnitsNotAllowed,
                  this::resetUnitsNotAllowed));
      default -> Optional.empty();
    };
  }
}
