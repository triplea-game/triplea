package games.strategy.triplea.attachments;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.BattleRecordsList;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.triplea.java.Interruptibles;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * An attachment for instances of {@link GamePlayer} that defines various conditions for enabling
 * certain rules (see the class description of {@link AbstractPlayerRulesAttachment}). Note: Empty
 * collection fields default to null to minimize memory use and serialization size.
 */
public class RulesAttachment extends AbstractPlayerRulesAttachment {
  private static final long serialVersionUID = 7301965634079412516L;

  // condition for having techs
  private @Nullable List<TechAdvance> techs = null;
  private int techCount = -1;
  // condition for having specific relationships
  private @Nullable List<String> relationship = null;
  // condition for checking AI player
  private @Nullable Boolean isAI = null;
  // condition for being at war
  private @Nullable Set<GamePlayer> atWarPlayers = null;
  private int atWarCount = -1;
  // condition for having destroyed at least X enemy non-neutral TUV (total unit value) [according
  // to
  // the prices the defender pays for the units]
  private @Nullable String destroyedTuv = null;
  // condition for having had a battle in some territory, attacker or defender, win
  // or lost, etc. these next 9 variables use territoryCount for determining the number needed.
  private @Nullable List<Tuple<String, List<Territory>>> battle = null;
  // ownership related
  private @Nullable String[] alliedOwnershipTerritories = null;
  private @Nullable String[] directOwnershipTerritories = null;
  // exclusion of units
  private @Nullable String[] alliedExclusionTerritories = null;
  private @Nullable String[] directExclusionTerritories = null;
  private @Nullable String[] enemyExclusionTerritories = null;
  private @Nullable String[] enemySurfaceExclusionTerritories = null;
  // presence of units
  private @Nullable String[] directPresenceTerritories = null;
  private @Nullable String[] alliedPresenceTerritories = null;
  private @Nullable String[] enemyPresenceTerritories = null;
  // used with above 3 to determine the type of unit that must be present
  private @Nullable IntegerMap<String> unitPresence = null;

  public RulesAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Convenience method, for use with rules attachments, objectives, and condition attachments.
   * Should return RulesAttachments.
   *
   * @param player PlayerId
   * @param nameOfAttachment exact full name of attachment
   * @return new rule attachment
   */
  public static RulesAttachment get(final GamePlayer player, final String nameOfAttachment) {
    return get(player, nameOfAttachment, List.of(), false);
  }

  static RulesAttachment get(
      final GamePlayer player,
      final String nameOfAttachment,
      final Collection<GamePlayer> playersToSearch,
      final boolean allowNull) {
    RulesAttachment ra = (RulesAttachment) player.getAttachment(nameOfAttachment);
    if (ra != null) {
      return ra;
    }
    for (final GamePlayer otherPlayer : playersToSearch) {
      if (otherPlayer.equals(player)) {
        continue;
      }
      ra = (RulesAttachment) otherPlayer.getAttachment(nameOfAttachment);
      if (ra != null) {
        return ra;
      }
    }
    if (!allowNull) {
      throw new IllegalStateException(
          "Rules & Conditions: No rule attachment for:"
              + player.getName()
              + " with name: "
              + nameOfAttachment);
    }
    return null;
  }

  /**
   * Convenience method, for use returning any RulesAttachment that begins with
   * "objectiveAttachment" National Objectives are just conditions that also give money to a player
   * during the end turn delegate. They can be used for testing by triggers as well. Conditions that
   * do not give money are not prefixed with "objectiveAttachment", and the trigger attachment that
   * uses these kinds of conditions gets them a different way because they are specifically named
   * inside that trigger.
   */
  public static Set<RulesAttachment> getNationalObjectives(final GamePlayer player) {
    final Set<RulesAttachment> natObjs = new HashSet<>();
    final Map<String, IAttachment> map = player.getAttachments();
    for (final Map.Entry<String, IAttachment> entry : map.entrySet()) {
      final IAttachment attachment = entry.getValue();
      if (attachment instanceof RulesAttachment
          && attachment.getName().startsWith(Constants.RULES_OBJECTIVE_PREFIX)) {
        natObjs.add((RulesAttachment) attachment);
      }
    }
    return natObjs;
  }

