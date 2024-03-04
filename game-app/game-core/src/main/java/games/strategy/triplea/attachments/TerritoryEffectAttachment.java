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
import javax.annotation.Nullable;
import org.triplea.java.collections.IntegerMap;

/**
 * An attachment for instances of {@link TerritoryEffect}. Note: Empty collection fields default to
 * null to minimize memory use and serialization size.
 */
public class TerritoryEffectAttachment extends DefaultAttachment {

  public static final String COMBAT_OFFENSE_EFFECT = "combatOffenseEffect";
  public static final String COMBAT_DEFENSE_EFFECT = "combatDefenseEffect";

  private static final long serialVersionUID = 6379810228136325991L;

  private @Nullable IntegerMap<UnitType> combatDefenseEffect = null;
  private @Nullable IntegerMap<UnitType> combatOffenseEffect = null;
  private @Nullable Map<UnitType, BigDecimal> movementCostModifier = null;
  private @Nullable List<UnitType> noBlitz = null;
  private @Nullable List<UnitType> unitsNotAllowed = null;

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
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case COMBAT_DEFENSE_EFFECT:
        return MutableProperty.of(
            this::setCombatDefenseEffect,
            this::setCombatDefenseEffect,
            this::getCombatDefenseEffect,
            this::resetCombatDefenseEffect);
      case COMBAT_OFFENSE_EFFECT:
        return MutableProperty.of(
            this::setCombatOffenseEffect,
            this::setCombatOffenseEffect,
            this::getCombatOffenseEffect,
            this::resetCombatOffenseEffect);
      case "movementCostModifier":
        return MutableProperty.of(
            this::setMovementCostModifier,
            this::setMovementCostModifier,
            this::getMovementCostModifier,
            this::resetMovementCostModifier);
      case "noBlitz":
        return MutableProperty.of(
            this::setNoBlitz, this::setNoBlitz, this::getNoBlitz, this::resetNoBlitz);
      case "unitsNotAllowed":
        return MutableProperty.of(
            this::setUnitsNotAllowed,
            this::setUnitsNotAllowed,
            this::getUnitsNotAllowed,
            this::resetUnitsNotAllowed);
      default:
        return null;
    }
  }
}
