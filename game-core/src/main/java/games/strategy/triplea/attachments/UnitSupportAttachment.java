package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;

@MapSupport
public class UnitSupportAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -3015679930172496082L;

  private Set<UnitType> m_unitType = null;
  @InternalDoNotExport
  // Do Not Export
  private boolean m_offence = false;
  @InternalDoNotExport
  // Do Not Export
  private boolean m_defence = false;
  @InternalDoNotExport
  private boolean m_roll = false;
  @InternalDoNotExport
  private boolean m_strength = false;
  private int m_bonus = 0;
  private int m_number = 0;
  @InternalDoNotExport
  private boolean m_allied = false;
  @InternalDoNotExport
  private boolean m_enemy = false;
  private String m_bonusType = null;
  private List<PlayerID> m_players = new ArrayList<>();
  private boolean m_impArtTech = false;
  // strings
  // roll or strength
  private String m_dice;
  // offence or defence
  private String m_side;
  private String m_faction;

  public UnitSupportAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Set<UnitSupportAttachment> get(final UnitType u) {
    return u.getAttachments().values().stream()
        .filter(attachment -> attachment.getName().startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX))
        .map(UnitSupportAttachment.class::cast)
        .collect(Collectors.toSet());
  }

  static UnitSupportAttachment get(final UnitType u, final String nameOfAttachment) {
    return getAttachment(u, nameOfAttachment, UnitSupportAttachment.class);
  }

  public static Set<UnitSupportAttachment> get(final GameData data) {
    data.acquireReadLock();
    try {
      return StreamSupport.stream(data.getUnitTypeList().spliterator(), false)
          .map(UnitSupportAttachment::get)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet());
    } finally {
      data.releaseReadLock();
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitType(final String names) throws GameParseException {
    if (names == null) {
      m_unitType = null;
      return;
    }
    m_unitType = new HashSet<>();
    final String[] s = names.split(":");
    for (final String element : s) {
      final UnitType type = getData().getUnitTypeList().getUnitType(element);
      if (type == null) {
        throw new GameParseException("Could not find unitType. name:" + element + thisErrorMsg());
      }
      m_unitType.add(type);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitType(final Set<UnitType> value) {
    m_unitType = value;
  }

  public void resetUnitType() {
    m_unitType = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setFaction(final String faction) throws GameParseException {
    m_faction = faction;
    if (faction == null) {
      resetFaction();
      return;
    }
    m_allied = false;
    m_enemy = false;
    final String[] s = faction.split(":");
    for (final String element : s) {
      if (element.equalsIgnoreCase("allied")) {
        m_allied = true;
      } else if (element.equalsIgnoreCase("enemy")) {
        m_enemy = true;
      } else {
        throw new GameParseException(faction + " faction must be allied, or enemy" + thisErrorMsg());
      }
    }
  }

  public String getFaction() {
    return m_faction;
  }

  public void resetFaction() {
    m_allied = false;
    m_enemy = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setSide(final String side) throws GameParseException {
    if (side == null) {
      resetSide();
      return;
    }
    m_defence = false;
    m_offence = false;
    final String[] s = side.split(":");
    for (final String element : s) {
      if (element.equalsIgnoreCase("defence")) {
        m_defence = true;
      } else if (element.equalsIgnoreCase("offence")) {
        m_offence = true;
      } else {
        throw new GameParseException(side + " side must be defence or offence" + thisErrorMsg());
      }
    }
    m_side = side;
  }

  public String getSide() {
    return m_side;
  }

  public void resetSide() {
    m_side = null;
    m_offence = false;
    m_defence = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDice(final String dice) throws GameParseException {
    if (dice == null) {
      resetDice();
      return;
    }
    m_roll = false;
    m_strength = false;
    final String[] s = dice.split(":");
    for (final String element : s) {
      if (element.equalsIgnoreCase("roll")) {
        m_roll = true;
      } else if (element.equalsIgnoreCase("strength")) {
        m_strength = true;
      } else {
        throw new GameParseException(dice + " dice must be roll or strength" + thisErrorMsg());
      }
    }
    m_dice = dice;
  }

  public String getDice() {
    return m_dice;
  }

  public void resetDice() {
    m_dice = null;
    m_roll = false;
    m_strength = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBonus(final String bonus) {
    m_bonus = getInt(bonus);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBonus(final Integer bonus) {
    m_bonus = bonus;
  }

  public void resetBonus() {
    m_bonus = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setNumber(final String number) {
    m_number = getInt(number);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setNumber(final Integer number) {
    m_number = number;
  }

  public void resetNumber() {
    m_number = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBonusType(final String type) {
    if (type == null) {
      m_bonusType = null;
      return;
    }
    m_bonusType = type;
  }

  public void resetBonusType() {
    m_bonusType = null;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setPlayers(final String names) throws GameParseException {
    final String[] s = names.split(":");
    for (final String element : s) {
      final PlayerID player = getData().getPlayerList().getPlayerId(element);
      if (player == null) {
        throw new GameParseException("Could not find player. name:" + element + thisErrorMsg());
      }
      m_players.add(player);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setPlayers(final List<PlayerID> value) {
    m_players = value;
  }

  public List<PlayerID> getPlayers() {
    return m_players;
  }

  public void clearPlayers() {
    m_players.clear();
  }

  public void resetPlayers() {
    m_players = new ArrayList<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setImpArtTech(final String tech) {
    m_impArtTech = getBool(tech);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setImpArtTech(final Boolean tech) {
    m_impArtTech = tech;
  }

  public void resetImpArtTech() {
    m_impArtTech = false;
  }

  public Set<UnitType> getUnitType() {
    return m_unitType;
  }

  public int getNumber() {
    return m_number;
  }

  public int getBonus() {
    return m_bonus;
  }

  public boolean getAllied() {
    return m_allied;
  }

  public boolean getEnemy() {
    return m_enemy;
  }

  public boolean getRoll() {
    return m_roll;
  }

  public boolean getStrength() {
    return m_strength;
  }

  public boolean getDefence() {
    return m_defence;
  }

  public boolean getOffence() {
    return m_offence;
  }

  public String getBonusType() {
    return m_bonusType;
  }

  public boolean getImpArtTech() {
    return m_impArtTech;
  }

  /*
   * following are all to support old artillery flags.
   * boolean first is a cheat, adds a bogus support to a unit
   * in the case that supportable units are declared before any artillery
   */
  @InternalDoNotExport
  static void addRule(final UnitType type, final GameData data, final boolean first) throws GameParseException {
    final String attachmentName =
        (first ? Constants.SUPPORT_RULE_NAME_OLD_TEMP_FIRST : Constants.SUPPORT_RULE_NAME_OLD) + type.getName();
    final UnitSupportAttachment rule = new UnitSupportAttachment(attachmentName, type, data);
    rule.setBonus("1");
    rule.setBonusType(Constants.OLD_ART_RULE_NAME);
    rule.setDice("strength");
    rule.setFaction("allied");
    rule.setImpArtTech("true");
    if (first) {
      rule.setNumber("0");
    } else {
      rule.setNumber("1");
    }
    rule.setSide("offence");
    if (first) {
      rule.addUnitTypes(Collections.singleton(type));
    } else {
      rule.addUnitTypes(getTargets(data));
    }
    if (!first) {
      rule.setPlayers(new ArrayList<>(data.getPlayerList().getPlayers()));
    }
    type.addAttachment(attachmentName, rule);
  }

  @InternalDoNotExport
  private static Set<UnitType> getTargets(final GameData data) {
    Set<UnitType> types = null;
    for (final UnitSupportAttachment rule : get(data)) {
      if (rule.getBonusType().equals(Constants.OLD_ART_RULE_NAME)) {
        types = rule.getUnitType();
        if (rule.getName().startsWith(Constants.SUPPORT_RULE_NAME_OLD_TEMP_FIRST)) {
          // remove it because it is a "first", which is just a temporary one made to hold target info. what a hack.
          final UnitType attachedTo = (UnitType) rule.getAttachedTo();
          attachedTo.removeAttachment(rule.getName());
          rule.setAttachedTo(null);
        }
      }
    }
    return types;
  }

  @InternalDoNotExport
  private void addUnitTypes(final Set<UnitType> types) {
    if (types == null) {
      return;
    }
    if (m_unitType == null) {
      m_unitType = new HashSet<>();
    }
    m_unitType.addAll(types);
  }

  @InternalDoNotExport
  static void setOldSupportCount(final UnitType type, final GameData data, final String count) {
    for (final UnitSupportAttachment rule : get(data)) {
      if (rule.getBonusType().equals(Constants.OLD_ART_RULE_NAME) && rule.getAttachedTo() == type) {
        rule.setNumber(count);
      }
    }
  }

  @InternalDoNotExport
  static void addTarget(final UnitType type, final GameData data) throws GameParseException {
    boolean first = true;
    for (final UnitSupportAttachment rule : get(data)) {
      if (rule.getBonusType().equals(Constants.OLD_ART_RULE_NAME)) {
        rule.addUnitTypes(Collections.singleton(type));
        first = false;
      }
    }
    // if first, it means we do not have any support attachments created yet. so create a temporary one on this unit
    // just to hold the target
    // info.
    if (first) {
      addRule(type, data, first);
    }
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("unitType",
            MutableProperty.of(
                Set.class,
                this::setUnitType,
                this::setUnitType,
                this::getUnitType,
                this::resetUnitType))
        .put("offence", MutableProperty.ofReadOnlyBoolean(this::getOffence))
        .put("defence", MutableProperty.ofReadOnlyBoolean(this::getDefence))
        .put("roll", MutableProperty.ofReadOnlyBoolean(this::getRoll))
        .put("strength", MutableProperty.ofReadOnlyBoolean(this::getStrength))
        .put("bonus",
            MutableProperty.ofInteger(
                this::setBonus,
                this::setBonus,
                this::getBonus,
                this::resetBonus))
        .put("number",
            MutableProperty.ofInteger(
                this::setNumber,
                this::setNumber,
                this::getNumber,
                this::resetNumber))
        .put("allied", MutableProperty.ofReadOnlyBoolean(this::getAllied))
        .put("enemy", MutableProperty.ofReadOnlyBoolean(this::getEnemy))
        .put("bonusType",
            MutableProperty.ofString(
                this::setBonusType,
                this::getBonusType,
                this::resetBonusType))
        .put("players",
            MutableProperty.of(
                List.class,
                this::setPlayers,
                this::setPlayers,
                this::getPlayers,
                this::resetPlayers))
        .put("impArtTech",
            MutableProperty.ofBoolean(
                this::setImpArtTech,
                this::setImpArtTech,
                this::getImpArtTech,
                this::resetImpArtTech))
        .put("dice",
            MutableProperty.ofString(
                this::setDice,
                this::getDice,
                this::resetDice))
        .put("side",
            MutableProperty.ofString(
                this::setSide,
                this::getSide,
                this::resetSide))
        .put("faction",
            MutableProperty.ofString(
                this::setFaction,
                this::getFaction,
                this::resetFaction))
        .build();
  }
}
