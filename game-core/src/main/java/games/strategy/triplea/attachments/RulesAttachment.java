package games.strategy.triplea.attachments;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.BattleRecordsList;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Interruptibles;
import games.strategy.util.Tuple;

@MapSupport
public class RulesAttachment extends AbstractPlayerRulesAttachment {
  private static final long serialVersionUID = 7301965634079412516L;

  // condition for having techs
  private List<TechAdvance> m_techs = null;
  @InternalDoNotExport
  // Do Not Export (do not include in IAttachment).
  private int m_techCount = -1;
  // condition for having specific relationships
  private List<String> m_relationship = new ArrayList<>();
  // condition for being at war
  private Set<PlayerID> m_atWarPlayers = null;
  @InternalDoNotExport
  // Do Not Export (do not include in IAttachment).
  private int m_atWarCount = -1;
  // condition for having destroyed at least X enemy non-neutral TUV (total unit value) [according to
  // the prices the defender pays for the units]
  private String m_destroyedTUV = null;
  // condition for having had a battle in some territory, attacker or defender, win
  // or lost, etc. these next 9 variables use m_territoryCount for determining the number needed.
  private List<Tuple<String, List<Territory>>> m_battle = new ArrayList<>();
  // ownership related
  private String[] m_alliedOwnershipTerritories = null;
  private String[] m_directOwnershipTerritories = null;
  // exclusion of units
  private String[] m_alliedExclusionTerritories = null;
  private String[] m_directExclusionTerritories = null;
  private String[] m_enemyExclusionTerritories = null;
  private String[] m_enemySurfaceExclusionTerritories = null;
  // presence of units
  private String[] m_directPresenceTerritories = null;
  private String[] m_alliedPresenceTerritories = null;
  private String[] m_enemyPresenceTerritories = null;
  // used with above 3 to determine the type of unit that must be present
  private IntegerMap<String> m_unitPresence = new IntegerMap<>();


  /** Creates new RulesAttachment. */
  public RulesAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Convenience method, for use with rules attachments, objectives, and condition attachments. Should return
   * RulesAttachments.
   *
   * @param player PlayerID
   * @param nameOfAttachment exact full name of attachment
   * @return new rule attachment
   */
  public static RulesAttachment get(final PlayerID player, final String nameOfAttachment) {
    return get(player, nameOfAttachment, null, false);
  }

  public static RulesAttachment get(final PlayerID player, final String nameOfAttachment,
      final Collection<PlayerID> playersToSearch, final boolean allowNull) {
    RulesAttachment ra = (RulesAttachment) player.getAttachment(nameOfAttachment);
    if (ra == null) {
      if (playersToSearch == null) {
        if (!allowNull) {
          throw new IllegalStateException(
              "Rules & Conditions: No rule attachment for:" + player.getName() + " with name: " + nameOfAttachment);
        }
      } else {
        for (final PlayerID otherPlayer : playersToSearch) {
          if (otherPlayer == player) {
            continue;
          }
          ra = (RulesAttachment) otherPlayer.getAttachment(nameOfAttachment);
          if (ra != null) {
            return ra;
          }
        }
        if (!allowNull) {
          throw new IllegalStateException(
              "Rules & Conditions: No rule attachment for:" + player.getName() + " with name: " + nameOfAttachment);
        }
      }
    }
    return ra;
  }

  /**
   * Convenience method, for use returning any RulesAttachment that begins with "objectiveAttachment"
   * National Objectives are just conditions that also give money to a player during the end turn delegate. They can be
   * used for testing by
   * triggers as well.
   * Conditions that do not give money are not prefixed with "objectiveAttachment",
   * and the trigger attachment that uses these kinds of conditions gets them a different way because they are
   * specifically named inside
   * that trigger.
   */
  public static Set<RulesAttachment> getNationalObjectives(final PlayerID player) {
    final Set<RulesAttachment> natObjs = new HashSet<>();
    final Map<String, IAttachment> map = player.getAttachments();
    for (final Map.Entry<String, IAttachment> entry : map.entrySet()) {
      final IAttachment attachment = entry.getValue();
      if (attachment instanceof RulesAttachment) {
        if (attachment.getName().startsWith(Constants.RULES_OBJECTIVE_PREFIX)) {
          natObjs.add((RulesAttachment) attachment);
        }
      }
    }
    return natObjs;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDestroyedTUV(final String value) throws GameParseException {
    if (value == null) {
      m_destroyedTUV = null;
      return;
    }
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException("destroyedTUV must have 2 fields, value=currentRound/allRounds, count= the amount "
          + "of TUV that this player must destroy" + thisErrorMsg());
    }
    final int i = getInt(s[0]);
    if (i < -1) {
      throw new GameParseException(
          "destroyedTUV count cannot be less than -1 [with -1 meaning the condition is not active]" + thisErrorMsg());
    }
    if (!(s[1].equals("currentRound") || s[1].equals("allRounds"))) {
      throw new GameParseException("destroyedTUV value must be currentRound or allRounds" + thisErrorMsg());
    }
    m_destroyedTUV = value;
  }

  public String getDestroyedTUV() {
    return m_destroyedTUV;
  }

