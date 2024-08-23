package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jetbrains.annotations.NonNls;

/**
 * An attachment for instances of {@link UnitType} that defines properties for unit types that
 * support other units. Note: Empty collection fields default to null to minimize memory use and
 * serialization size.
 *
 * <p>The set of UnitSupportAttachments do not change during a game.
 */
public class UnitSupportAttachment extends DefaultAttachment {
  @NonNls public static final String BONUS = "bonus";
  @NonNls public static final String BONUS_TYPE = "bonusType";
  @NonNls public static final String DICE = "dice";
  @NonNls public static final String UNIT_TYPE = "unitType";

  private static final long serialVersionUID = -3015679930172496082L;

  private @Nullable Set<UnitType> unitType = null;
  private boolean offence = false;
  private boolean defence = false;
  private boolean roll = false;
  private boolean strength = false;
  private boolean aaRoll = false;
  private boolean aaStrength = false;
  @Getter private int bonus = 0;
  @Getter private int number = 0;
  private boolean allied = false;
  private boolean enemy = false;
  private @Nullable BonusType bonusType = null;
  private @Nullable List<GamePlayer> players = null;
  private boolean impArtTech = false;
  // strings
  // roll or strength or AAroll or AAstrength
  private @Nullable String dice;
  // offence or defence
  private @Nullable String side;
  private @Nullable String faction;

  /** Type to represent name and count */
  @Value
  public static class BonusType implements Serializable {
    private static final long serialVersionUID = -7445551357956238314L;

    @Nonnull String name;

    @EqualsAndHashCode.Exclude @Nonnull Integer count;

    public int getCount() {
      return count < 0 ? Integer.MAX_VALUE : count;
    }

    boolean isOldArtilleryRule() {
      return name.equals(Constants.OLD_ART_RULE_NAME);
    }
  }

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