  private void setDestroyedTuv(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(
          "destroyedTUV must have 2 fields, value=currentRound/allRounds, count= the amount "
              + "of TUV that this player must destroy"
              + thisErrorMsg());
    }
    final int i = getInt(s[0]);
    if (i < -1) {
      throw new GameParseException(
          "destroyedTUV count cannot be less than -1 [with -1 meaning the condition is not active]"
              + thisErrorMsg());
    }
    if (!(s[1].equals("currentRound") || s[1].equals("allRounds"))) {
      throw new GameParseException(
          "destroyedTUV value must be currentRound or allRounds" + thisErrorMsg());
    }
    destroyedTuv = value.intern();
  }

  private @Nullable String getDestroyedTuv() {
    return destroyedTuv;
  }

  private void resetDestroyedTuv() {
    destroyedTuv = null;
  }

  private void setBattle(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 5) {
      throw new GameParseException(
          "battle must have at least 5 fields, attacker:defender:resultType:round:territory1..."
              + thisErrorMsg());
    }
    final GamePlayer attacker = getData().getPlayerList().getPlayerId(s[0]);
    if (attacker == null && !s[0].equalsIgnoreCase("any")) {
      throw new GameParseException("no player named: " + s[0] + thisErrorMsg());
    }
    final GamePlayer defender = getData().getPlayerList().getPlayerId(s[1]);
    if (defender == null && !s[1].equalsIgnoreCase("any")) {
      throw new GameParseException("no player named: " + s[1] + thisErrorMsg());
    }
    if (!s[2].equalsIgnoreCase("any")) {
      throw new GameParseException(
          "battle allows the following for resultType: any" + thisErrorMsg());
    }
    if (!s[3].equalsIgnoreCase("currentRound")) {
      try {
        getInt(splitOnHyphen(s[3])[0]);
        getInt(splitOnHyphen(s[3])[1]);
      } catch (final Exception e) {
        throw new GameParseException(
            "round must either be currentRound or two numbers like: 2-4" + thisErrorMsg());
      }
    }
    final List<Territory> terrs = new ArrayList<>();
    final GameMap map = getData().getMap();
    // this loop starts on 4, so do not replace with an enhanced for loop
    for (int i = 4; i < s.length; i++) {
      final Territory t = map.getTerritory(s[i]);
      if (t == null) {
        throw new GameParseException("no such territory called: " + s[i] + thisErrorMsg());
      }
      terrs.add(t);
    }
    if (battle == null) {
      battle = new ArrayList<>();
    }
    battle.add(Tuple.of((s[0] + ":" + s[1] + ":" + s[2] + ":" + s[3]).intern(), terrs));
  }

  private void setBattle(final List<Tuple<String, List<Territory>>> value) {
    battle = value;
  }

  private List<Tuple<String, List<Territory>>> getBattle() {
    return getListProperty(battle);
  }

  private void resetBattle() {
    battle = null;
  }

  /**
   * Condition to check if a certain relationship exists between 2 players.
   *
   * @param value should be a string containing: "player:player:relationship"
   */
  private void setRelationship(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3 || s.length > 4) {
      throw new GameParseException(
          "relationship should have value=\"playername1:playername2"
              + ":relationshiptype:numberOfRoundsExisting\""
              + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[0]) == null) {
      throw new GameParseException(
          "playername: "
              + s[0]
              + " isn't valid in condition with relationship: "
              + value
              + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[1]) == null) {
      throw new GameParseException(
          "playername: "
              + s[1]
              + " isn't valid in condition with relationship: "
              + value
              + thisErrorMsg());
    }
    if (!(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
        || Matches.isValidRelationshipName(getData().getRelationshipTypeList()).test(s[2]))) {
      throw new GameParseException(
          "relationship: "
              + s[2]
              + " isn't valid in condition with relationship: "
              + value
              + thisErrorMsg());
    }
    if (s.length == 4 && Integer.parseInt(s[3]) < -1) {
      throw new GameParseException(
          "numberOfRoundsExisting should be a number between -1 and 100000.  -1 should be "
              + "default value if you don't know what to put"
              + thisErrorMsg());
    }
    if (relationship == null) {
      relationship = new ArrayList<>();
    }
    String str = (s.length == 3) ? (value + ":-1") : value;
    relationship.add(str.intern());
  }

  private void setRelationship(final List<String> value) {
    relationship = value;
  }

  private List<String> getRelationship() {
    return getListProperty(relationship);
  }

  private void resetRelationship() {
    relationship = null;
  }

  private void setAlliedOwnershipTerritories(final String value) {
    alliedOwnershipTerritories = splitOnColon(value);
    validateNames(alliedOwnershipTerritories);
  }

  private void setAlliedOwnershipTerritories(final String[] value) {
    alliedOwnershipTerritories = value;
  }

  private String[] getAlliedOwnershipTerritories() {
    return alliedOwnershipTerritories;
  }

  private void resetAlliedOwnershipTerritories() {
    alliedOwnershipTerritories = null;
  }

  // exclusion types = controlled, controlledNoWater, original, all, or list
  private void setAlliedExclusionTerritories(final String value) {
    alliedExclusionTerritories = splitOnColon(value);
    validateNames(alliedExclusionTerritories);
  }

  private void setAlliedExclusionTerritories(final String[] value) {
    alliedExclusionTerritories = value;
  }

  private String[] getAlliedExclusionTerritories() {
    return alliedExclusionTerritories;
  }

  private void resetAlliedExclusionTerritories() {
    alliedExclusionTerritories = null;
  }

  private void setDirectExclusionTerritories(final String value) {
    directExclusionTerritories = splitOnColon(value);
    validateNames(directExclusionTerritories);
  }

  private void setDirectExclusionTerritories(final String[] value) {
    directExclusionTerritories = value;
  }

  private String[] getDirectExclusionTerritories() {
    return directExclusionTerritories;
  }

  private void resetDirectExclusionTerritories() {
    directExclusionTerritories = null;
  }

  // exclusion types = original or list
  private void setEnemyExclusionTerritories(final String value) {
    enemyExclusionTerritories = splitOnColon(value);
    validateNames(enemyExclusionTerritories);
  }

  private void setEnemyExclusionTerritories(final String[] value) {
    enemyExclusionTerritories = value;
  }

  private String[] getEnemyExclusionTerritories() {
    return enemyExclusionTerritories;
  }

  private void resetEnemyExclusionTerritories() {
    enemyExclusionTerritories = null;
  }

  private void setDirectPresenceTerritories(final String value) {
    directPresenceTerritories = splitOnColon(value);
    validateNames(directPresenceTerritories);
  }

  private void setDirectPresenceTerritories(final String[] value) {
    directPresenceTerritories = value;
  }

  private String[] getDirectPresenceTerritories() {
    return directPresenceTerritories;
  }

  private void resetDirectPresenceTerritories() {
    directPresenceTerritories = null;
  }

  private void setAlliedPresenceTerritories(final String value) {
    alliedPresenceTerritories = splitOnColon(value);
    validateNames(alliedPresenceTerritories);
  }

  private void setAlliedPresenceTerritories(final String[] value) {
    alliedPresenceTerritories = value;
  }

  private String[] getAlliedPresenceTerritories() {
    return alliedPresenceTerritories;
  }

  private void resetAlliedPresenceTerritories() {
    alliedPresenceTerritories = null;
  }

  private void setEnemyPresenceTerritories(final String value) {
    enemyPresenceTerritories = splitOnColon(value);
    validateNames(enemyPresenceTerritories);
  }

  private void setEnemyPresenceTerritories(final String[] value) {
    enemyPresenceTerritories = value;
  }

  private String[] getEnemyPresenceTerritories() {
    return enemyPresenceTerritories;
  }

  private void resetEnemyPresenceTerritories() {
    enemyPresenceTerritories = null;
  }

  // exclusion types = original or list
  private void setEnemySurfaceExclusionTerritories(final String value) {
    enemySurfaceExclusionTerritories = splitOnColon(value);
    validateNames(enemySurfaceExclusionTerritories);
  }

  private void setEnemySurfaceExclusionTerritories(final String[] value) {
    enemySurfaceExclusionTerritories = value;
  }

  private String[] getEnemySurfaceExclusionTerritories() {
    return enemySurfaceExclusionTerritories;
  }

  private void resetEnemySurfaceExclusionTerritories() {
    enemySurfaceExclusionTerritories = null;
  }

  private void setDirectOwnershipTerritories(final String value) {
    directOwnershipTerritories = splitOnColon(value);
    validateNames(directOwnershipTerritories);
  }

  private void setDirectOwnershipTerritories(final String[] value) {
    directOwnershipTerritories = value;
  }

  private String[] getDirectOwnershipTerritories() {
    return directOwnershipTerritories;
  }

  private void resetDirectOwnershipTerritories() {
    directOwnershipTerritories = null;
  }

  private void setUnitPresence(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length <= 1) {
      throw new GameParseException(
          "unitPresence must have at least 2 fields. Format value=unit1 count=number, or "
              + "value=unit1:unit2:unit3 count=number"
              + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 0) {
      throw new GameParseException("unitPresence must be a positive integer" + thisErrorMsg());
    }
    for (int i = 1; i < s.length; i++) {
      final String unitTypeToProduce = s[i];
      // validate that this unit exists in the xml
      final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
      if (ut == null && !(unitTypeToProduce.equals("any") || unitTypeToProduce.equals("ANY"))) {
        throw new GameParseException("No unit called: " + unitTypeToProduce + thisErrorMsg());
      }
    }
    if (unitPresence == null) {
      unitPresence = new IntegerMap<>();
    }
    unitPresence.put(value.substring(s[0].length() + 1).intern(), n);
  }

  private void setUnitPresence(final IntegerMap<String> value) {
    unitPresence = value;
  }

  private IntegerMap<String> getUnitPresence() {
    return getIntegerMapProperty(unitPresence);
  }

  private void resetUnitPresence() {
    unitPresence = null;
  }

  private void setIsAI(final String s) {
    isAI = (s == null) ? null : getBool(s);
  }

  private void setIsAI(final Boolean s) {
    isAI = s;
  }

  public Boolean getIsAI() {
    return isAI;
  }

  private void resetIsAI() {
    isAI = null;
  }

  private int getAtWarCount() {
    return atWarCount;
  }

  private int getTechCount() {
    return techCount;
  }

  private void setAtWarPlayers(final String players) throws GameParseException {
    final String[] s = splitOnColon(players);
    if (s.length < 1) {
      throw new GameParseException("Empty enemy list" + thisErrorMsg());
    }
    int count = -1;
    try {
      count = getInt(s[0]);
      atWarCount = count;
    } catch (final Exception e) {
      atWarCount = 0;
    }
    if (s.length == 1 && count != -1) {
      throw new GameParseException("Empty enemy list" + thisErrorMsg());
    }
    atWarPlayers = new HashSet<>();
    for (int i = count == -1 ? 0 : 1; i < s.length; i++) {
      atWarPlayers.add(getPlayerOrThrow(s[i]));
    }
  }

  private void setAtWarPlayers(final Set<GamePlayer> value) {
    atWarPlayers = value;
  }

  private Set<GamePlayer> getAtWarPlayers() {
    return getSetProperty(atWarPlayers);
  }

  private void resetAtWarPlayers() {
    atWarPlayers = null;
  }

  private void setTechs(final String newTechs) throws GameParseException {
    final String[] s = splitOnColon(newTechs);
    if (s.length < 1) {
      throw new GameParseException("Empty tech list" + thisErrorMsg());
    }
    int count = -1;
    try {
      count = getInt(s[0]);
      techCount = count;
    } catch (final Exception e) {
      techCount = 0;
    }
    if (s.length == 1 && count != -1) {
      throw new GameParseException("Empty tech list" + thisErrorMsg());
    }
    techs = new ArrayList<>();
    for (int i = count == -1 ? 0 : 1; i < s.length; i++) {
      TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
      if (ta == null) {
        ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
      }
      if (ta == null) {
        throw new GameParseException(
            "Technology not found :" + Arrays.toString(s) + thisErrorMsg());
      }
      techs.add(ta);
    }
  }

  private void setTechs(final List<TechAdvance> value) {
    techs = value;
  }

  private List<TechAdvance> getTechs() {
    return getListProperty(techs);
  }

  private void resetTechs() {
    techs = null;
  }

  @Override
  public boolean isSatisfied(final Map<ICondition, Boolean> testedConditions) {
    checkNotNull(testedConditions);
    checkState(testedConditions.containsKey(this));

    return testedConditions.get(this);
  }

  @Override
  public boolean isSatisfied(
      final Map<ICondition, Boolean> testedConditions, final IDelegateBridge delegateBridge) {
    if (testedConditions != null && testedConditions.containsKey(this)) {
      return testedConditions.get(this);
    }
    boolean objectiveMet = true;
    final List<GamePlayer> players = getPlayers();
    final GameState data = delegateBridge.getData();
    // check meta conditions (conditions which hold other conditions)
    if (!getConditions().isEmpty()) {
      final Map<ICondition, Boolean> actualTestedConditions =
          Optional.ofNullable(testedConditions)
              .orElseGet(
                  () ->
                      testAllConditionsRecursive(
                          getAllConditionsRecursive(new HashSet<>(conditions), null),
                          null,
                          delegateBridge));
      objectiveMet =
          areConditionsMet(new ArrayList<>(conditions), actualTestedConditions, conditionType);
    }
    // check switch (on/off)
    if (objectiveMet) {
      objectiveMet = switched;
    }
    // check turn limits
    if (objectiveMet && turns != null) {
      objectiveMet = checkRounds(data);
    }
    // check custom game property options
    if (objectiveMet && gameProperty != null) {
      objectiveMet = this.getGamePropertyState(data);
    }
    // Check for unit presence (Veqryn)
    if (objectiveMet && getDirectPresenceTerritories() != null) {
      objectiveMet =
          checkUnitPresence(
              getDirectPresenceTerritories(), directOwnership(players), players, data);
    }
    // Check for unit presence (Veqryn)
    if (objectiveMet && getAlliedPresenceTerritories() != null) {
      objectiveMet =
          checkUnitPresence(
              getAlliedPresenceTerritories(), alliedOwnership(players), players, data);
    }
    // Check for unit presence (Veqryn)
    if (objectiveMet && getEnemyPresenceTerritories() != null) {
      objectiveMet =
          checkUnitPresence(getEnemyPresenceTerritories(), enemyOwnership(players), players, data);
    }
    // Check for direct unit exclusions (veqryn)
    if (objectiveMet && getDirectExclusionTerritories() != null) {
      objectiveMet =
          checkUnitExclusions(
              getDirectExclusionTerritories(), directOwnership(players), players, data);
    }
    // Check for allied unit exclusions
    if (objectiveMet && getAlliedExclusionTerritories() != null) {
      objectiveMet =
          checkUnitExclusions(
              getAlliedExclusionTerritories(),
              directOwnership(players).negate().and(alliedOwnership(players)),
              players,
              data);
    }
    // Check for enemy unit exclusions (ANY UNITS)
    if (objectiveMet && getEnemyExclusionTerritories() != null) {
      objectiveMet =
          checkUnitExclusions(
              getEnemyExclusionTerritories(), enemyOwnership(players), players, data);
    }
    // Check for enemy unit exclusions (SURFACE UNITS with ATTACK POWER)
    if (objectiveMet && getEnemySurfaceExclusionTerritories() != null) {
      objectiveMet =
          checkUnitExclusions(
              getEnemySurfaceExclusionTerritories(), enemySurfaceOwnership(players), players, data);
    }
    // Check for Territory Ownership rules
    if (objectiveMet && getAlliedOwnershipTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getAlliedOwnershipTerritories();
      final Set<Territory> listedTerritories;
      if (terrs.length == 1) {
        switch (terrs[0]) {
          case "original":
            final Collection<GamePlayer> allies =
                CollectionUtils.getMatches(
                    data.getPlayerList().getPlayers(),
                    Matches.isAlliedWithAnyOfThesePlayers(players));
            listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, allies, data);
            break;
          case "enemy":
            final Collection<GamePlayer> enemies =
                CollectionUtils.getMatches(
                    data.getPlayerList().getPlayers(),
                    Matches.isAtWarWithAnyOfThesePlayers(players));
            listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
            break;
          default:
            listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
            break;
        }
      } else if (terrs.length == 2) {
        switch (terrs[1]) {
          case "original":
            final Collection<GamePlayer> allies =
                CollectionUtils.getMatches(
                    data.getPlayerList().getPlayers(),
                    Matches.isAlliedWithAnyOfThesePlayers(players));
            listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, allies, data);
            break;
          case "enemy":
            final Collection<GamePlayer> enemies =
                CollectionUtils.getMatches(
                    data.getPlayerList().getPlayers(),
                    Matches.isAtWarWithAnyOfThesePlayers(players));
            listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
            break;
          default:
            listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
            break;
        }
      } else {
        listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
      }
      objectiveMet = checkAlliedOwnership(listedTerritories, players, data);
    }
    // Check for Direct Territory Ownership rules
    if (objectiveMet && getDirectOwnershipTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getDirectOwnershipTerritories();
      final Set<Territory> listedTerritories;
      if (terrs.length == 1) {
        if ("enemy".equals(terrs[0])) {
          final Collection<GamePlayer> enemies =
              CollectionUtils.getMatches(
                  data.getPlayerList().getPlayers(), Matches.isAtWarWithAnyOfThesePlayers(players));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
        } else {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        }
      } else if (terrs.length == 2) {
        if ("enemy".equals(terrs[1])) {
          final Collection<GamePlayer> enemies =
              CollectionUtils.getMatches(
                  data.getPlayerList().getPlayers(), Matches.isAtWarWithAnyOfThesePlayers(players));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
        } else {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        }
      } else {
        listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
      }
      objectiveMet = checkDirectOwnership(listedTerritories, players);
    }
    // check for AI controlled player
    if (objectiveMet && getIsAI() != null) {
      objectiveMet = checkIsAI(players);
    }
    // get attached to player
    final GamePlayer playerAttachedTo = (GamePlayer) getAttachedTo();
    // check for players at war
    if (objectiveMet && !getAtWarPlayers().isEmpty()) {
      objectiveMet = checkAtWar(playerAttachedTo, getAtWarPlayers(), getAtWarCount());
    }
    // check for techs
    if (objectiveMet && !getTechs().isEmpty()) {
      objectiveMet = checkTechs(playerAttachedTo, data.getTechnologyFrontier());
    }
    // check for relationships
    if (objectiveMet && !getRelationship().isEmpty()) {
      objectiveMet = checkRelationships();
    }
    // check for battle stats
    if (objectiveMet && destroyedTuv != null) {
      final String[] s = splitOnColon(destroyedTuv);
      final int requiredDestroyedTuv = getInt(s[0]);
      if (requiredDestroyedTuv >= 0) {
        final boolean justCurrentRound = s[1].equals("currentRound");
        final int destroyedTuvForThisRoundSoFar =
            BattleRecordsList.getTuvDamageCausedByPlayer(
                playerAttachedTo,
                data.getBattleRecordsList(),
                0,
                data.getSequence().getRound(),
                justCurrentRound,
                false);
        if (requiredDestroyedTuv > destroyedTuvForThisRoundSoFar) {
          objectiveMet = false;
        }
        if (getCountEach()) {
          eachMultiple = destroyedTuvForThisRoundSoFar;
        }
      }
    }
    // check for battles
    if (objectiveMet && !getBattle().isEmpty()) {
      final BattleRecordsList brl = data.getBattleRecordsList();
      final int round = data.getSequence().getRound();
      for (final Tuple<String, List<Territory>> entry : battle) {
        final String[] type = splitOnColon(entry.getFirst());
        // they could be "any", and if they are "any" then this would be null, which is good!
        final GamePlayer attacker = data.getPlayerList().getPlayerId(type[0]);
        final GamePlayer defender = data.getPlayerList().getPlayerId(type[1]);
        final String resultType = type[2];
        final String roundType = type[3];
        int start = 0;
        int end = round;
        final boolean currentRound = roundType.equalsIgnoreCase("currentRound");
        if (!currentRound) {
          final String[] rounds = splitOnHyphen(roundType);
          start = getInt(rounds[0]);
          end = getInt(rounds[1]);
        }
        objectiveMet =
            BattleRecordsList.getWereThereBattlesInTerritoriesMatching(
                attacker, defender, resultType, entry.getSecond(), brl, start, end, currentRound);
        if (!objectiveMet) {
          break;
        }
      }
    }
    // "chance" should ALWAYS be checked last!
    final int hitTarget = getChanceToHit();
    final int diceSides = getChanceDiceSides();
    final int incrementOnFailure = this.getChanceIncrementOnFailure();
    final int decrementOnSuccess = this.getChanceDecrementOnSuccess();
    if (objectiveMet
        && (hitTarget != diceSides || incrementOnFailure != 0 || decrementOnSuccess != 0)) {
      if (diceSides <= 0 || hitTarget >= diceSides) {
        objectiveMet = true;
        changeChanceDecrementOrIncrementOnSuccessOrFailure(delegateBridge, objectiveMet, false);
      } else if (hitTarget <= 0) {
        objectiveMet = false;
        changeChanceDecrementOrIncrementOnSuccessOrFailure(delegateBridge, objectiveMet, false);
      } else {
        // there is an issue with maps using thousands of chance triggers: they are causing the
        // cypted random source (ie: live and pbem games) to lock up or error out so we need to slow
        // them down a bit, until we come up with a better solution (like aggregating all the
        // chances together, then getting a ton of random numbers at once instead of one at a time)
        if (PbemMessagePoster.gameDataHasPlayByEmailOrForumMessengers(data)) {
          Interruptibles.sleep(100);
        }
        final int rollResult =
            delegateBridge.getRandom(
                    diceSides,
                    null,
                    DiceType.ENGINE,
                    "Attempting the Condition: " + MyFormatter.attachmentNameToText(this.getName()))
                + 1;
        objectiveMet = rollResult <= hitTarget;
        final String notificationMessage =
            (objectiveMet ? TRIGGER_CHANCE_SUCCESSFUL : TRIGGER_CHANCE_FAILURE)
                + " (Rolled at "
                + hitTarget
                + " out of "
                + diceSides
                + " Result: "
                + rollResult
                + "  for "
                + MyFormatter.attachmentNameToText(this.getName())
                + ")";
        delegateBridge.getHistoryWriter().startEvent(notificationMessage);
        changeChanceDecrementOrIncrementOnSuccessOrFailure(delegateBridge, objectiveMet, true);
        delegateBridge
            .getRemotePlayer(delegateBridge.getGamePlayer())
            .reportMessage(notificationMessage, notificationMessage);
      }
    }
    return objectiveMet != invert;
  }

  /**
   * Checks if all relationship requirements are set.
   *
   * @return whether all relationships as are required are set correctly.
   */
  private boolean checkRelationships() {
    for (final String encodedRelationCheck : getRelationship()) {
      final String[] relationCheck = splitOnColon(encodedRelationCheck);
      final GamePlayer p1 = getData().getPlayerList().getPlayerId(relationCheck[0]);
      final GamePlayer p2 = getData().getPlayerList().getPlayerId(relationCheck[1]);
      final int relationshipsExistence = Integer.parseInt(relationCheck[3]);
      final Relationship currentRelationship =
          getData().getRelationshipTracker().getRelationship(p1, p2);
      final RelationshipType currentRelationshipType = currentRelationship.getRelationshipType();
      if (!relationshipExistsLongEnough(currentRelationship, relationshipsExistence)) {
        return false;
      }
      if (!((relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
              && Matches.relationshipTypeIsAllied().test(currentRelationshipType))
          || (relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
              && Matches.relationshipTypeIsNeutral().test(currentRelationshipType))
          || (relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
              && Matches.relationshipTypeIsAtWar().test(currentRelationshipType))
          || currentRelationshipType.equals(
              getData().getRelationshipTypeList().getRelationshipType(relationCheck[2])))) {
        return false;
      }
    }
    return true;
  }

  private boolean relationshipExistsLongEnough(
      final Relationship relationship, final int relationshipsExistence) {
    int roundCurrentRelationshipWasCreated = relationship.getRoundCreated();
    roundCurrentRelationshipWasCreated +=
        Properties.getRelationshipsLastExtraRounds(getData().getProperties());
    return getData().getSequence().getRound() - roundCurrentRelationshipWasCreated
        >= relationshipsExistence;
  }

  private Predicate<Unit> directOwnership(Collection<GamePlayer> players) {
    return Matches.unitIsOwnedByAnyOf(players);
  }

  private Predicate<Unit> alliedOwnership(Collection<GamePlayer> players) {
    return Matches.alliedUnitOfAnyOfThesePlayers(players);
  }

  private Predicate<Unit> enemyOwnership(Collection<GamePlayer> players) {
    return Matches.enemyUnitOfAnyOfThesePlayers(players);
  }

  private Predicate<Unit> enemySurfaceOwnership(Collection<GamePlayer> players) {
    return Matches.enemyUnitOfAnyOfThesePlayers(players)
        .and(Matches.unitIsSea())
        .and(Matches.unitCanEvade().negate())
        .and(Matches.unitIsNotSeaTransportButCouldBeCombatSeaTransport());
  }

  /**
   * Checks for the collection of territories to see if they have sufficient units matching the unit
   * filter.
   */
  private boolean checkUnitPresence(
      final String[] territoryStrings,
      final Predicate<Unit> unitFilter,
      final List<GamePlayer> players,
      final GameState data) {
    final Predicate<Territory> predicate =
        t -> {
          final Collection<Unit> units = t.getUnitCollection().getMatches(unitFilter);
          return !units.isEmpty()
              && checkUnitPresenceByType(t.getData(), units, false).orElse(true);
        };
    return matchTerritories(
        getTerritoryListBasedOnInputFromXml(territoryStrings, players, data), predicate);
  }

  /**
   * Checks for the collection of territories to see if they have sufficient units not matching the
   * unit filter.
   */
  private boolean checkUnitExclusions(
      final String[] territories,
      final Predicate<Unit> unitFilter,
      final List<GamePlayer> players,
      final GameState data) {
    final Predicate<Territory> predicate =
        t -> {
          final Collection<Unit> units = t.getUnitCollection().getMatches(unitFilter);
          return units.isEmpty() || checkUnitPresenceByType(t.getData(), units, true).orElse(false);
        };
    return matchTerritories(
        getTerritoryListBasedOnInputFromXml(territories, players, data), predicate);
  }

  private Optional<Boolean> checkUnitPresenceByType(
      GameState data, Collection<Unit> units, boolean lessThanOrEqual) {
    final IntegerMap<String> unitPresenceMap = getUnitPresence();
    if (unitPresenceMap != null && !unitPresenceMap.keySet().isEmpty()) {
      final Predicate<String> predicate =
          uc -> {
            final int unitsNeeded = unitPresenceMap.getInt(uc);
            final int unitsMatched =
                CollectionUtils.countMatches(units, getUnitTypesPredicate(data, uc));
            if (lessThanOrEqual) {
              return unitsMatched <= unitsNeeded;
            } else {
              return unitsMatched >= unitsNeeded;
            }
          };
      return Optional.of(unitPresenceMap.keySet().stream().allMatch(predicate));
    }
    return Optional.empty();
  }

  private Predicate<Unit> getUnitTypesPredicate(GameState data, String uc) {
    if (uc == null || uc.equals("ANY") || uc.equals("any")) {
      return unit -> true;
    }
    return Matches.unitIsOfTypes(data.getUnitTypeList().getUnitTypes(splitOnColon(uc)));
  }

  /**
   * Checks for allied ownership of the collection of territories. Once the needed number threshold
   * is reached, the satisfied flag is set to true and returned.
   */
  private boolean checkAlliedOwnership(
      final Collection<Territory> territories,
      final Collection<GamePlayer> players,
      final GameState data) {
    final Collection<GamePlayer> allies =
        CollectionUtils.getMatches(
            data.getPlayerList().getPlayers(), Matches.isAlliedWithAnyOfThesePlayers(players));
    return matchTerritories(territories, Matches.isTerritoryOwnedByAnyOf(allies));
  }

  /**
   * Checks for direct ownership of the collection of territories. Once the needed number threshold
   * is reached, return true.
   */
  private boolean checkDirectOwnership(
      final Collection<Territory> territories, final Collection<GamePlayer> players) {
    return matchTerritories(territories, Matches.isTerritoryOwnedByAnyOf(players));
  }

  private boolean matchTerritories(
      final Collection<Territory> territories, final Predicate<Territory> predicate) {
    final int numberMet = CollectionUtils.countMatches(territories, predicate);
    if (getCountEach()) {
      eachMultiple = numberMet;
    }
    return numberMet >= getTerritoryCount();
  }

  private boolean checkIsAI(final List<GamePlayer> players) {
    boolean bcheck = true;
    for (GamePlayer player : players) {
      bcheck = (bcheck && (getIsAI() == player.isAi()));
    }
    return bcheck;
  }

  private boolean checkAtWar(
      final GamePlayer player, final Set<GamePlayer> enemies, final int count) {
    int found = CollectionUtils.countMatches(enemies, player::isAtWar);
    if (count == 0) {
      return found == 0;
    }
    if (getCountEach()) {
      eachMultiple = found;
    }
    return found >= count;
  }

  private boolean checkTechs(final GamePlayer player, final TechnologyFrontier technologyFrontier) {
    int found = 0;
    if (techs != null) {
      for (final TechAdvance a : TechTracker.getCurrentTechAdvances(player, technologyFrontier)) {
        if (techs.contains(a)) {
          found++;
        }
      }
    }
    if (techCount == 0) {
      return techCount == found;
    }
    if (getCountEach()) {
      eachMultiple = found;
    }
    return found >= techCount;
  }

  @Override
  public void validate(final GameState data) {
    validateNames(alliedOwnershipTerritories);
    validateNames(enemyExclusionTerritories);
    validateNames(enemySurfaceExclusionTerritories);
    validateNames(alliedExclusionTerritories);
    validateNames(directExclusionTerritories);
    validateNames(directOwnershipTerritories);
    validateNames(directPresenceTerritories);
    validateNames(alliedPresenceTerritories);
    validateNames(enemyPresenceTerritories);
  }

  @Override
  public MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "techs":
        return MutableProperty.of(this::setTechs, this::setTechs, this::getTechs, this::resetTechs);
      case "techCount":
        return MutableProperty.ofReadOnly(this::getTechCount);
      case "relationship":
        return MutableProperty.of(
            this::setRelationship,
            this::setRelationship,
            this::getRelationship,
            this::resetRelationship);
      case "isAI":
        return MutableProperty.of(this::setIsAI, this::setIsAI, this::getIsAI, this::resetIsAI);
      case "atWarPlayers":
        return MutableProperty.of(
            this::setAtWarPlayers,
            this::setAtWarPlayers,
            this::getAtWarPlayers,
            this::resetAtWarPlayers);
      case "atWarCount":
        return MutableProperty.ofReadOnly(this::getAtWarCount);
      case "destroyedTUV":
        return MutableProperty.ofString(
            this::setDestroyedTuv, this::getDestroyedTuv, this::resetDestroyedTuv);
      case "battle":
        return MutableProperty.of(
            this::setBattle, this::setBattle, this::getBattle, this::resetBattle);
      case "alliedOwnershipTerritories":
        return MutableProperty.of(
            this::setAlliedOwnershipTerritories,
            this::setAlliedOwnershipTerritories,
            this::getAlliedOwnershipTerritories,
            this::resetAlliedOwnershipTerritories);
      case "directOwnershipTerritories":
        return MutableProperty.of(
            this::setDirectOwnershipTerritories,
            this::setDirectOwnershipTerritories,
            this::getDirectOwnershipTerritories,
            this::resetDirectOwnershipTerritories);
      case "alliedExclusionTerritories":
        return MutableProperty.of(
            this::setAlliedExclusionTerritories,
            this::setAlliedExclusionTerritories,
            this::getAlliedExclusionTerritories,
            this::resetAlliedExclusionTerritories);
      case "directExclusionTerritories":
        return MutableProperty.of(
            this::setDirectExclusionTerritories,
            this::setDirectExclusionTerritories,
            this::getDirectExclusionTerritories,
            this::resetDirectExclusionTerritories);
      case "enemyExclusionTerritories":
        return MutableProperty.of(
            this::setEnemyExclusionTerritories,
            this::setEnemyExclusionTerritories,
            this::getEnemyExclusionTerritories,
            this::resetEnemyExclusionTerritories);
      case "enemySurfaceExclusionTerritories":
        return MutableProperty.of(
            this::setEnemySurfaceExclusionTerritories,
            this::setEnemySurfaceExclusionTerritories,
            this::getEnemySurfaceExclusionTerritories,
            this::resetEnemySurfaceExclusionTerritories);
      case "directPresenceTerritories":
        return MutableProperty.of(
            this::setDirectPresenceTerritories,
            this::setDirectPresenceTerritories,
            this::getDirectPresenceTerritories,
            this::resetDirectPresenceTerritories);
      case "alliedPresenceTerritories":
        return MutableProperty.of(
            this::setAlliedPresenceTerritories,
            this::setAlliedPresenceTerritories,
            this::getAlliedPresenceTerritories,
            this::resetAlliedPresenceTerritories);
      case "enemyPresenceTerritories":
        return MutableProperty.of(
            this::setEnemyPresenceTerritories,
            this::setEnemyPresenceTerritories,
            this::getEnemyPresenceTerritories,
            this::resetEnemyPresenceTerritories);
      case "unitPresence":
        return MutableProperty.of(
            this::setUnitPresence,
            this::setUnitPresence,
            this::getUnitPresence,
            this::resetUnitPresence);
      default:
        return super.getPropertyOrNull(propertyName);
    }
  }
}