  public void resetDestroyedTUV() {
    m_destroyedTUV = null;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setBattle(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length < 5) {
      throw new GameParseException(
          "battle must have at least 5 fields, attacker:defender:resultType:round:territory1..." + thisErrorMsg());
    }
    final PlayerID attacker = getData().getPlayerList().getPlayerId(s[0]);
    if (attacker == null && !s[0].equalsIgnoreCase("any")) {
      throw new GameParseException("no player named: " + s[0] + thisErrorMsg());
    }
    final PlayerID defender = getData().getPlayerList().getPlayerId(s[1]);
    if (defender == null && !s[1].equalsIgnoreCase("any")) {
      throw new GameParseException("no player named: " + s[1] + thisErrorMsg());
    }
    if (!s[2].equalsIgnoreCase("any")) {
      throw new GameParseException("battle allows the following for resultType: any" + thisErrorMsg());
    }
    if (!s[3].equalsIgnoreCase("currentRound")) {
      try {
        getInt(s[3].split("-")[0]);
        getInt(s[3].split("-")[1]);
      } catch (final Exception e) {
        throw new GameParseException("round must either be currentRound or two numbers like: 2-4" + thisErrorMsg());
      }
    }
    final ArrayList<Territory> terrs = new ArrayList<>();
    final GameMap map = getData().getMap();
    // this loop starts on 4, so do not replace with an enhanced for loop
    for (int i = 4; i < s.length; i++) {
      final Territory t = map.getTerritory(s[i]);
      if (t == null) {
        throw new GameParseException("no such territory called: " + s[i] + thisErrorMsg());
      }
      terrs.add(t);
    }
    m_battle.add(Tuple.of((s[0] + ":" + s[1] + ":" + s[2] + ":" + s[3]), terrs));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBattle(final List<Tuple<String, List<Territory>>> value) {
    m_battle = value;
  }

  public List<Tuple<String, List<Territory>>> getBattle() {
    return m_battle;
  }

  public void clearBattle() {
    m_battle.clear();
  }

  public void resetBattle() {
    m_battle = new ArrayList<>();
  }

  /**
   * Condition to check if a certain relationship exists between 2 players.
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value should be a string containing: "player:player:relationship"
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setRelationship(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length < 3 || s.length > 4) {
      throw new GameParseException(
          "relationship should have value=\"playername1:playername2:relationshiptype:numberOfRoundsExisting\""
              + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[0]) == null) {
      throw new GameParseException(
          "playername: " + s[0] + " isn't valid in condition with relationship: " + value + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[1]) == null) {
      throw new GameParseException(
          "playername: " + s[1] + " isn't valid in condition with relationship: " + value + thisErrorMsg());
    }
    if (!(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
        || Matches.isValidRelationshipName(getData()).test(s[2]))) {
      throw new GameParseException(
          "relationship: " + s[2] + " isn't valid in condition with relationship: " + value + thisErrorMsg());
    }
    if (s.length == 4 && Integer.parseInt(s[3]) < -1) {
      throw new GameParseException("numberOfRoundsExisting should be a number between -1 and 100000.  -1 should be "
          + "default value if you don't know what to put" + thisErrorMsg());
    }
    m_relationship.add((s.length == 3) ? (value + ":-1") : value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRelationship(final List<String> value) {
    m_relationship = value;
  }

  public List<String> getRelationship() {
    return m_relationship;
  }

  public void clearRelationship() {
    m_relationship.clear();
  }

  public void resetRelationship() {
    m_relationship = new ArrayList<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlliedOwnershipTerritories(final String value) {
    if (value == null) {
      m_alliedOwnershipTerritories = null;
      return;
    }
    m_alliedOwnershipTerritories = value.split(":");
    validateNames(m_alliedOwnershipTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlliedOwnershipTerritories(final String[] value) {
    m_alliedOwnershipTerritories = value;
  }

  public String[] getAlliedOwnershipTerritories() {
    return m_alliedOwnershipTerritories;
  }

  public void resetAlliedOwnershipTerritories() {
    m_alliedOwnershipTerritories = null;
  }

  // exclusion types = controlled, controlledNoWater, original, all, or list
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlliedExclusionTerritories(final String value) {
    if (value == null) {
      m_alliedExclusionTerritories = null;
      return;
    }
    m_alliedExclusionTerritories = value.split(":");
    validateNames(m_alliedExclusionTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlliedExclusionTerritories(final String[] value) {
    m_alliedExclusionTerritories = value;
  }

  public String[] getAlliedExclusionTerritories() {
    return m_alliedExclusionTerritories;
  }

  public void resetAlliedExclusionTerritories() {
    m_alliedExclusionTerritories = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDirectExclusionTerritories(final String value) {
    if (value == null) {
      m_directExclusionTerritories = null;
      return;
    }
    m_directExclusionTerritories = value.split(":");
    validateNames(m_directExclusionTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDirectExclusionTerritories(final String[] value) {
    m_directExclusionTerritories = value;
  }

  public String[] getDirectExclusionTerritories() {
    return m_directExclusionTerritories;
  }

  public void resetDirectExclusionTerritories() {
    m_directExclusionTerritories = null;
  }

  // exclusion types = original or list
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setEnemyExclusionTerritories(final String value) {
    if (value == null) {
      m_enemyExclusionTerritories = null;
      return;
    }
    m_enemyExclusionTerritories = value.split(":");
    validateNames(m_enemyExclusionTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setEnemyExclusionTerritories(final String[] value) {
    m_enemyExclusionTerritories = value;
  }

  public String[] getEnemyExclusionTerritories() {
    return m_enemyExclusionTerritories;
  }

  public void resetEnemyExclusionTerritories() {
    m_enemyExclusionTerritories = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDirectPresenceTerritories(final String value) {
    if (value == null) {
      m_directPresenceTerritories = null;
      return;
    }
    m_directPresenceTerritories = value.split(":");
    validateNames(m_directPresenceTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDirectPresenceTerritories(final String[] value) {
    m_directPresenceTerritories = value;
  }

  public String[] getDirectPresenceTerritories() {
    return m_directPresenceTerritories;
  }

  public void resetDirectPresenceTerritories() {
    m_directPresenceTerritories = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlliedPresenceTerritories(final String value) {
    if (value == null) {
      m_alliedPresenceTerritories = null;
      return;
    }
    m_alliedPresenceTerritories = value.split(":");
    validateNames(m_alliedPresenceTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlliedPresenceTerritories(final String[] value) {
    m_alliedPresenceTerritories = value;
  }

  public String[] getAlliedPresenceTerritories() {
    return m_alliedPresenceTerritories;
  }

  public void resetAlliedPresenceTerritories() {
    m_alliedPresenceTerritories = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setEnemyPresenceTerritories(final String value) {
    if (value == null) {
      m_enemyPresenceTerritories = null;
      return;
    }
    m_enemyPresenceTerritories = value.split(":");
    validateNames(m_enemyPresenceTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setEnemyPresenceTerritories(final String[] value) {
    m_enemyPresenceTerritories = value;
  }

  public String[] getEnemyPresenceTerritories() {
    return m_enemyPresenceTerritories;
  }

  public void resetEnemyPresenceTerritories() {
    m_enemyPresenceTerritories = null;
  }

  // exclusion types = original or list
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setEnemySurfaceExclusionTerritories(final String value) {
    if (value == null) {
      m_enemySurfaceExclusionTerritories = null;
      return;
    }
    m_enemySurfaceExclusionTerritories = value.split(":");
    validateNames(m_enemySurfaceExclusionTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setEnemySurfaceExclusionTerritories(final String[] value) {
    m_enemySurfaceExclusionTerritories = value;
  }

  public String[] getEnemySurfaceExclusionTerritories() {
    return m_enemySurfaceExclusionTerritories;
  }

  public void resetEnemySurfaceExclusionTerritories() {
    m_enemySurfaceExclusionTerritories = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDirectOwnershipTerritories(final String value) {
    if (value == null) {
      m_directOwnershipTerritories = null;
      return;
    }
    m_directOwnershipTerritories = value.split(":");
    validateNames(m_directOwnershipTerritories);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDirectOwnershipTerritories(final String[] value) {
    m_directOwnershipTerritories = value;
  }

  public String[] getDirectOwnershipTerritories() {
    return m_directOwnershipTerritories;
  }

  public void resetDirectOwnershipTerritories() {
    m_directOwnershipTerritories = null;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setUnitPresence(String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length <= 1) {
      throw new GameParseException("unitPresence must have at least 2 fields. Format value=unit1 count=number, or "
          + "value=unit1:unit2:unit3 count=number" + thisErrorMsg());
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
    value = value.replaceFirst(s[0] + ":", "");
    m_unitPresence.put(value, n);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitPresence(final IntegerMap<String> value) {
    m_unitPresence = value;
  }

  public IntegerMap<String> getUnitPresence() {
    return m_unitPresence;
  }

  public void clearUnitPresence() {
    m_unitPresence.clear();
  }

  public void resetUnitPresence() {
    m_unitPresence = new IntegerMap<>();
  }

  public int getAtWarCount() {
    return m_atWarCount;
  }

  public int getTechCount() {
    return m_techCount;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAtWarPlayers(final String players) throws GameParseException {
    if (players == null) {
      m_atWarPlayers = null;
      return;
    }
    final String[] s = players.split(":");
    if (s.length < 1) {
      throw new GameParseException("Empty enemy list" + thisErrorMsg());
    }
    int count = -1;
    try {
      count = getInt(s[0]);
      m_atWarCount = count;
    } catch (final Exception e) {
      m_atWarCount = 0;
    }
    if (s.length < 1 || (s.length == 1 && count != -1)) {
      throw new GameParseException("Empty enemy list" + thisErrorMsg());
    }
    m_atWarPlayers = new HashSet<>();
    for (int i = count == -1 ? 0 : 1; i < s.length; i++) {
      final PlayerID player = getData().getPlayerList().getPlayerId(s[i]);
      if (player == null) {
        throw new GameParseException("Could not find player. name:" + s[i] + thisErrorMsg());
      }
      m_atWarPlayers.add(player);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAtWarPlayers(final Set<PlayerID> value) {
    m_atWarPlayers = value;
  }

  public Set<PlayerID> getAtWarPlayers() {
    return m_atWarPlayers;
  }

  public void resetAtWarPlayers() {
    m_atWarPlayers = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTechs(final String newTechs) throws GameParseException {
    if (newTechs == null) {
      m_techs = null;
      return;
    }
    final String[] s = newTechs.split(":");
    if (s.length < 1) {
      throw new GameParseException("Empty tech list" + thisErrorMsg());
    }
    int count = -1;
    try {
      count = getInt(s[0]);
      m_techCount = count;
    } catch (final Exception e) {
      m_techCount = 0;
    }
    if (s.length < 1 || (s.length == 1 && count != -1)) {
      throw new GameParseException("Empty tech list" + thisErrorMsg());
    }
    m_techs = new ArrayList<>();
    for (int i = count == -1 ? 0 : 1; i < s.length; i++) {
      TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
      if (ta == null) {
        ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
      }
      if (ta == null) {
        throw new GameParseException("Technology not found :" + Arrays.toString(s) + thisErrorMsg());
      }
      m_techs.add(ta);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTechs(final List<TechAdvance> value) {
    m_techs = value;
  }

  public List<TechAdvance> getTechs() {
    return m_techs;
  }

  public void resetTechs() {
    m_techs = null;
  }

  @Override
  public boolean isSatisfied(final HashMap<ICondition, Boolean> testedConditions) {
    checkNotNull(testedConditions);
    checkState(testedConditions.containsKey(this));

    return testedConditions.get(this);
  }

  @Override
  public boolean isSatisfied(HashMap<ICondition, Boolean> testedConditions, final IDelegateBridge delegateBridge) {
    if (testedConditions != null) {
      if (testedConditions.containsKey(this)) {
        return testedConditions.get(this);
      }
    }
    boolean objectiveMet = true;
    final List<PlayerID> players = getPlayers();
    final GameData data = delegateBridge.getData();
    // check meta conditions (conditions which hold other conditions)
    if (objectiveMet && m_conditions.size() > 0) {
      if (testedConditions == null) {
        testedConditions = testAllConditionsRecursive(
            getAllConditionsRecursive(new HashSet<>(m_conditions), null), null, delegateBridge);
      }
      objectiveMet = areConditionsMet(new ArrayList<>(m_conditions), testedConditions, m_conditionType);
    }
    // check switch (on/off)
    if (objectiveMet) {
      objectiveMet = m_switch;
    }
    // check turn limits
    if (objectiveMet && m_turns != null) {
      objectiveMet = checkTurns(data);
    }
    // check custom game property options
    if (objectiveMet && m_gameProperty != null) {
      objectiveMet = this.getGamePropertyState(data);
    }
    // Check for unit presence (Veqryn)
    if (objectiveMet && getDirectPresenceTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getDirectPresenceTerritories();
      objectiveMet = checkUnitPresence(getTerritoryListBasedOnInputFromXml(terrs, players, data),
          "direct", getTerritoryCount(), players, data);
    }
    // Check for unit presence (Veqryn)
    if (objectiveMet && getAlliedPresenceTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getAlliedPresenceTerritories();
      objectiveMet = checkUnitPresence(getTerritoryListBasedOnInputFromXml(terrs, players, data),
          "allied", getTerritoryCount(), players, data);
    }
    // Check for unit presence (Veqryn)
    if (objectiveMet && getEnemyPresenceTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getEnemyPresenceTerritories();
      objectiveMet = checkUnitPresence(getTerritoryListBasedOnInputFromXml(terrs, players, data), "enemy",
          getTerritoryCount(), players, data);
    }
    // Check for direct unit exclusions (veqryn)
    if (objectiveMet && getDirectExclusionTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getDirectExclusionTerritories();
      objectiveMet = checkUnitExclusions(getTerritoryListBasedOnInputFromXml(terrs, players, data),
          "direct", getTerritoryCount(), players, data);
    }
    // Check for allied unit exclusions
    if (objectiveMet && getAlliedExclusionTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getAlliedExclusionTerritories();
      objectiveMet = checkUnitExclusions(getTerritoryListBasedOnInputFromXml(terrs, players, data),
          "allied", getTerritoryCount(), players, data);
    }
    // Check for enemy unit exclusions (ANY UNITS)
    if (objectiveMet && getEnemyExclusionTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getEnemyExclusionTerritories();
      objectiveMet = checkUnitExclusions(getTerritoryListBasedOnInputFromXml(terrs, players, data),
          "enemy", getTerritoryCount(), players, data);
    }
    // Check for enemy unit exclusions (SURFACE UNITS with ATTACK POWER)
    if (objectiveMet && getEnemySurfaceExclusionTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getEnemySurfaceExclusionTerritories();
      objectiveMet = checkUnitExclusions(getTerritoryListBasedOnInputFromXml(terrs, players, data),
          "enemy_surface", getTerritoryCount(), players, data);
    }
    // Check for Territory Ownership rules
    if (objectiveMet && getAlliedOwnershipTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getAlliedOwnershipTerritories();
      final Set<Territory> listedTerritories;
      if (terrs.length == 1) {
        if (terrs[0].equals("original")) {
          final Collection<PlayerID> allies =
              CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
                  Matches.isAlliedWithAnyOfThesePlayers(players, data));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, allies, data);
        } else if (terrs[0].equals("enemy")) {
          final Collection<PlayerID> enemies =
              CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
                  Matches.isAtWarWithAnyOfThesePlayers(players, data));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
        } else {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        }
      } else if (terrs.length == 2) {
        if (terrs[1].equals("original")) {
          final Collection<PlayerID> allies =
              CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
                  Matches.isAlliedWithAnyOfThesePlayers(players, data));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, allies, data);
        } else if (terrs[1].equals("enemy")) {
          final Collection<PlayerID> enemies =
              CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
                  Matches.isAtWarWithAnyOfThesePlayers(players, data));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
        } else {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        }
      } else {
        listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
      }
      objectiveMet = checkAlliedOwnership(listedTerritories, getTerritoryCount(), players, data);
    }
    // Check for Direct Territory Ownership rules
    if (objectiveMet && getDirectOwnershipTerritories() != null) {
      // Get the listed territories
      final String[] terrs = getDirectOwnershipTerritories();
      final Set<Territory> listedTerritories;
      if (terrs.length == 1) {
        if (terrs[0].equals("original")) {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        } else if (terrs[0].equals("enemy")) {
          final Collection<PlayerID> enemies =
              CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
                  Matches.isAtWarWithAnyOfThesePlayers(players, data));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
        } else {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        }
      } else if (terrs.length == 2) {
        if (terrs[1].equals("original")) {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        } else if (terrs[1].equals("enemy")) {
          final Collection<PlayerID> enemies =
              CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
                  Matches.isAtWarWithAnyOfThesePlayers(players, data));
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, enemies, data);
        } else {
          listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
        }
      } else {
        listedTerritories = getTerritoryListBasedOnInputFromXml(terrs, players, data);
      }
      objectiveMet = checkDirectOwnership(listedTerritories, getTerritoryCount(), players);
    }
    // get attached to player
    final PlayerID playerAttachedTo = (PlayerID) getAttachedTo();
    if (objectiveMet && getAtWarPlayers() != null) {
      objectiveMet = checkAtWar(playerAttachedTo, getAtWarPlayers(), getAtWarCount(), data);
    }
    if (objectiveMet && m_techs != null) {
      objectiveMet = checkTechs(playerAttachedTo, data);
    }
    // check for relationships
    if (objectiveMet && m_relationship.size() > 0) {
      objectiveMet = checkRelationships();
    }
    // check for battle stats
    if (objectiveMet && m_destroyedTUV != null) {
      final String[] s = m_destroyedTUV.split(":");
      final int requiredDestroyedTuv = getInt(s[0]);
      if (requiredDestroyedTuv >= 0) {
        final boolean justCurrentRound = s[1].equals("currentRound");
        final int destroyedTuvForThisRoundSoFar = BattleRecordsList.getTuvDamageCausedByPlayer(playerAttachedTo,
            data.getBattleRecordsList(), 0, data.getSequence().getRound(), justCurrentRound, false);
        if (requiredDestroyedTuv > destroyedTuvForThisRoundSoFar) {
          objectiveMet = false;
        }
        if (getCountEach()) {
          m_eachMultiple = destroyedTuvForThisRoundSoFar;
        }
      }
    }
    // check for battles
    if (objectiveMet && !m_battle.isEmpty()) {
      final BattleRecordsList brl = data.getBattleRecordsList();
      final int round = data.getSequence().getRound();
      for (final Tuple<String, List<Territory>> entry : m_battle) {
        final String[] type = entry.getFirst().split(":");
        // they could be "any", and if they are "any" then this would be null, which is good!
        final PlayerID attacker = data.getPlayerList().getPlayerId(type[0]);
        final PlayerID defender = data.getPlayerList().getPlayerId(type[1]);
        final String resultType = type[2];
        final String roundType = type[3];
        int start = 0;
        int end = round;
        final boolean currentRound = roundType.equalsIgnoreCase("currentRound");
        if (!currentRound) {
          final String[] rounds = roundType.split("-");
          start = getInt(rounds[0]);
          end = getInt(rounds[1]);
        }
        objectiveMet = BattleRecordsList.getWereThereBattlesInTerritoriesMatching(attacker, defender, resultType,
            entry.getSecond(), brl, start, end, currentRound);
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
    if (objectiveMet && (hitTarget != diceSides || incrementOnFailure != 0 || decrementOnSuccess != 0)) {
      if (diceSides <= 0 || hitTarget >= diceSides) {
        objectiveMet = true;
        changeChanceDecrementOrIncrementOnSuccessOrFailure(delegateBridge, objectiveMet, false);
      } else if (hitTarget <= 0) {
        objectiveMet = false;
        changeChanceDecrementOrIncrementOnSuccessOrFailure(delegateBridge, objectiveMet, false);
      } else {
        // there is an issue with maps using thousands of chance triggers: they are causing the cypted random source
        // (ie: live and pbem
        // games) to lock up or error out
        // so we need to slow them down a bit, until we come up with a better solution (like aggregating all the chances
        // together, then
        // getting a ton of random numbers at once instead of one at a time)
        Interruptibles.sleep(100);
        final int rollResult = delegateBridge.getRandom(diceSides, null, DiceType.ENGINE,
            "Attempting the Condition: " + MyFormatter.attachmentNameToText(this.getName())) + 1;
        objectiveMet = rollResult <= hitTarget;
        final String notificationMessage = (objectiveMet ? TRIGGER_CHANCE_SUCCESSFUL : TRIGGER_CHANCE_FAILURE)
            + " (Rolled at " + hitTarget + " out of " + diceSides + " Result: " + rollResult + "  for "
            + MyFormatter.attachmentNameToText(this.getName()) + ")";
        delegateBridge.getHistoryWriter().startEvent(notificationMessage);
        changeChanceDecrementOrIncrementOnSuccessOrFailure(delegateBridge, objectiveMet, true);
        ((ITripleAPlayer) delegateBridge.getRemotePlayer(delegateBridge.getPlayerId()))
            .reportMessage(notificationMessage, notificationMessage);
      }
    }
    return objectiveMet != m_invert;
  }

  /**
   * checks if all relationship requirements are set
   *
   * @return whether all relationships as are required are set correctly.
   */
  private boolean checkRelationships() {
    for (final String encodedRelationCheck : m_relationship) {
      final String[] relationCheck = encodedRelationCheck.split(":");
      final PlayerID p1 = getData().getPlayerList().getPlayerId(relationCheck[0]);
      final PlayerID p2 = getData().getPlayerList().getPlayerId(relationCheck[1]);
      final int relationshipsExistance = Integer.parseInt(relationCheck[3]);
      final Relationship currentRelationship = getData().getRelationshipTracker().getRelationship(p1, p2);
      final RelationshipType currentRelationshipType = currentRelationship.getRelationshipType();
      if (!relationShipExistsLongEnnough(currentRelationship, relationshipsExistance)) {
        return false;
      }
      if (!((relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
          && Matches.relationshipTypeIsAllied().test(currentRelationshipType))
          || (relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
              && Matches.relationshipTypeIsNeutral().test(currentRelationshipType))
          || (relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
              && Matches.relationshipTypeIsAtWar().test(currentRelationshipType))
          || currentRelationshipType
              .equals(getData().getRelationshipTypeList().getRelationshipType(relationCheck[2])))) {
        return false;
      }
    }
    return true;
  }

  private boolean relationShipExistsLongEnnough(final Relationship r, final int relationshipsExistance) {
    int roundCurrentRelationshipWasCreated = r.getRoundCreated();
    roundCurrentRelationshipWasCreated += Properties.getRelationshipsLastExtraRounds(getData());
    return getData().getSequence().getRound() - roundCurrentRelationshipWasCreated >= relationshipsExistance;
  }

  /**
   * Checks for the collection of territories to see if they have units owned by the exclType alliance.
   */
  private boolean checkUnitPresence(final Collection<Territory> territories, final String exclType,
      final int numberNeeded, final List<PlayerID> players, final GameData data) {
    boolean useSpecific = false;
    if (getUnitPresence() != null && !getUnitPresence().keySet().isEmpty()) {
      useSpecific = true;
    }
    boolean satisfied = false;
    int numberMet = 0;
    for (final Territory terr : territories) {
      final Collection<Unit> allUnits = new ArrayList<>(terr.getUnits().getUnits());
      if (exclType.equals("direct")) {
        allUnits.removeAll(
            CollectionUtils.getMatches(allUnits, Matches.unitIsOwnedByOfAnyOfThesePlayers(players).negate()));
      } else if (exclType.equals("allied")) {
        allUnits.retainAll(CollectionUtils.getMatches(allUnits, Matches.alliedUnitOfAnyOfThesePlayers(players, data)));
      } else if (exclType.equals("enemy")) {
        allUnits.retainAll(CollectionUtils.getMatches(allUnits, Matches.enemyUnitOfAnyOfThesePlayers(players, data)));
      } else {
        return false;
      }
      if (allUnits.size() > 0) {
        if (!useSpecific) {
          numberMet += 1;
          if (numberMet >= numberNeeded) {
            satisfied = true;
            if (!getCountEach()) {
              break;
            }
          }
        } else if (useSpecific) {
          final IntegerMap<String> unitComboMap = getUnitPresence();
          final Set<String> unitCombos = unitComboMap.keySet();
          boolean hasEnough = false;
          for (final String uc : unitCombos) {
            final int unitsNeeded = unitComboMap.getInt(uc);
            if (uc == null || uc.equals("ANY") || uc.equals("any")) {
              hasEnough = allUnits.size() >= unitsNeeded;
            } else {
              final Set<UnitType> typesAllowed = data.getUnitTypeList().getUnitTypes(uc.split(":"));
              hasEnough =
                  CollectionUtils.getMatches(allUnits, Matches.unitIsOfTypes(typesAllowed)).size() >= unitsNeeded;
            }
            if (!hasEnough) {
              break;
            }
          }
          if (hasEnough) {
            numberMet += 1;
            if (numberMet >= numberNeeded) {
              satisfied = true;
              if (!getCountEach()) {
                break;
              }
            }
          }
        }
      }
    }
    if (getCountEach()) {
      m_eachMultiple = numberMet;
    }
    return satisfied;
  }

  /**
   * Checks for the collection of territories to see if they have units owned by the exclType alliance.
   * It doesn't yet threshold the data
   */
  private boolean checkUnitExclusions(final Collection<Territory> territories, final String exclType,
      final int numberNeeded, final List<PlayerID> players, final GameData data) {
    boolean useSpecific = false;
    if (getUnitPresence() != null && !getUnitPresence().keySet().isEmpty()) {
      useSpecific = true;
    }
    // Go through the owned territories and see if there are any units owned by allied/enemy based on exclType
    boolean satisfied = false;
    int numberMet = 0;
    for (final Territory terr : territories) {
      // get all the units in the territory
      final Collection<Unit> allUnits = new ArrayList<>(terr.getUnits().getUnits());
      if (exclType.equals("allied")) { // any allied units in the territory. (does not include owned units)
        allUnits.removeAll(CollectionUtils.getMatches(allUnits, Matches.unitIsOwnedByOfAnyOfThesePlayers(players)));
        allUnits.retainAll(CollectionUtils.getMatches(allUnits, Matches.alliedUnitOfAnyOfThesePlayers(players, data)));
      } else if (exclType.equals("direct")) {
        allUnits.removeAll(
            CollectionUtils.getMatches(allUnits, Matches.unitIsOwnedByOfAnyOfThesePlayers(players).negate()));
      } else if (exclType.equals("enemy")) { // any enemy units in the territory
        allUnits.retainAll(CollectionUtils.getMatches(allUnits, Matches.enemyUnitOfAnyOfThesePlayers(players, data)));
      } else if (exclType.equals("enemy_surface")) { // any enemy units (not trn/sub) in the territory
        allUnits.retainAll(
            CollectionUtils.getMatches(allUnits, Matches.enemyUnitOfAnyOfThesePlayers(players, data)
                .and(Matches.unitIsNotSub())
                .and(Matches.unitIsNotTransportButCouldBeCombatTransport())));
      } else {
        return false;
      }
      if (allUnits.size() == 0) {
        numberMet += 1;
        if (numberMet >= numberNeeded) {
          satisfied = true;
          if (!getCountEach()) {
            break;
          }
        }
      } else if (useSpecific) {
        final IntegerMap<String> unitComboMap = getUnitPresence();
        final Set<String> unitCombos = unitComboMap.keySet();
        boolean hasLess = false;
        for (final String uc : unitCombos) {
          final int unitsMax = unitComboMap.getInt(uc);
          if (uc == null || uc.equals("ANY") || uc.equals("any")) {
            hasLess = allUnits.size() <= unitsMax;
          } else {
            final Set<UnitType> typesAllowed = data.getUnitTypeList().getUnitTypes(uc.split(":"));
            hasLess = CollectionUtils.getMatches(allUnits, Matches.unitIsOfTypes(typesAllowed)).size() <= unitsMax;
          }
          if (!hasLess) {
            break;
          }
        }
        if (hasLess) {
          numberMet += 1;
          if (numberMet >= numberNeeded) {
            satisfied = true;
            if (!getCountEach()) {
              break;
            }
          }
        }
      }
    }
    if (getCountEach()) {
      m_eachMultiple = numberMet;
    }
    return satisfied;
  }

  /**
   * Checks for allied ownership of the collection of territories. Once the needed number threshold is reached, the
   * satisfied flag is set
   * to true and returned
   */
  private boolean checkAlliedOwnership(final Collection<Territory> listedTerrs,
      final int numberNeeded, final Collection<PlayerID> players, final GameData data) {
    int numberMet = 0;
    boolean satisfied = false;
    final Collection<PlayerID> allies = CollectionUtils.getMatches(data.getPlayerList().getPlayers(),
        Matches.isAlliedWithAnyOfThesePlayers(players, data));
    for (final Territory listedTerr : listedTerrs) {
      // if the territory owner is an ally
      if (Matches.isTerritoryOwnedBy(allies).test(listedTerr)) {
        numberMet += 1;
        if (numberMet >= numberNeeded) {
          satisfied = true;
          if (!getCountEach()) {
            break;
          }
        }
      }
    }
    if (getCountEach()) {
      m_eachMultiple = numberMet;
    }
    return satisfied;
  }

  /**
   * Checks for direct ownership of the collection of territories. Once the needed number threshold is reached, return
   * true.
   */
  private boolean checkDirectOwnership(final Collection<Territory> listedTerrs,
      final int numberNeeded, final Collection<PlayerID> players) {
    int numberMet = 0;
    boolean satisfied = false;
    for (final Territory listedTerr : listedTerrs) {
      if (Matches.isTerritoryOwnedBy(players).test(listedTerr)) {
        numberMet += 1;
        if (numberMet >= numberNeeded) {
          satisfied = true;
          if (!getCountEach()) {
            break;
          }
        }
      }
    }
    if (getCountEach()) {
      m_eachMultiple = numberMet;
    }
    return satisfied;
  }

  private boolean checkAtWar(final PlayerID player, final Set<PlayerID> enemies, final int count, final GameData data) {
    int found = 0;
    for (final PlayerID e : enemies) {
      if (data.getRelationshipTracker().isAtWar(player, e)) {
        found++;
      }
    }
    if (count == 0) {
      return found == 0;
    }
    if (getCountEach()) {
      m_eachMultiple = found;
    }
    return found >= count;
  }

  private boolean checkTechs(final PlayerID player, final GameData data) {
    int found = 0;
    for (final TechAdvance a : TechTracker.getCurrentTechAdvances(player, data)) {
      if (m_techs.contains(a)) {
        found++;
      }
    }
    if (m_techCount == 0) {
      return m_techCount == found;
    }
    if (getCountEach()) {
      m_eachMultiple = found;
    }
    return found >= m_techCount;
  }

  @Override
  public void validate(final GameData data) {
    validateNames(m_alliedOwnershipTerritories);
    validateNames(m_enemyExclusionTerritories);
    validateNames(m_enemySurfaceExclusionTerritories);
    validateNames(m_alliedExclusionTerritories);
    validateNames(m_directExclusionTerritories);
    validateNames(m_directOwnershipTerritories);
    validateNames(m_directPresenceTerritories);
    validateNames(m_alliedPresenceTerritories);
    validateNames(m_enemyPresenceTerritories);
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("techs",
            MutableProperty.of(
                List.class,
                this::setTechs,
                this::setTechs,
                this::getTechs,
                this::resetTechs))
        .put("techCount",
            MutableProperty.of(Integer.class, this::getTechCount))
        .put("relationship",
            MutableProperty.of(
                List.class,
                this::setRelationship,
                this::setRelationship,
                this::getRelationship,
                this::resetRelationship))
        .put("atWarPlayers",
            MutableProperty.of(
                Set.class,
                this::setAtWarPlayers,
                this::setAtWarPlayers,
                this::getAtWarPlayers,
                this::resetAtWarPlayers))
        .put("atWarCount",
            MutableProperty.of(Integer.class, this::getAtWarCount))
        .put("destroyedTUV",
            MutableProperty.ofString(
                this::setDestroyedTUV,
                this::getDestroyedTUV,
                this::resetDestroyedTUV))
        .put("battle",
            MutableProperty.of(
                List.class,
                this::setBattle,
                this::setBattle,
                this::getBattle,
                this::resetBattle))
        .put("alliedOwnershipTerritories",
            MutableProperty.of(
                String[].class,
                this::setAlliedOwnershipTerritories,
                this::setAlliedOwnershipTerritories,
                this::getAlliedOwnershipTerritories,
                this::resetAlliedOwnershipTerritories))
        .put("directOwnershipTerritories",
            MutableProperty.of(
                String[].class,
                this::setDirectOwnershipTerritories,
                this::setDirectOwnershipTerritories,
                this::getDirectOwnershipTerritories,
                this::resetDirectOwnershipTerritories))
        .put("alliedExclusionTerritories",
            MutableProperty.of(
                String[].class,
                this::setAlliedExclusionTerritories,
                this::setAlliedExclusionTerritories,
                this::getAlliedExclusionTerritories,
                this::resetAlliedExclusionTerritories))
        .put("directExclusionTerritories",
            MutableProperty.of(
                String[].class,
                this::setDirectExclusionTerritories,
                this::setDirectExclusionTerritories,
                this::getDirectExclusionTerritories,
                this::resetDirectExclusionTerritories))
        .put("enemyExclusionTerritories",
            MutableProperty.of(
                String[].class,
                this::setEnemyExclusionTerritories,
                this::setEnemyExclusionTerritories,
                this::getEnemyExclusionTerritories,
                this::resetEnemyExclusionTerritories))
        .put("enemySurfaceExclusionTerritories",
            MutableProperty.of(
                String[].class,
                this::setEnemySurfaceExclusionTerritories,
                this::setEnemySurfaceExclusionTerritories,
                this::getEnemySurfaceExclusionTerritories,
                this::resetEnemySurfaceExclusionTerritories))
        .put("directPresenceTerritories",
            MutableProperty.of(
                String[].class,
                this::setDirectPresenceTerritories,
                this::setDirectPresenceTerritories,
                this::getDirectPresenceTerritories,
                this::resetDirectPresenceTerritories))
        .put("alliedPresenceTerritories",
            MutableProperty.of(
                String[].class,
                this::setAlliedPresenceTerritories,
                this::setAlliedPresenceTerritories,
                this::getAlliedPresenceTerritories,
                this::resetAlliedPresenceTerritories))
        .put("enemyPresenceTerritories",
            MutableProperty.of(
                String[].class,
                this::setEnemyPresenceTerritories,
                this::setEnemyPresenceTerritories,
                this::getEnemyPresenceTerritories,
                this::resetEnemyPresenceTerritories))
        .put("unitPresence",
            MutableProperty.of(
                IntegerMap.class,
                this::setUnitPresence,
                this::setUnitPresence,
                this::getUnitPresence,
                this::resetUnitPresence))
        .build();
  }
}