  public static Set<UnitSupportAttachment> get(final UnitTypeList unitTypeList) {
    return unitTypeList.stream()
        .map(UnitSupportAttachment::get)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private void setUnitType(final String names) throws GameParseException {
    unitType = new HashSet<>();
    for (final String element : splitOnColon(names)) {
      unitType.add(getUnitTypeOrThrow(element));
    }
  }

  @VisibleForTesting
  public UnitSupportAttachment setUnitType(final Set<UnitType> value) {
    unitType = value;
    return this;
  }

  private void resetUnitType() {
    unitType = null;
  }

  @VisibleForTesting
  public UnitSupportAttachment setFaction(final String faction) throws GameParseException {
    this.faction = faction;
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
    return this;
  }

  private @Nullable String getFaction() {
    return faction;
  }

  private void resetFaction() {
    allied = false;
    enemy = false;
  }

  @VisibleForTesting
  public UnitSupportAttachment setSide(final String side) throws GameParseException {
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
    this.side = side.intern();
    return this;
  }

  private @Nullable String getSide() {
    return side;
  }

  private void resetSide() {
    side = null;
    offence = false;
    defence = false;
  }

  @VisibleForTesting
  public UnitSupportAttachment setDice(final String dice) throws GameParseException {
    resetDice();
    this.dice = dice.intern();
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
    return this;
  }

  @Nullable
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

  @VisibleForTesting
  public UnitSupportAttachment setBonus(final int bonus) {
    this.bonus = bonus;
    return this;
  }

  private void resetBonus() {
    bonus = 0;
  }

  private void setNumber(final String number) {
    this.number = getInt(number);
  }

  @VisibleForTesting
  public UnitSupportAttachment setNumber(final int number) {
    this.number = number;
    return this;
  }

  private void resetNumber() {
    number = 0;
  }

  @VisibleForTesting
  public UnitSupportAttachment setBonusType(final String type) throws GameParseException {
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
    return this;
  }

  @VisibleForTesting
  public UnitSupportAttachment setBonusType(final BonusType type) {
    bonusType = type;
    return this;
  }

  private void resetBonusType() {
    bonusType = null;
  }

  private void setPlayers(final String names) throws GameParseException {
    players = parsePlayerList(names, players);
  }

  @VisibleForTesting
  public UnitSupportAttachment setPlayers(final List<GamePlayer> value) {
    players = value;
    return this;
  }

  public List<GamePlayer> getPlayers() {
    return getListProperty(players);
  }

  private void resetPlayers() {
    players = null;
  }

  private void setImpArtTech(final String tech) {
    impArtTech = getBool(tech);
  }

  @VisibleForTesting
  public UnitSupportAttachment setImpArtTech(final boolean tech) {
    impArtTech = tech;
    return this;
  }

  private void resetImpArtTech() {
    impArtTech = false;
  }

  @Nullable
  public Set<UnitType> getUnitType() {
    return unitType;
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

  public @Nullable BonusType getBonusType() {
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
    rule.addUnitTypes(first ? Set.of(type) : getTargets(data.getUnitTypeList()));
    if (!first) {
      rule.setPlayers(new ArrayList<>(data.getPlayerList().getPlayers()));
    }
    type.addAttachment(attachmentName, rule);
  }

  private static Set<UnitType> getTargets(final UnitTypeList unitTypeList) {
    Set<UnitType> types = Set.of();
    for (final UnitSupportAttachment rule : get(unitTypeList)) {
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
    if (unitType == null) {
      unitType = new HashSet<>();
    }
    unitType.addAll(types);
  }

  static void setOldSupportCount(
      final UnitType type, final UnitTypeList unitTypeList, final String count) {
    for (final UnitSupportAttachment rule : get(unitTypeList)) {
      if (rule.getBonusType().isOldArtilleryRule() && rule.getAttachedTo() == type) {
        rule.setNumber(count);
      }
    }
  }

  static void addTarget(final UnitType type, final GameData data) throws GameParseException {
    boolean first = true;
    for (final UnitSupportAttachment rule : get(data.getUnitTypeList())) {
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
  public void validate(final GameState data) {}

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case UNIT_TYPE:
        return MutableProperty.of(
            this::setUnitType, this::setUnitType, this::getUnitType, this::resetUnitType);
      case "offence":
        return MutableProperty.ofReadOnly(this::getOffence);
      case "defence":
        return MutableProperty.ofReadOnly(this::getDefence);
      case "roll":
        return MutableProperty.ofReadOnly(this::getRoll);
      case "strength":
        return MutableProperty.ofReadOnly(this::getStrength);
      case "aaRoll":
        return MutableProperty.ofReadOnly(this::getAaRoll);
      case "aaStrength":
        return MutableProperty.ofReadOnly(this::getAaStrength);
      case BONUS:
        return MutableProperty.of(this::setBonus, this::setBonus, this::getBonus, this::resetBonus);
      case "number":
        return MutableProperty.of(
            this::setNumber, this::setNumber, this::getNumber, this::resetNumber);
      case "allied":
        return MutableProperty.ofReadOnly(this::getAllied);
      case "enemy":
        return MutableProperty.ofReadOnly(this::getEnemy);
      case BONUS_TYPE:
        return MutableProperty.of(
            this::setBonusType, this::setBonusType, this::getBonusType, this::resetBonusType);
      case "players":
        return MutableProperty.of(
            this::setPlayers, this::setPlayers, this::getPlayers, this::resetPlayers);
      case "impArtTech":
        return MutableProperty.of(
            this::setImpArtTech, this::setImpArtTech, this::getImpArtTech, this::resetImpArtTech);
      case DICE:
        return MutableProperty.ofString(this::setDice, this::getDice, this::resetDice);
      case "side":
        return MutableProperty.ofString(this::setSide, this::getSide, this::resetSide);
      case "faction":
        return MutableProperty.ofString(this::setFaction, this::getFaction, this::resetFaction);
      default:
        return null;
    }
  }
}
