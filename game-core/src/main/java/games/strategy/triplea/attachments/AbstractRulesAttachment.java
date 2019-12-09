package games.strategy.triplea.attachments;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;

/** The Purpose of this class is to hold shared and simple methods used by RulesAttachment. */
public abstract class AbstractRulesAttachment extends AbstractConditionsAttachment {
  private static final long serialVersionUID = -6977650137928964759L;

  // Determines if we will be counting each for the purposes of objectiveValue
  private boolean countEach = false;
  // The multiple that will be applied to objectiveValue if countEach is true
  protected int eachMultiple = 1;
  // Used with the next Territory conditions to determine the number of territories needed to be
  // valid
  // (ex: alliedOwnershipTerritories)
  private int territoryCount = -1;
  // A list of players that can be used with directOwnershipTerritories, directExclusionTerritories,
  // directPresenceTerritories, or any of the other territory lists
  // only used if the attachment begins with "objectiveAttachment"
  protected List<PlayerId> players = new ArrayList<>();
  protected int objectiveValue = 0;
  // only matters for objectiveValue, does not affect the condition
  protected int uses = -1;
  // condition for what turn it is
  protected Map<Integer, Integer> turns = null;
  // for on/off conditions
  protected boolean switched = true;
  // allows custom GameProperties
  protected String gameProperty = null;

  protected AbstractRulesAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  private void setPlayers(final String names) throws GameParseException {
    final PlayerList pl = getData().getPlayerList();
    for (final String p : splitOnColon(names)) {
      final PlayerId player = pl.getPlayerId(p);
      if (player == null) {
        throw new GameParseException("Could not find player. name:" + p + thisErrorMsg());
      }
      players.add(player);
    }
  }

  private void setPlayers(final List<PlayerId> value) {
    players = value;
  }

  protected List<PlayerId> getPlayers() {
    return players.isEmpty() ? new ArrayList<>(List.of((PlayerId) getAttachedTo())) : players;
  }

