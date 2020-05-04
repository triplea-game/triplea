package games.strategy.triplea.attachments;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.Setter;

/**
 * An attachment for instances of {@link UnitType} that defines properties for unit types that
 * support other units.
 */
@Setter
public class UnitSupportAttachment extends DefaultAttachment {
  public static final String ATTACHMENT_NAME = "UnitSupportAttachment";

  private static final long serialVersionUID = -3015679930172496082L;

  /** Type to represent name and count */
  @Data
  public static class BonusType implements Serializable {
    private static final long serialVersionUID = -7445551357956238314L;

    @Nonnull String name;
    @Nonnull Integer count;

    public int getCount() {
      return count < 0 ? Integer.MAX_VALUE : count;
    }

    boolean isOldArtilleryRule() {
      return name.equals(Constants.OLD_ART_RULE_NAME);
    }
  }

  private Set<UnitType> unitType = null;
  private boolean offence = false;
  private boolean defence = false;
  private boolean roll = false;
  private boolean strength = false;
  private boolean aaRoll = false;
  private boolean aaStrength = false;
  private int bonus = 0;
  private int number = 0;
  private boolean allied = false;
  private boolean enemy = false;
  private BonusType bonusType = null;
  private List<GamePlayer> players = new ArrayList<>();
  private boolean impArtTech = false;
  // strings
  // roll or strength or AAroll or AAstrength
  private String dice;
  // offence or defence
  private String side;
  private String faction;

