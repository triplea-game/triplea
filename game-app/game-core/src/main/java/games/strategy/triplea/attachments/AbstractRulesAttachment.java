package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.java.RenameOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;

/**
 * The Purpose of this class is to hold shared and simple methods used by RulesAttachment. Note:
 * Empty collection fields default to null to minimize memory use and serialization size.
 */
public abstract class AbstractRulesAttachment extends AbstractConditionsAttachment {
  private static final long serialVersionUID = -6977650137928964759L;

  // The multiple that will be applied to objectiveValue if countEach is true
  protected int eachMultiple = 1;
  // A list of players that can be used with directOwnershipTerritories, directExclusionTerritories,
  // directPresenceTerritories, or any of the other territory lists
  // only used if the attachment begins with "objectiveAttachment"
  // Note: Subclasses should use getPlayers() which take into account getAttachedTo().
  private @Nullable List<GamePlayer> players = null;
  @Getter protected int objectiveValue = 0;

  /**
   * -- GETTER -- "uses" on RulesAttachments apply ONLY to giving money (PUs) to the player, they do
   * NOT apply to the condition, and therefore should not be tested for in isSatisfied.
   */
  // only matters for objectiveValue, does not affect the condition
  @Getter protected int uses = -1;

  // condition for what turn it is
  @RenameOnNextMajorRelease(newName = "rounds")
  protected @Nullable Map<Integer, Integer> turns = null;

  // for on/off conditions
  protected boolean switched = true;
  // allows custom GameProperties
  protected @Nullable String gameProperty = null;
  // Determines if we will be counting each for the purposes of objectiveValue
  private boolean countEach = false;
  // Used with the next Territory conditions to determine the number of territories needed to be
  // valid (ex: alliedOwnershipTerritories)
  private int territoryCount = -1;

  protected AbstractRulesAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  private void setPlayers(final String names) throws GameParseException {
    players = parsePlayerList(names, players);
  }

  private void setPlayers(final List<GamePlayer> value) {
    players = value;
  }

  protected List<GamePlayer> getPlayers() {
    List<GamePlayer> result = getListProperty(players);
    return result.isEmpty() ? List.of((GamePlayer) getAttachedTo()) : result;
  }

  private void resetPlayers() {
    players = null;
  }

  @Override
  protected void setChance(final String chance) throws GameParseException {
    throw new GameParseException(
        "chance not allowed for use with RulesAttachments, instead use it with "
            + "Triggers or PoliticalActions"
            + thisErrorMsg());
  }

  private void setObjectiveValue(final String value) {
    objectiveValue = getInt(value);
  }

  private void setObjectiveValue(final int value) {
    objectiveValue = value;
  }

  private void resetObjectiveValue() {
    objectiveValue = 0;
  }

  private void setTerritoryCount(final int territoryCount) {
    this.territoryCount = territoryCount;
  }

  int getTerritoryCount() {
    return territoryCount;
  }

  /**
   * Used to determine if there is a multiple on this national objective (if the user specified
   * 'each' in the count. For example, you may want to have the player receive 3 PUs for controlling
   * each territory, in a list of territories.
   */
  public int getEachMultiple() {
    if (!getCountEach()) {
      return 1;
    }
    return eachMultiple;
  }

  protected boolean getCountEach() {
    return countEach;
  }

  private void setUses(final String s) {
    uses = getInt(s);
  }

  private void setUses(final int u) {
    uses = u;
  }

  private void resetUses() {
    uses = -1;
  }

  private void setSwitch(final String value) {
    switched = getBool(value);
  }

  private void setSwitch(final Boolean value) {
    switched = value;
  }

  private boolean getSwitch() {
    return switched;
  }

  private void resetSwitch() {
    switched = true;
  }

  private void setGameProperty(final String value) {
    gameProperty = value.intern();
  }

  private @Nullable String getGameProperty() {
    return gameProperty;
  }

  boolean getGamePropertyState(final GameState data) {
    return gameProperty != null && data.getProperties().get(gameProperty, false);
  }

  private void resetGameProperty() {
    gameProperty = null;
  }