  private void resetPlayers() {
    players = new ArrayList<>();
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

  public int getObjectiveValue() {
    return objectiveValue;
  }

  private void resetObjectiveValue() {
    objectiveValue = 0;
  }

  /**
   * Internal use only, is not set by xml or property utils. Is used to determine the number of
   * territories we need to satisfy a specific territory based condition check. It is set multiple
   * times during each check [isSatisfied], as there might be multiple types of territory checks
   * being done. So it is just a temporary value.
   */
  private void setTerritoryCount(final String value) {
    if (value.equals("each")) {
      territoryCount = 1;
      countEach = true;
    } else {
      territoryCount = getInt(value);
    }
  }

  private void setTerritoryCount(final int setTerritoryCount) {
    this.territoryCount = setTerritoryCount;
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

  /**
   * "uses" on RulesAttachments apply ONLY to giving money (PUs) to the player, they do NOT apply to
   * the condition, and therefore should not be tested for in isSatisfied.
   */
  public int getUses() {
    return uses;
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
    gameProperty = value;
  }

  private String getGameProperty() {
    return gameProperty;
  }

  boolean getGamePropertyState(final GameData data) {
    return gameProperty != null && data.getProperties().get(gameProperty, false);
  }

  private void resetGameProperty() {
    gameProperty = null;
  }

  private void setRounds(final String rounds) throws GameParseException {
    if (rounds == null) {
      turns = null;
      return;
    }
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

  private void setTurns(final String turns) throws GameParseException {
    setRounds(turns);
  }

  private void setTurns(final Map<Integer, Integer> value) {
    turns = value;
  }

  private Map<Integer, Integer> getTurns() {
    return turns;
  }

  private void resetTurns() {
    turns = null;
  }

  protected boolean checkTurns(final GameData data) {
    final int turn = data.getSequence().getRound();
    for (final int t : turns.keySet()) {
      if (turn >= t && turn <= turns.get(t)) {
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
      final String name, final Collection<PlayerId> players, final GameData data) {
    final GameMap gameMap = data.getMap();
    switch (name) {
      case "original":
      case "enemy":
        // get all originally owned territories
        final Set<Territory> originalTerrs = new HashSet<>();
        for (final PlayerId player : players) {
          originalTerrs.addAll(OriginalOwnerTracker.getOriginallyOwned(data, player));
        }
        setTerritoryCount(originalTerrs.size());
        return originalTerrs;
      case "originalNoWater":
        // get all originally owned territories, but no water or impassables
        final Set<Territory> originalTerritories = new HashSet<>();
        for (final PlayerId player : players) {
          originalTerritories.addAll(
              CollectionUtils.getMatches(
                  OriginalOwnerTracker.getOriginallyOwned(data, player),
                  // TODO: does this account for occupiedTerrOf???
                  Matches.territoryIsNotImpassableToLandUnits(player, data)));
        }
        setTerritoryCount(originalTerritories.size());
        return originalTerritories;

      case "controlled":
        final Set<Territory> ownedTerrs = new HashSet<>();
        for (final PlayerId player : players) {
          ownedTerrs.addAll(gameMap.getTerritoriesOwnedBy(player));
        }
        setTerritoryCount(ownedTerrs.size());
        return ownedTerrs;
      case "controlledNoWater":
        final Set<Territory> ownedTerrsNoWater = new HashSet<>();
        for (final PlayerId player : players) {
          ownedTerrsNoWater.addAll(
              CollectionUtils.getMatches(
                  gameMap.getTerritoriesOwnedBy(player),
                  Matches.territoryIsNotImpassableToLandUnits(player, data)));
        }
        setTerritoryCount(ownedTerrsNoWater.size());
        return ownedTerrsNoWater;
      case "all":
        final Set<Territory> allTerrs = new HashSet<>();
        for (final PlayerId player : players) {
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
          throw new IllegalStateException("No territory called:" + name + thisErrorMsg());
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
      final String[] terrs, final Collection<PlayerId> players, final GameData data) {
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
      // set it a second time, since getTerritoriesBasedOnStringName also sets it (so do it
      setTerritoryCount(String.valueOf(terrs[0]));
      // after the method call).
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
      if (testFirstItemForCount && i == 0) {
        // See if the first entry contains the number of territories needed to meet the criteria
        try {
          // check if this is an integer, and if so set territory count
          final int territoryCount = Integer.parseInt(name);
          if (mustSetTerritoryCount) {
            haveSetCount = true;
            setTerritoryCount(territoryCount);
          }
          continue;
        } catch (final NumberFormatException e) {
          // territory name is not an integer; fall through
        }
      }
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
      // Validate all territories exist
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new IllegalStateException("No territory called:" + name + thisErrorMsg());
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
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("countEach", MutableProperty.ofReadOnly(this::getCountEach))
        .put("eachMultiple", MutableProperty.ofReadOnly(this::getEachMultiple))
        .put(
            "players",
            MutableProperty.of(
                this::setPlayers, this::setPlayers, this::getPlayers, this::resetPlayers))
        .put(
            "objectiveValue",
            MutableProperty.of(
                this::setObjectiveValue,
                this::setObjectiveValue,
                this::getObjectiveValue,
                this::resetObjectiveValue))
        .put(
            "uses",
            MutableProperty.of(this::setUses, this::setUses, this::getUses, this::resetUses))
        .put(
            "turns",
            MutableProperty.of(this::setTurns, this::setTurns, this::getTurns, this::resetTurns))
        .put(
            "switch",
            MutableProperty.of(
                this::setSwitch, this::setSwitch, this::getSwitch, this::resetSwitch))
        .put(
            "gameProperty",
            MutableProperty.ofString(
                this::setGameProperty, this::getGameProperty, this::resetGameProperty))
        .put("rounds", MutableProperty.ofWriteOnlyString(this::setRounds))
        .build();
  }
}
