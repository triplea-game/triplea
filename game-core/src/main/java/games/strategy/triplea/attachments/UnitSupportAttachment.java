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
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;

@MapSupport
public class UnitSupportAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -3015679930172496082L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private Set<UnitType> m_unitType = null;
  @InternalDoNotExport
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_offence = false;
  @InternalDoNotExport
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_defence = false;
  @InternalDoNotExport
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_roll = false;
  @InternalDoNotExport
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_strength = false;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_bonus = 0;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_number = 0;
  @InternalDoNotExport
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_allied = false;
  @InternalDoNotExport
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_enemy = false;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_bonusType = null;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private List<PlayerID> m_players = new ArrayList<>();
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_impArtTech = false;
  // strings
  // roll or strength
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_dice;
  // offence or defence
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_side;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
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

  private void setUnitType(final String names) throws GameParseException {
    if (names == null) {
      m_unitType = null;
      return;
    }
    m_unitType = new HashSet<>();
    for (final String element : splitOnColon(names)) {
      final UnitType type = getData().getUnitTypeList().getUnitType(element);
      if (type == null) {
        throw new GameParseException("Could not find unitType. name:" + element + thisErrorMsg());
      }
      m_unitType.add(type);
    }
  }

  private void setUnitType(final Set<UnitType> value) {
    m_unitType = value;
  }

  private void resetUnitType() {
    m_unitType = null;
  }

  private void setFaction(final String faction) throws GameParseException {
    m_faction = faction;
    if (faction == null) {
      resetFaction();
      return;
    }
    m_allied = false;
    m_enemy = false;
    for (final String element : splitOnColon(faction)) {
      if (element.equalsIgnoreCase("allied")) {
        m_allied = true;
      } else if (element.equalsIgnoreCase("enemy")) {
        m_enemy = true;
      } else {
        throw new GameParseException(faction + " faction must be allied, or enemy" + thisErrorMsg());
      }
    }
  }

  private String getFaction() {
    return m_faction;
  }

  private void resetFaction() {
    m_allied = false;
    m_enemy = false;
  }

  private void setSide(final String side) throws GameParseException {
    if (side == null) {
      resetSide();
      return;
    }
    m_defence = false;
    m_offence = false;
    for (final String element : splitOnColon(side)) {
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

  private String getSide() {
    return m_side;
  }

  private void resetSide() {
    m_side = null;
    m_offence = false;
    m_defence = false;
  }

  private void setDice(final String dice) throws GameParseException {
    if (dice == null) {
      resetDice();
      return;
    }
    m_roll = false;
    m_strength = false;
    for (final String element : splitOnColon(dice)) {
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

  private String getDice() {
    return m_dice;
  }

  private void resetDice() {
    m_dice = null;
    m_roll = false;
    m_strength = false;
  }

  private void setBonus(final String bonus) {
    m_bonus = getInt(bonus);
  }

  private void setBonus(final int bonus) {
    m_bonus = bonus;
  }

  private void resetBonus() {
    m_bonus = 0;
  }

  private void setNumber(final String number) {
    m_number = getInt(number);
  }

  private void setNumber(final int number) {
    m_number = number;
  }

  private void resetNumber() {
    m_number = 0;
  }

  private void setBonusType(final String type) {
    m_bonusType = type;
  }

  private void resetBonusType() {
    m_bonusType = null;
  }

  private void setPlayers(final String names) throws GameParseException {
    final String[] s = splitOnColon(names);
    for (final String element : s) {
      final PlayerID player = getData().getPlayerList().getPlayerId(element);
      if (player == null) {
        throw new GameParseException("Could not find player. name:" + element + thisErrorMsg());
      }
      m_players.add(player);
    }
  }

  private void setPlayers(final List<PlayerID> value) {
    m_players = value;
  }

  public List<PlayerID> getPlayers() {
    return m_players;
  }

  private void resetPlayers() {
    m_players = new ArrayList<>();
  }

  private void setImpArtTech(final String tech) {
    m_impArtTech = getBool(tech);
  }

  private void setImpArtTech(final boolean tech) {
    m_impArtTech = tech;
  }

  private void resetImpArtTech() {
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
    rule.setBonus(1);
    rule.setBonusType(Constants.OLD_ART_RULE_NAME);
    rule.setDice("strength");
    rule.setFaction("allied");
    rule.setImpArtTech(true);
    rule.setNumber(first ? 0 : 1);
    rule.setSide("offence");
    rule.addUnitTypes(first ? Collections.singleton(type) : getTargets(data));
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
                this::setUnitType,
                this::setUnitType,
                this::getUnitType,
                this::resetUnitType))
        .put("offence", MutableProperty.ofReadOnly(this::getOffence))
        .put("defence", MutableProperty.ofReadOnly(this::getDefence))
        .put("roll", MutableProperty.ofReadOnly(this::getRoll))
        .put("strength", MutableProperty.ofReadOnly(this::getStrength))
        .put("bonus",
            MutableProperty.of(
                this::setBonus,
                this::setBonus,
                this::getBonus,
                this::resetBonus))
        .put("number",
            MutableProperty.of(
                this::setNumber,
                this::setNumber,
                this::getNumber,
                this::resetNumber))
        .put("allied", MutableProperty.ofReadOnly(this::getAllied))
        .put("enemy", MutableProperty.ofReadOnly(this::getEnemy))
        .put("bonusType",
            MutableProperty.ofString(
                this::setBonusType,
                this::getBonusType,
                this::resetBonusType))
        .put("players",
            MutableProperty.of(
                this::setPlayers,
                this::setPlayers,
                this::getPlayers,
                this::resetPlayers))
        .put("impArtTech",
            MutableProperty.of(
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