  private void setRounds(final String rounds) throws GameParseException {
    turns = new HashMap<>();
    final String[] s = splitOnColon(rounds);
    if (s.length < 1) {
      throw new GameParseException("Empty turn list" + thisErrorMsg());
    }
    for (final String subString : s) {
      int start;
      int end;
      try {
        start = getInt(subString);
        end = start;
      } catch (final Exception e) {
        final String[] s2 = splitOnHyphen(subString);
        if (s2.length != 2) {
          throw new GameParseException(
              "Invalid syntax for turn range, must be 'int-int'" + thisErrorMsg());
        }
        start = getInt(s2[0]);
        if (s2[1].equals("+")) {
          end = Integer.MAX_VALUE;
        } else {
          end = getInt(s2[1]);
        }
      }
      turns.put(start, end);
    }
  }

  private void setRounds(final Map<Integer, Integer> value) {
    turns = value;
  }

  @VisibleForTesting
  public Map<Integer, Integer> getRounds() {
    return getMapProperty(turns);
  }

  private void resetRounds() {
    turns = null;
  }

  protected boolean checkRounds(final GameState data) {
    final int turn = data.getSequence().getRound();
    for (final Map.Entry<Integer, Integer> entry : getRounds().entrySet()) {
      if (turn >= entry.getKey() && turn <= entry.getValue()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Takes a string like "original", "originalNoWater", "enemy", "controlled", "controlledNoWater",
   * "all", "map", and turns it into an actual list of territories. Also sets territoryCount.
   */
  private Set<Territory> getTerritoriesBasedOnStringName(
      final String name, final Collection<GamePlayer> players, final GameState data) {
    final GameMap gameMap = data.getMap();
    switch (name) {
      case "original":
      case "enemy":
        // get all originally owned territories
        final Set<Territory> originalTerrs = new HashSet<>();
        for (final GamePlayer player : players) {
          originalTerrs.addAll(OriginalOwnerTracker.getOriginallyOwned(data, player));
        }
        setTerritoryCount(originalTerrs.size());
        return originalTerrs;
      case "originalNoWater":
        // get all originally owned territories, but no water or impassables
        final Set<Territory> originalTerritories = new HashSet<>();
        for (final GamePlayer player : players) {
          originalTerritories.addAll(
              CollectionUtils.getMatches(
                  OriginalOwnerTracker.getOriginallyOwned(data, player),
                  // TODO: does this account for occupiedTerrOf???
                  Matches.territoryIsNotImpassableToLandUnits(player)));
        }
        setTerritoryCount(originalTerritories.size());
        return originalTerritories;

      case "controlled":
        final Set<Territory> ownedTerrs = new HashSet<>();
        for (final GamePlayer player : players) {
          ownedTerrs.addAll(gameMap.getTerritoriesOwnedBy(player));
        }
        setTerritoryCount(ownedTerrs.size());
        return ownedTerrs;
      case "controlledNoWater":
        final Set<Territory> ownedTerrsNoWater = new HashSet<>();
        for (final GamePlayer player : players) {
          ownedTerrsNoWater.addAll(
              CollectionUtils.getMatches(
                  gameMap.getTerritoriesOwnedBy(player),
                  Matches.territoryIsNotImpassableToLandUnits(player)));
        }
        setTerritoryCount(ownedTerrsNoWater.size());
        return ownedTerrsNoWater;
      case "all":
        final Set<Territory> allTerrs = new HashSet<>();
        for (final GamePlayer player : players) {
          allTerrs.addAll(gameMap.getTerritoriesOwnedBy(player));
          allTerrs.addAll(OriginalOwnerTracker.getOriginallyOwned(data, player));
        }
        setTerritoryCount(allTerrs.size());
        return allTerrs;
      case "map":
        final Set<Territory> allTerritories = new HashSet<>(gameMap.getTerritories());
        setTerritoryCount(allTerritories.size());
        return allTerritories;
      default: // The list just contained 1 territory
        final Territory t = data.getMap().getTerritory(name);
        if (t == null) {
          throw new IllegalStateException("No territory called: " + name + thisErrorMsg());
        }
        final Set<Territory> terr = new HashSet<>();
        terr.add(t);
        setTerritoryCount(1);
        return terr;
    }
  }

  /**
   * Takes the raw data from the xml, and turns it into an actual territory list. Will also set
   * territoryCount.
   */
  protected Set<Territory> getTerritoryListBasedOnInputFromXml(
      final String[] terrs, final Collection<GamePlayer> players, final GameState data) {
    // If there's only 1, it might be a 'group' (original, controlled, controlledNoWater, all)
    if (terrs.length == 1) {
      return getTerritoriesBasedOnStringName(terrs[0], players, data);
    } else if (terrs.length == 2) {
      if (!terrs[1].equals("controlled")
          && !terrs[1].equals("controlledNoWater")
          && !terrs[1].equals("original")
          && !terrs[1].equals("originalNoWater")
          && !terrs[1].equals("all")
          && !terrs[1].equals("map")
          && !terrs[1].equals("enemy")) {
        // Get the list of territories
        return getListedTerritories(terrs, true, true);
      }

      final Set<Territory> territories = getTerritoriesBasedOnStringName(terrs[1], players, data);
      // set it a second time, since getTerritoriesBasedOnStringName also sets it
      // (so do it after the method call).
      if ("each".equals(terrs[0])) {
        setTerritoryCount(1);
        countEach = true;
      } else {
        setTerritoryCount(getInt(terrs[0]));
      }

      return territories;
    } else {
      // Get the list of territories
      return getListedTerritories(terrs, true, true);
    }
  }

  protected void validateNames(final String[] terrList) {
    if (terrList != null && terrList.length > 0) {
      getListedTerritories(terrList, true, true);
      // removed checks for length & group commands because it breaks the setTerritoryCount feature.
      for (int i = 0; i < terrList.length; i++) {
        terrList[i] = terrList[i].intern();
      }
    }
  }

  /**
   * Validate that all listed territories actually exist. Will return an empty list of territories
   * if sent a list that is empty or contains only an empty/{@code null} string.
   */
  public Set<Territory> getListedTerritories(
      final String[] list,
      final boolean testFirstItemForCount,
      final boolean mustSetTerritoryCount) {
    final Set<Territory> territories = new HashSet<>();
    // this list is null, empty, or contains "", so return a blank list of territories
    if (list == null
        || list.length == 0
        || (list.length == 1 && (list[0] == null || list[0].length() == 0))) {
      return territories;
    }
    boolean haveSetCount = false;
    for (int i = 0; i < list.length; i++) {
      final String name = list[i];
      if (name.equals("each")) {
        countEach = true;
        if (mustSetTerritoryCount) {
          haveSetCount = true;
          setTerritoryCount(1);
        }
        continue;
      }
      // Skip looking for the territory if the original list contains one of the 'group' commands
      if (name.equals("controlled")
          || name.equals("controlledNoWater")
          || name.equals("original")
          || name.equals("originalNoWater")
          || name.equals("all")
          || name.equals("map")
          || name.equals("enemy")) {
        break;
      }
      if (testFirstItemForCount && i == 0) {
        // See if the first entry contains the number of territories needed to meet the criteria
        // check if this is an integer, and if so set territory count
        final Integer territoryCount = Ints.tryParse(name);
        if (territoryCount != null) {
          if (mustSetTerritoryCount) {
            haveSetCount = true;
            setTerritoryCount(territoryCount);
          }
          continue;
        }
        // territory name is not an integer; fall through
      }
      // Validate all territories exist
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new IllegalStateException("No territory called: " + name + thisErrorMsg());
      }
      territories.add(territory);
    }
    if (mustSetTerritoryCount && !haveSetCount) {
      // if we have not set it, then set it to be the size of this list
      setTerritoryCount(territories.size());
    }
    return territories;
  }

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "countEach":
        return MutableProperty.ofReadOnly(this::getCountEach);
      case "eachMultiple":
        return MutableProperty.ofReadOnly(this::getEachMultiple);
      case "players":
        return MutableProperty.of(
            this::setPlayers, this::setPlayers, this::getPlayers, this::resetPlayers);
      case "objectiveValue":
        return MutableProperty.of(
            this::setObjectiveValue,
            this::setObjectiveValue,
            this::getObjectiveValue,
            this::resetObjectiveValue);
      case "uses":
        return MutableProperty.of(this::setUses, this::setUses, this::getUses, this::resetUses);
      case "rounds":
        return MutableProperty.of(
            this::setRounds, this::setRounds, this::getRounds, this::resetRounds);
      case "switch":
        return MutableProperty.of(
            this::setSwitch, this::setSwitch, this::getSwitch, this::resetSwitch);
      case "gameProperty":
        return MutableProperty.ofString(
            this::setGameProperty, this::getGameProperty, this::resetGameProperty);
      default:
        return super.getPropertyOrNull(propertyName);
    }
  }
}