  public UnitSupportAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
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
      unitType = null;
      return;
    }
    unitType = new HashSet<>();
    for (final String element : splitOnColon(names)) {
      final UnitType type = getData().getUnitTypeList().getUnitType(element);
      if (type == null) {
        throw new GameParseException("Could not find unitType. name:" + element + thisErrorMsg());
      }
      unitType.add(type);
    }
  }

  public void setUnitType(final Set<UnitType> value) {
    unitType = value;
  }

  private void resetUnitType() {
    unitType = null;
  }

  private void setFaction(final String faction) throws GameParseException {
    this.faction = faction;
    if (faction == null) {
      resetFaction();
      return;
    }
    allied = false;
    enemy = false;
    for (final String element : splitOnColon(faction)) {
      if (element.equalsIgnoreCase("allied")) {
        allied = true;
      } else if (element.equalsIgnoreCase("enemy")) {
        enemy = true;
      } else {
        throw new GameParseException(
            faction + " faction must be allied, or enemy" + thisErrorMsg());
      }
    }
  }

  private String getFaction() {
    return faction;
  }

  private void resetFaction() {
    allied = false;
    enemy = false;
  }

  private void setSide(final String side) throws GameParseException {
    if (side == null) {
      resetSide();
      return;
    }
    defence = false;
    offence = false;
    for (final String element : splitOnColon(side)) {
      if (element.equalsIgnoreCase("defence")) {
        defence = true;
      } else if (element.equalsIgnoreCase("offence")) {
        offence = true;
      } else {
        throw new GameParseException(side + " side must be defence or offence" + thisErrorMsg());
      }
    }
    this.side = side;
  }

  private String getSide() {
    return side;
  }

  private void resetSide() {
    side = null;
    offence = false;
    defence = false;
  }

  private void setDice(final String dice) throws GameParseException {
    resetDice();
    if (dice == null) {
      return;
    }
    this.dice = dice;
    for (final String element : splitOnColon(dice)) {
      if (element.equalsIgnoreCase("roll")) {
        roll = true;
      } else if (element.equalsIgnoreCase("strength")) {
        strength = true;
      } else if (element.equalsIgnoreCase("AAroll")) {
        aaRoll = true;
      } else if (element.equalsIgnoreCase("AAstrength")) {
        aaStrength = true;
      } else {
        throw new GameParseException(
            dice + " dice must be roll, strength, AAroll, or AAstrength: " + thisErrorMsg());
      }
    }
  }

  String getDice() {
    return dice;
  }

  private void resetDice() {
    dice = null;
    roll = false;
    strength = false;
    aaRoll = false;
    aaStrength = false;
  }

  private void setBonus(final String bonus) {
    this.bonus = getInt(bonus);
  }

  public void setBonus(final int bonus) {
    this.bonus = bonus;
  }

  private void resetBonus() {
    bonus = 0;
  }

  private void setNumber(final String number) {
    this.number = getInt(number);
  }

  private void setNumber(final int number) {
    this.number = number;
  }

  private void resetNumber() {
    number = 0;
  }

  private void setBonusType(final String type) throws GameParseException {
    final String[] s = splitOnColon(type);
    if (s.length > 2) {
      throw new GameParseException(
          "bonusType can only have value and count: " + type + thisErrorMsg());
    }
    if (s.length == 1) {
      bonusType = new BonusType(s[0], 1);
    } else {
      bonusType = new BonusType(s[1], getInt(s[0]));
    }
  }

  public void setBonusType(final BonusType type) {
    bonusType = type;
  }

  private void resetBonusType() {
    bonusType = null;
  }

  private void setPlayers(final String names) throws GameParseException {
    final String[] s = splitOnColon(names);
    for (final String element : s) {
      final GamePlayer player = getData().getPlayerList().getPlayerId(element);
      if (player == null) {
        throw new GameParseException("Could not find player. name:" + element + thisErrorMsg());
      }
      players.add(player);
    }
  }

  private void setPlayers(final List<GamePlayer> value) {
    players = value;
  }

  public List<GamePlayer> getPlayers() {
    return players;
  }

  private void resetPlayers() {
    players = new ArrayList<>();
  }

  private void setImpArtTech(final String tech) {
    impArtTech = getBool(tech);
  }

  private void setImpArtTech(final boolean tech) {
    impArtTech = tech;
  }

  private void resetImpArtTech() {
    impArtTech = false;
  }

  public Set<UnitType> getUnitType() {
    return unitType;
  }

  public int getNumber() {
    return number;
  }

  public int getBonus() {
    return bonus;
  }

  public boolean getAllied() {
    return allied;
  }

  public boolean getEnemy() {
    return enemy;
  }

  public boolean getRoll() {
    return roll;
  }

  public boolean getStrength() {
    return strength;
  }

  public boolean getAaRoll() {
    return aaRoll;
  }

  public boolean getAaStrength() {
    return aaStrength;
  }

  public boolean getDefence() {
    return defence;
  }

  public boolean getOffence() {
    return offence;
  }

  public BonusType getBonusType() {
    return bonusType;
  }

  public boolean getImpArtTech() {
    return impArtTech;
  }

  /*
   * following are all to support old artillery flags.
   * boolean first is a cheat, adds a bogus support to a unit
   * in the case that supportable units are declared before any artillery
   */
  static void addRule(final UnitType type, final GameData data, final boolean first)
      throws GameParseException {
    final String attachmentName =
        (first ? Constants.SUPPORT_RULE_NAME_OLD_TEMP_FIRST : Constants.SUPPORT_RULE_NAME_OLD)
            + type.getName();
    final UnitSupportAttachment rule = new UnitSupportAttachment(attachmentName, type, data);
    rule.setBonus(1);
    rule.setBonusType(Constants.OLD_ART_RULE_NAME);
    rule.setDice("strength");
    rule.setFaction("allied");
    rule.setImpArtTech(true);
    rule.setNumber(first ? 0 : 1);
    rule.setSide("offence");
    rule.addUnitTypes(first ? Set.of(type) : getTargets(data));
    if (!first) {
      rule.setPlayers(new ArrayList<>(data.getPlayerList().getPlayers()));
    }
    type.addAttachment(attachmentName, rule);
  }

  private static Set<UnitType> getTargets(final GameData data) {
    Set<UnitType> types = null;
    for (final UnitSupportAttachment rule : get(data)) {
      if (rule.getBonusType().isOldArtilleryRule()) {
        types = rule.getUnitType();
        if (rule.getName().startsWith(Constants.SUPPORT_RULE_NAME_OLD_TEMP_FIRST)) {
          // remove it because it is a "first", which is just a temporary one made to hold target
          // info. what a hack.
          final UnitType attachedTo = (UnitType) rule.getAttachedTo();
          attachedTo.removeAttachment(rule.getName());
          rule.setAttachedTo(null);
        }
      }
    }
    return types;
  }

  private void addUnitTypes(final Set<UnitType> types) {
    if (types == null) {
      return;
    }
    if (unitType == null) {
      unitType = new HashSet<>();
    }
    unitType.addAll(types);
  }

  static void setOldSupportCount(final UnitType type, final GameData data, final String count) {
    for (final UnitSupportAttachment rule : get(data)) {
      if (rule.getBonusType().isOldArtilleryRule() && rule.getAttachedTo() == type) {
        rule.setNumber(count);
      }
    }
  }

  static void addTarget(final UnitType type, final GameData data) throws GameParseException {
    boolean first = true;
    for (final UnitSupportAttachment rule : get(data)) {
      if (rule.getBonusType().isOldArtilleryRule()) {
        rule.addUnitTypes(Set.of(type));
        first = false;
      }
    }
    // if first, it means we do not have any support attachments created yet. so create a temporary
    // one on this unit just to hold the target info.
    if (first) {
      addRule(type, data, true);
    }
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put(
            "unitType",
            MutableProperty.of(
                this::setUnitType, this::setUnitType, this::getUnitType, this::resetUnitType))
        .put("offence", MutableProperty.ofReadOnly(this::getOffence))
        .put("defence", MutableProperty.ofReadOnly(this::getDefence))
        .put("roll", MutableProperty.ofReadOnly(this::getRoll))
        .put("strength", MutableProperty.ofReadOnly(this::getStrength))
        .put("aaRoll", MutableProperty.ofReadOnly(this::getAaRoll))
        .put("aaStrength", MutableProperty.ofReadOnly(this::getAaStrength))
        .put(
            "bonus",
            MutableProperty.of(this::setBonus, this::setBonus, this::getBonus, this::resetBonus))
        .put(
            "number",
            MutableProperty.of(
                this::setNumber, this::setNumber, this::getNumber, this::resetNumber))
        .put("allied", MutableProperty.ofReadOnly(this::getAllied))
        .put("enemy", MutableProperty.ofReadOnly(this::getEnemy))
        .put(
            "bonusType",
            MutableProperty.of(
                this::setBonusType, this::setBonusType, this::getBonusType, this::resetBonusType))
        .put(
            "players",
            MutableProperty.of(
                this::setPlayers, this::setPlayers, this::getPlayers, this::resetPlayers))
        .put(
            "impArtTech",
            MutableProperty.of(
                this::setImpArtTech,
                this::setImpArtTech,
                this::getImpArtTech,
                this::resetImpArtTech))
        .put("dice", MutableProperty.ofString(this::setDice, this::getDice, this::resetDice))
        .put("side", MutableProperty.ofString(this::setSide, this::getSide, this::resetSide))
        .put(
            "faction",
            MutableProperty.ofString(this::setFaction, this::getFaction, this::resetFaction))
        .build();
  }
}
