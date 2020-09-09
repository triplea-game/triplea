package games.strategy.triplea.attachments;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
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
import org.triplea.java.collections.IntegerMap;

/** An attachment for instances of {@link TerritoryEffect}. */
public class TerritoryEffectAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 6379810228136325991L;

  private IntegerMap<UnitType> combatDefenseEffect = new IntegerMap<>();
  private IntegerMap<UnitType> combatOffenseEffect = new IntegerMap<>();
  private Map<UnitType, BigDecimal> movementCostModifier = new HashMap<>();
  private List<UnitType> noBlitz = new ArrayList<>();
  private List<UnitType> unitsNotAllowed = new ArrayList<>();

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

  private void setCombatDefenseEffect(final IntegerMap<UnitType> value) {
    combatDefenseEffect = value;
  }

  private IntegerMap<UnitType> getCombatDefenseEffect() {
    return new IntegerMap<>(combatDefenseEffect);
  }

  private void resetCombatDefenseEffect() {
    combatDefenseEffect = new IntegerMap<>();
  }

  private void setCombatOffenseEffect(final String combatOffenseEffect) throws GameParseException {
    setCombatEffect(combatOffenseEffect, false);
  }

  private void setCombatOffenseEffect(final IntegerMap<UnitType> value) {
    combatOffenseEffect = value;
  }

  private IntegerMap<UnitType> getCombatOffenseEffect() {
    return new IntegerMap<>(combatOffenseEffect);
  }

  private void resetCombatOffenseEffect() {
    combatOffenseEffect = new IntegerMap<>();
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
      final String unitTypeToProduce = iter.next();
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
      if (ut == null) {
        throw new GameParseException("No unit called: " + unitTypeToProduce + thisErrorMsg());
      }
      if (defending) {
        combatDefenseEffect.put(ut, effect);
      } else {
        combatOffenseEffect.put(ut, effect);
      }
    }
  }

  public int getCombatEffect(final UnitType type, final boolean defending) {
    return defending ? combatDefenseEffect.getInt(type) : combatOffenseEffect.getInt(type);
  }

  private void setMovementCostModifier(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 2) {
      throw new GameParseException(
          "movementCostModifier must have a count and at least one unitType" + thisErrorMsg());
    }
    final Iterator<String> iter = List.of(s).iterator();
    final BigDecimal effect = getBigDecimal(iter.next());
    while (iter.hasNext()) {
      final String unitTypeToProduce = iter.next();
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
      if (ut == null) {
        throw new GameParseException("No unit called: " + unitTypeToProduce + thisErrorMsg());
      }
      movementCostModifier.put(ut, effect);
    }
  }

  private void setMovementCostModifier(final Map<UnitType, BigDecimal> value) {
    movementCostModifier = value;
  }

  public Map<UnitType, BigDecimal> getMovementCostModifier() {
    return new HashMap<>(movementCostModifier);
  }

  private void resetMovementCostModifier() {
    movementCostModifier = new HashMap<>();
  }

  private void setNoBlitz(final String noBlitzUnitTypes) throws GameParseException {
    final String[] s = splitOnColon(noBlitzUnitTypes);
    if (s.length < 1) {
      throw new GameParseException("noBlitz must have at least one unitType" + thisErrorMsg());
    }
    for (final String unitTypeName : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeName);
      if (ut == null) {
        throw new GameParseException("No unit called:" + unitTypeName + thisErrorMsg());
      }
      noBlitz.add(ut);
    }
  }

  private void setNoBlitz(final List<UnitType> value) {
    noBlitz = value;
  }

  public List<UnitType> getNoBlitz() {
    return new ArrayList<>(noBlitz);
  }

  private void resetNoBlitz() {
    noBlitz = new ArrayList<>();
  }

  private void setUnitsNotAllowed(final String unitsNotAllowedUnitTypes) throws GameParseException {
    final String[] s = splitOnColon(unitsNotAllowedUnitTypes);
    if (s.length < 1) {
      throw new GameParseException(
          "unitsNotAllowed must have at least one unitType" + thisErrorMsg());
    }
    for (final String unitTypeName : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeName);
      if (ut == null) {
        throw new GameParseException("No unit called:" + unitTypeName + thisErrorMsg());
      }
      unitsNotAllowed.add(ut);
    }
  }

  private void setUnitsNotAllowed(final List<UnitType> value) {
    unitsNotAllowed = value;
  }

  public List<UnitType> getUnitsNotAllowed() {
    return new ArrayList<>(unitsNotAllowed);
  }

  private void resetUnitsNotAllowed() {
    unitsNotAllowed = new ArrayList<>();
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put(
            "combatDefenseEffect",
            MutableProperty.of(
                this::setCombatDefenseEffect,
                this::setCombatDefenseEffect,
                this::getCombatDefenseEffect,
                this::resetCombatDefenseEffect))
        .put(
            "combatOffenseEffect",
            MutableProperty.of(
                this::setCombatOffenseEffect,
                this::setCombatOffenseEffect,
                this::getCombatOffenseEffect,
                this::resetCombatOffenseEffect))
        .put(
            "movementCostModifier",
            MutableProperty.of(
                this::setMovementCostModifier,
                this::setMovementCostModifier,
                this::getMovementCostModifier,
                this::resetMovementCostModifier))
        .put(
            "noBlitz",
            MutableProperty.of(
                this::setNoBlitz, this::setNoBlitz, this::getNoBlitz, this::resetNoBlitz))
        .put(
            "unitsNotAllowed",
            MutableProperty.of(
                this::setUnitsNotAllowed,
                this::setUnitsNotAllowed,
                this::getUnitsNotAllowed,
                this::resetUnitsNotAllowed))
        .build();
  }
}
