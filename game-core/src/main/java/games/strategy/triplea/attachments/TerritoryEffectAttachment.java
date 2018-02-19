package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.AttachmentProperty;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.util.IntegerMap;

@MapSupport
public class TerritoryEffectAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 6379810228136325991L;
  private static final Map<String, Function<IAttachment, AttachmentProperty<?>>> attachmentSetters =
      getPopulatedAttachmentMap();

  private IntegerMap<UnitType> m_combatDefenseEffect = new IntegerMap<>();
  private IntegerMap<UnitType> m_combatOffenseEffect = new IntegerMap<>();
  private List<UnitType> m_noBlitz = new ArrayList<>();
  private List<UnitType> m_unitsNotAllowed = new ArrayList<>();

  /**
   * Creates new TerritoryEffectAttachment.
   */
  public TerritoryEffectAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Convenience method.
   */
  public static TerritoryEffectAttachment get(final TerritoryEffect te) {
    return get(te, Constants.TERRITORYEFFECT_ATTACHMENT_NAME);
  }

  static TerritoryEffectAttachment get(final TerritoryEffect te, final String nameOfAttachment) {
    return getAttachment(te, nameOfAttachment, TerritoryEffectAttachment.class);
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCombatDefenseEffect(final String combatDefenseEffect) throws GameParseException {
    setCombatEffect(combatDefenseEffect, true);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCombatDefenseEffect(final IntegerMap<UnitType> value) {
    m_combatDefenseEffect = value;
  }

  public IntegerMap<UnitType> getCombatDefenseEffect() {
    return new IntegerMap<>(m_combatDefenseEffect);
  }

  public void clearCombatDefenseEffect() {
    m_combatDefenseEffect.clear();
  }

  public void resetCombatDefenseEffect() {
    m_combatDefenseEffect = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCombatOffenseEffect(final String combatOffenseEffect) throws GameParseException {
    setCombatEffect(combatOffenseEffect, false);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCombatOffenseEffect(final IntegerMap<UnitType> value) {
    m_combatOffenseEffect = value;
  }

  public IntegerMap<UnitType> getCombatOffenseEffect() {
    return new IntegerMap<>(m_combatOffenseEffect);
  }

  public void clearCombatOffenseEffect() {
    m_combatOffenseEffect.clear();
  }

  public void resetCombatOffenseEffect() {
    m_combatOffenseEffect = new IntegerMap<>();
  }

  @InternalDoNotExport
  private void setCombatEffect(final String combatEffect, final boolean defending) throws GameParseException {
    final String[] s = combatEffect.split(":");
    if (s.length < 2) {
      throw new GameParseException(
          "combatDefenseEffect and combatOffenseEffect must have a count and at least one unitType" + thisErrorMsg());
    }
    final Iterator<String> iter = Arrays.asList(s).iterator();
    final int effect = getInt(iter.next());
    while (iter.hasNext()) {
      final String unitTypeToProduce = iter.next();
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
      if (ut == null) {
        throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
      }
      if (defending) {
        m_combatDefenseEffect.put(ut, effect);
      } else {
        m_combatOffenseEffect.put(ut, effect);
      }
    }
  }

  public int getCombatEffect(final UnitType type, final boolean defending) {
    return defending ? m_combatDefenseEffect.getInt(type) : m_combatOffenseEffect.getInt(type);
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setNoBlitz(final String noBlitzUnitTypes) throws GameParseException {
    final String[] s = noBlitzUnitTypes.split(":");
    if (s.length < 1) {
      throw new GameParseException("noBlitz must have at least one unitType" + thisErrorMsg());
    }
    for (final String unitTypeName : Arrays.asList(s)) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeName);
      if (ut == null) {
        throw new GameParseException("No unit called:" + unitTypeName + thisErrorMsg());
      }
      m_noBlitz.add(ut);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setNoBlitz(final List<UnitType> value) {
    m_noBlitz = value;
  }

  public List<UnitType> getNoBlitz() {
    return new ArrayList<>(m_noBlitz);
  }

  public void clearNoBlitz() {
    m_noBlitz.clear();
  }

  public void resetNoBlitz() {
    m_noBlitz = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setUnitsNotAllowed(final String unitsNotAllowedUnitTypes) throws GameParseException {
    final String[] s = unitsNotAllowedUnitTypes.split(":");
    if (s.length < 1) {
      throw new GameParseException("unitsNotAllowed must have at least one unitType" + thisErrorMsg());
    }
    for (final String unitTypeName : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeName);
      if (ut == null) {
        throw new GameParseException("No unit called:" + unitTypeName + thisErrorMsg());
      }
      m_unitsNotAllowed.add(ut);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitsNotAllowed(final List<UnitType> value) {
    m_unitsNotAllowed = value;
  }

  public List<UnitType> getUnitsNotAllowed() {
    return new ArrayList<>(m_unitsNotAllowed);
  }

  public void clearUnitsNotAllowed() {
    m_unitsNotAllowed.clear();
  }

  public void resetUnitsNotAllowed() {
    m_unitsNotAllowed = new ArrayList<>();
  }

  @Override
  public void validate(final GameData data) {}


  private static Map<String, Function<IAttachment, AttachmentProperty<?>>> getPopulatedAttachmentMap() {
    return ImmutableMap.<String, Function<IAttachment, AttachmentProperty<?>>>builder()
        .put("combatDefenseEffect",
            ofCast(a -> AttachmentProperty.of(
                a::setCombatDefenseEffect,
                a::setCombatDefenseEffect,
                a::getCombatDefenseEffect,
                a::resetCombatDefenseEffect)))
        .put("combatOffenseEffect",
            ofCast(a -> AttachmentProperty.of(
                a::setCombatOffenseEffect,
                a::setCombatOffenseEffect,
                a::getCombatOffenseEffect,
                a::resetCombatOffenseEffect)))
        .put("noBlitz",
            ofCast(a -> AttachmentProperty.of(
                a::setNoBlitz,
                a::setNoBlitz,
                a::getNoBlitz,
                a::resetNoBlitz)))
        .put("unitsNotAllowed",
            ofCast(a -> AttachmentProperty.of(
                a::setUnitsNotAllowed,
                a::setUnitsNotAllowed,
                a::getUnitsNotAllowed,
                a::resetUnitsNotAllowed)))
        .build();
  }

  @Override
  public Map<String, Function<IAttachment, AttachmentProperty<?>>> getAttachmentMap() {
    return attachmentSetters;
  }

  private static Function<IAttachment, AttachmentProperty<?>> ofCast(
      final Function<TerritoryEffectAttachment, AttachmentProperty<?>> function) {
    return function.compose(TerritoryEffectAttachment.class::cast);
  }
}
