package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.NotificationMessages;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;
import lombok.extern.java.Log;

@MapSupport
@Log
public class TriggerAttachment extends AbstractTriggerAttachment {
  private static final long serialVersionUID = -3327739180569606093L;
  private static final String PREFIX_CLEAR = "-clear-";
  private static final String PREFIX_RESET = "-reset-";

  private ProductionFrontier m_frontier = null;
  private List<String> m_productionRule = null;
  private List<TechAdvance> m_tech = new ArrayList<>();
  private Map<String, Map<TechAdvance, Boolean>> m_availableTech = null;
  private Map<Territory, IntegerMap<UnitType>> m_placement = null;
  private Map<Territory, IntegerMap<UnitType>> m_removeUnits = null;
  private IntegerMap<UnitType> m_purchase = null;
  private String m_resource = null;
  private int m_resourceCount = 0;
  // never use a map of other attachments, inside of an attachment. java will not be able to deserialize it.
  private Map<String, Boolean> m_support = null;
  // List of relationshipChanges that should be executed when this trigger hits.
  private List<String> m_relationshipChange = new ArrayList<>();
  private String m_victory = null;
  private List<Tuple<String, String>> m_activateTrigger = new ArrayList<>();
  private List<String> m_changeOwnership = new ArrayList<>();
  // raw property changes below:
  //
  // really m_unitTypes, but we are not going to rename because it will break all existing maps
  private List<UnitType> m_unitType = new ArrayList<>();
  private Tuple<String, String> m_unitAttachmentName = null; // covers UnitAttachment, UnitSupportAttachment
  private List<Tuple<String, String>> m_unitProperty = null;
  private List<Territory> m_territories = new ArrayList<>();
  private Tuple<String, String> m_territoryAttachmentName = null; // covers TerritoryAttachment, CanalAttachment
  private List<Tuple<String, String>> m_territoryProperty = null;
  private List<PlayerID> m_players = new ArrayList<>();
  // covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment, UserActionAttachment
  private Tuple<String, String> m_playerAttachmentName = null;
  private List<Tuple<String, String>> m_playerProperty = null;
  private List<RelationshipType> m_relationshipTypes = new ArrayList<>();
  private Tuple<String, String> m_relationshipTypeAttachmentName = null; // covers RelationshipTypeAttachment
  private List<Tuple<String, String>> m_relationshipTypeProperty = null;
  private List<TerritoryEffect> m_territoryEffects = new ArrayList<>();
  private Tuple<String, String> m_territoryEffectAttachmentName = null; // covers TerritoryEffectAttachment
  private List<Tuple<String, String>> m_territoryEffectProperty = null;

  public TriggerAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Convenience method for returning TriggerAttachments.
   *
   * @return a new trigger attachment
   */
  public static TriggerAttachment get(final PlayerID player, final String nameOfAttachment) {
    return get(player, nameOfAttachment, null);
  }

  public static TriggerAttachment get(final PlayerID player, final String nameOfAttachment,
      final Collection<PlayerID> playersToSearch) {
    TriggerAttachment ta = (TriggerAttachment) player.getAttachment(nameOfAttachment);
    if (ta == null) {
      if (playersToSearch == null) {
        throw new IllegalStateException(
            "Triggers: No trigger attachment for:" + player.getName() + " with name: " + nameOfAttachment);
      }

      for (final PlayerID otherPlayer : playersToSearch) {
        if (otherPlayer == player) {
          continue;
        }
        ta = (TriggerAttachment) otherPlayer.getAttachment(nameOfAttachment);
        if (ta != null) {
          return ta;
        }
      }
      throw new IllegalStateException(
          "Triggers: No trigger attachment for:" + player.getName() + " with name: " + nameOfAttachment);
    }
    return ta;
  }

  /**
   * Convenience method for return all TriggerAttachments attached to a player.
   *
   * @return set of trigger attachments (If you use null for the match condition, you will get all triggers for this
   *         player)
   */
  static Set<TriggerAttachment> getTriggers(final PlayerID player, final Predicate<TriggerAttachment> cond) {
    final Set<TriggerAttachment> trigs = new HashSet<>();
    for (final IAttachment a : player.getAttachments().values()) {
      if (a instanceof TriggerAttachment) {
        if (cond == null || cond.test((TriggerAttachment) a)) {
          trigs.add((TriggerAttachment) a);
        }
      }
    }
    return trigs;
  }

  /**
   * This will collect all triggers for the desired players, based on a match provided,
   * and then it will gather all the conditions necessary, then test all the conditions,
   * and then it will fire all the conditions which are satisfied.
   */
  public static void collectAndFireTriggers(final HashSet<PlayerID> players,
      final Predicate<TriggerAttachment> triggerMatch, final IDelegateBridge bridge, final String beforeOrAfter,
      final String stepName) {
    final HashSet<TriggerAttachment> toFirePossible = collectForAllTriggersMatching(players, triggerMatch);
    if (toFirePossible.isEmpty()) {
      return;
    }
    final HashMap<ICondition, Boolean> testedConditions = collectTestsForAllTriggers(toFirePossible, bridge);
    final List<TriggerAttachment> toFireTestedAndSatisfied =
        CollectionUtils.getMatches(toFirePossible, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions));
    if (toFireTestedAndSatisfied.isEmpty()) {
      return;
    }
    TriggerAttachment.fireTriggers(new HashSet<>(toFireTestedAndSatisfied), testedConditions, bridge,
        beforeOrAfter, stepName, true, true, true, true);
  }

  public static HashSet<TriggerAttachment> collectForAllTriggersMatching(final HashSet<PlayerID> players,
      final Predicate<TriggerAttachment> triggerMatch) {
    final HashSet<TriggerAttachment> toFirePossible = new HashSet<>();
    for (final PlayerID player : players) {
      toFirePossible.addAll(TriggerAttachment.getTriggers(player, triggerMatch));
    }
    return toFirePossible;
  }

  public static HashMap<ICondition, Boolean> collectTestsForAllTriggers(final HashSet<TriggerAttachment> toFirePossible,
      final IDelegateBridge bridge) {
    return collectTestsForAllTriggers(toFirePossible, bridge, null, null);
  }

  static HashMap<ICondition, Boolean> collectTestsForAllTriggers(final HashSet<TriggerAttachment> toFirePossible,
      final IDelegateBridge bridge, final HashSet<ICondition> allConditionsNeededSoFar,
      final HashMap<ICondition, Boolean> allConditionsTestedSoFar) {
    final HashSet<ICondition> allConditionsNeeded = AbstractConditionsAttachment
        .getAllConditionsRecursive(new HashSet<>(toFirePossible), allConditionsNeededSoFar);
    return AbstractConditionsAttachment.testAllConditionsRecursive(allConditionsNeeded, allConditionsTestedSoFar,
        bridge);
  }

  /**
   * This will fire all triggers, and it will not test to see if they are satisfied or not first. Please use
   * collectAndFireTriggers instead
   * of using this directly.
   * To see if they are satisfied, first create the list of triggers using Matches + TriggerAttachment.getTriggers.
   * Then test the triggers using RulesAttachment.getAllConditionsRecursive, and RulesAttachment.testAllConditions
   */
  public static void fireTriggers(final HashSet<TriggerAttachment> triggersToBeFired,
      final HashMap<ICondition, Boolean> testedConditionsSoFar, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    // all triggers at this point have their conditions satisfied
    // so we now test chance (because we test chance last), and remove any conditions that do not succeed in their dice
    // rolls
    final HashSet<TriggerAttachment> triggersToFire = new HashSet<>();
    for (final TriggerAttachment t : triggersToBeFired) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      triggersToFire.add(t);
    }
    // Order: Notifications, Attachment Property Changes (Player, Relationship, Territory, TerritoryEffect, Unit),
    // Relationship,
    // AvailableTech, Tech, ProductionFrontier, ProductionEdit, Support, Purchase, UnitPlacement, Resource, Victory
    // Notifications to current player
    triggerNotifications(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    // Attachment property changes
    triggerPlayerPropertyChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerRelationshipTypePropertyChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false,
        testWhen);
    triggerTerritoryPropertyChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false,
        testWhen);
    triggerTerritoryEffectPropertyChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false,
        testWhen);
    triggerUnitPropertyChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    // Misc changes that only need to happen once (twice or more is meaningless)
    triggerRelationshipChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerAvailableTechChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerTechChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerProductionChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerProductionFrontierEditChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false,
        testWhen);
    triggerSupportChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerChangeOwnership(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    // Misc changes that can happen multiple times, because they add or subtract, something from the game (and therefore
    // can use "each")
    triggerUnitRemoval(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerPurchase(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerUnitPlacement(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    triggerResourceChange(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    // Activating other triggers, and trigger victory, should ALWAYS be LAST in this list!
    triggerActivateTriggerOther(testedConditionsSoFar, triggersToFire, bridge, beforeOrAfter, stepName, useUses,
        testUses, false, testWhen); // Triggers firing other triggers
    // Victory messages and recording of winners
    triggerVictory(triggersToFire, bridge, beforeOrAfter, stepName, useUses, testUses, false, testWhen);
    // for both 'when' and 'activated triggers', we can change the uses now. (for other triggers, we change at end of
    // each round)
    if (useUses) {
      setUsesForWhenTriggers(triggersToFire, bridge, useUses);
    }
  }

  protected static void setUsesForWhenTriggers(final HashSet<TriggerAttachment> triggersToBeFired,
      final IDelegateBridge bridge, final boolean useUses) {
    if (!useUses) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment trig : triggersToBeFired) {
      final int currentUses = trig.getUses();
      // we only care about triggers that have WHEN set. Triggers without When set are changed during EndRoundDelegate.
      if (currentUses > 0 && !trig.getWhen().isEmpty()) {
        change.add(ChangeFactory.attachmentPropertyChange(trig, Integer.toString(currentUses - 1), "uses"));
        if (trig.getUsedThisRound()) {
          change.add(ChangeFactory.attachmentPropertyChange(trig, false, "usedThisRound"));
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Setting uses for triggers used this phase.");
      bridge.addChange(change);
    }
  }

  private void setActivateTrigger(final String value) throws GameParseException {
    // triggerName:numberOfTimes:useUses:testUses:testConditions:testChance
    final String[] s = value.split(":");
    if (s.length != 6) {
      throw new GameParseException(
          "activateTrigger must have 6 parts: triggerName:numberOfTimes:useUses:testUses:testConditions:testChance"
              + thisErrorMsg());
    }
    TriggerAttachment trigger = null;
    for (final PlayerID player : getData().getPlayerList().getPlayers()) {
      for (final TriggerAttachment ta : getTriggers(player, null)) {
        if (ta.getName().equals(s[0])) {
          trigger = ta;
          break;
        }
      }
      if (trigger != null) {
        break;
      }
    }
    if (trigger == null) {
      throw new GameParseException("No TriggerAttachment named: " + s[0] + thisErrorMsg());
    }
    if (trigger == this) {
      throw new GameParseException("Cannot have a trigger activate itself!" + thisErrorMsg());
    }
    String options = value;
    options = options.replaceFirst((s[0] + ":"), "");
    final int numberOfTimes = getInt(s[1]);
    if (numberOfTimes < 0) {
      throw new GameParseException(
          "activateTrigger must be positive for the number of times to fire: " + s[1] + thisErrorMsg());
    }
    getBool(s[2]);
    getBool(s[3]);
    getBool(s[4]);
    getBool(s[5]);
    m_activateTrigger.add(Tuple.of(s[0], options));
  }

  private void setActivateTrigger(final List<Tuple<String, String>> value) {
    m_activateTrigger = value;
  }

  private List<Tuple<String, String>> getActivateTrigger() {
    return m_activateTrigger;
  }

  private void resetActivateTrigger() {
    m_activateTrigger = new ArrayList<>();
  }

  private void setFrontier(final String s) throws GameParseException {
    if (s == null) {
      m_frontier = null;
      return;
    }
    final ProductionFrontier front = getData().getProductionFrontierList().getProductionFrontier(s);
    if (front == null) {
      throw new GameParseException("Could not find frontier. name:" + s + thisErrorMsg());
    }
    m_frontier = front;
  }

  private void setFrontier(final ProductionFrontier value) {
    m_frontier = value;
  }

  private ProductionFrontier getFrontier() {
    return m_frontier;
  }

  private void resetFrontier() {
    m_frontier = null;
  }

  private void setProductionRule(final String prop) throws GameParseException {
    if (prop == null) {
      m_productionRule = null;
      return;
    }
    final String[] s = prop.split(":");
    if (s.length != 2) {
      throw new GameParseException("Invalid productionRule declaration: " + prop + thisErrorMsg());
    }
    if (m_productionRule == null) {
      m_productionRule = new ArrayList<>();
    }
    if (getData().getProductionFrontierList().getProductionFrontier(s[0]) == null) {
      throw new GameParseException("Could not find frontier. name:" + s[0] + thisErrorMsg());
    }
    String rule = s[1];
    if (rule.startsWith("-")) {
      rule = rule.replaceFirst("-", "");
    }
    if (getData().getProductionRuleList().getProductionRule(rule) == null) {
      throw new GameParseException("Could not find production rule. name:" + rule + thisErrorMsg());
    }
    m_productionRule.add(prop);
  }

  private void setProductionRule(final List<String> value) {
    m_productionRule = value;
  }

  List<String> getProductionRule() {
    return m_productionRule;
  }

  private void resetProductionRule() {
    m_productionRule = null;
  }

  private void setResourceCount(final String s) {
    m_resourceCount = getInt(s);
  }

  private void setResourceCount(final Integer s) {
    m_resourceCount = s;
  }

  private int getResourceCount() {
    return m_resourceCount;
  }

  private void resetResourceCount() {
    m_resourceCount = 0;
  }

  private void setVictory(final String s) {
    if (s == null) {
      m_victory = null;
      return;
    }
    m_victory = s;
  }

  private String getVictory() {
    return m_victory;
  }

  private void resetVictory() {
    m_victory = null;
  }

  private void setTech(final String techs) throws GameParseException {
    for (final String subString : techs.split(":")) {
      TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(subString);
      if (ta == null) {
        ta = getData().getTechnologyFrontier().getAdvanceByName(subString);
      }
      if (ta == null) {
        throw new GameParseException("Technology not found :" + subString + thisErrorMsg());
      }
      m_tech.add(ta);
    }
  }

  private void setTech(final List<TechAdvance> value) {
    m_tech = value;
  }

  private List<TechAdvance> getTech() {
    return m_tech;
  }

  private void resetTech() {
    m_tech = new ArrayList<>();
  }

  private void setAvailableTech(final String techs) throws GameParseException {
    if (techs == null) {
      m_availableTech = null;
      return;
    }
    final String[] s = techs.split(":");
    if (s.length < 2) {
      throw new GameParseException(
          "Invalid tech availability: " + techs + " should be category:techs" + thisErrorMsg());
    }
    final String cat = s[0];
    final LinkedHashMap<TechAdvance, Boolean> tlist = new LinkedHashMap<>();
    for (int i = 1; i < s.length; i++) {
      boolean add = true;
      if (s[i].startsWith("-")) {
        add = false;
        s[i] = s[i].substring(1);
      }
      TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
      if (ta == null) {
        ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
      }
      if (ta == null) {
        throw new GameParseException("Technology not found :" + s[i] + thisErrorMsg());
      }
      tlist.put(ta, add);
    }
    if (m_availableTech == null) {
      m_availableTech = new HashMap<>();
    }
    if (m_availableTech.containsKey(cat)) {
      tlist.putAll(m_availableTech.get(cat));
    }
    m_availableTech.put(cat, tlist);
  }

  private void setAvailableTech(final Map<String, Map<TechAdvance, Boolean>> value) {
    m_availableTech = value;
  }

  private Map<String, Map<TechAdvance, Boolean>> getAvailableTech() {
    return m_availableTech;
  }

  private void resetAvailableTech() {
    m_availableTech = null;
  }

  private void setSupport(final String sup) throws GameParseException {
    if (sup == null) {
      m_support = null;
      return;
    }
    final String[] s = sup.split(":");
    for (int i = 0; i < s.length; i++) {
      boolean add = true;
      if (s[i].startsWith("-")) {
        add = false;
        s[i] = s[i].substring(1);
      }
      boolean found = false;
      for (final UnitSupportAttachment support : UnitSupportAttachment.get(getData())) {
        if (support.getName().equals(s[i])) {
          found = true;
          if (m_support == null) {
            m_support = new LinkedHashMap<>();
          }
          m_support.put(s[i], add);
          break;
        }
      }
      if (!found) {
        throw new GameParseException("Could not find unitSupportAttachment. name:" + s[i] + thisErrorMsg());
      }
    }
  }

  private void setSupport(final Map<String, Boolean> value) {
    m_support = value;
  }

  private Map<String, Boolean> getSupport() {
    return m_support;
  }

  private void resetSupport() {
    m_support = null;
  }

  private void setResource(final String s) throws GameParseException {
    if (s == null) {
      m_resource = null;
      return;
    }
    final Resource r = getData().getResourceList().getResource(s);
    if (r == null) {
      throw new GameParseException("Invalid resource: " + s + thisErrorMsg());
    }
    m_resource = s;
  }

  private String getResource() {
    return m_resource;
  }

  private void resetResource() {
    m_resource = null;
  }

  private void setRelationshipChange(final String relChange) throws GameParseException {
    final String[] s = relChange.split(":");
    if (s.length != 4) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange
          + " \n Use: player1:player2:oldRelation:newRelation\n" + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[0]) == null) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n player: " + s[0]
          + " unknown " + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[1]) == null) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n player: " + s[1]
          + " unknown " + thisErrorMsg());
    }
    if (!(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
        || Matches.isValidRelationshipName(getData()).test(s[2]))) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n relationshipType: "
          + s[2] + " unknown " + thisErrorMsg());
    }
    if (Matches.isValidRelationshipName(getData()).negate().test(s[3])) {
      throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n relationshipType: "
          + s[3] + " unknown " + thisErrorMsg());
    }
    m_relationshipChange.add(relChange);
  }

  private void setRelationshipChange(final List<String> value) {
    m_relationshipChange = value;
  }

  private List<String> getRelationshipChange() {
    return m_relationshipChange;
  }

  private void resetRelationshipChange() {
    m_relationshipChange = new ArrayList<>();
  }

  private void setUnitType(final String names) throws GameParseException {
    final String[] s = names.split(":");
    for (final String element : s) {
      final UnitType type = getData().getUnitTypeList().getUnitType(element);
      if (type == null) {
        throw new GameParseException("Could not find unitType. name:" + element + thisErrorMsg());
      }
      m_unitType.add(type);
    }
  }

  private void setUnitType(final List<UnitType> value) {
    m_unitType = value;
  }

  private List<UnitType> getUnitType() {
    return m_unitType;
  }

  private void resetUnitType() {
    m_unitType = new ArrayList<>();
  }

  private void setUnitAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      m_unitAttachmentName = null;
      return;
    }
    final String[] s = name.split(":");
    if (s.length != 2) {
      throw new GameParseException(
          "unitAttachmentName must have 2 entries, the type of attachment and the name of the attachment."
              + thisErrorMsg());
    }
    // covers UnitAttachment, UnitSupportAttachment
    if (!(s[1].equals("UnitAttachment") || s[1].equals("UnitSupportAttachment"))) {
      throw new GameParseException(
          "unitAttachmentName value must be UnitAttachment or UnitSupportAttachment" + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException("unitAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("UnitAttachment") && !s[0].startsWith(Constants.UNIT_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("UnitSupportAttachment") && !s[0].startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    m_unitAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setUnitAttachmentName(final Tuple<String, String> value) {
    m_unitAttachmentName = value;
  }

  private Tuple<String, String> getUnitAttachmentName() {
    if (m_unitAttachmentName == null) {
      return Tuple.of("UnitAttachment", Constants.UNIT_ATTACHMENT_NAME);
    }
    return m_unitAttachmentName;
  }

  private void resetUnitAttachmentName() {
    m_unitAttachmentName = null;
  }

  private void setUnitProperty(final String prop) {
    if (prop == null) {
      m_unitProperty = null;
      return;
    }
    final String[] s = prop.split(":");
    if (m_unitProperty == null) {
      m_unitProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it to
    final String property = s[s.length - 1];
    m_unitProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setUnitProperty(final List<Tuple<String, String>> value) {
    m_unitProperty = value;
  }

  private List<Tuple<String, String>> getUnitProperty() {
    return m_unitProperty;
  }

  private void resetUnitProperty() {
    m_unitProperty = null;
  }

  private void setTerritories(final String names) throws GameParseException {
    final String[] s = names.split(":");
    for (final String element : s) {
      final Territory terr = getData().getMap().getTerritory(element);
      if (terr == null) {
        throw new GameParseException("Could not find territory. name:" + element + thisErrorMsg());
      }
      m_territories.add(terr);
    }
  }

  private void setTerritories(final List<Territory> value) {
    m_territories = value;
  }

  private List<Territory> getTerritories() {
    return m_territories;
  }

  private void resetTerritories() {
    m_territories = new ArrayList<>();
  }

  private void setTerritoryAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      m_territoryAttachmentName = null;
      return;
    }
    final String[] s = name.split(":");
    if (s.length != 2) {
      throw new GameParseException(
          "territoryAttachmentName must have 2 entries, the type of attachment and the name of the attachment."
              + thisErrorMsg());
    }
    // covers TerritoryAttachment, CanalAttachment
    if (!(s[1].equals("TerritoryAttachment") || s[1].equals("CanalAttachment"))) {
      throw new GameParseException(
          "territoryAttachmentName value must be TerritoryAttachment or CanalAttachment" + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException("territoryAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("TerritoryAttachment") && !s[0].startsWith(Constants.TERRITORY_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("CanalAttachment") && !s[0].startsWith(Constants.CANAL_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    m_territoryAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setTerritoryAttachmentName(final Tuple<String, String> value) {
    m_territoryAttachmentName = value;
  }

  private Tuple<String, String> getTerritoryAttachmentName() {
    if (m_territoryAttachmentName == null) {
      return Tuple.of("TerritoryAttachment", Constants.TERRITORY_ATTACHMENT_NAME);
    }
    return m_territoryAttachmentName;
  }

  private void resetTerritoryAttachmentName() {
    m_territoryAttachmentName = null;
  }

  private void setTerritoryProperty(final String prop) {
    if (prop == null) {
      m_territoryProperty = null;
      return;
    }
    final String[] s = prop.split(":");
    if (m_territoryProperty == null) {
      m_territoryProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it to
    final String property = s[s.length - 1];
    m_territoryProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setTerritoryProperty(final List<Tuple<String, String>> value) {
    m_territoryProperty = value;
  }

  private List<Tuple<String, String>> getTerritoryProperty() {
    return m_territoryProperty;
  }

  private void resetTerritoryProperty() {
    m_territoryProperty = null;
  }

  private void setPlayers(final String names) throws GameParseException {
    final String[] s = names.split(":");
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

  private List<PlayerID> getPlayers() {
    return m_players.isEmpty() ? new ArrayList<>(Collections.singletonList((PlayerID) getAttachedTo())) : m_players;
  }

  private void resetPlayers() {
    m_players = new ArrayList<>();
  }

  private void setPlayerAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      m_playerAttachmentName = null;
      return;
    }
    final String[] s = name.split(":");
    if (s.length != 2) {
      throw new GameParseException(
          "playerAttachmentName must have 2 entries, the type of attachment and the name of the attachment."
              + thisErrorMsg());
    }
    // covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment
    if (!(s[1].equals("PlayerAttachment") || s[1].equals("RulesAttachment") || s[1].equals("TriggerAttachment")
        || s[1].equals("TechAttachment") || s[1].equals("PoliticalActionAttachment")
        || s[1].equals("UserActionAttachment"))) {
      throw new GameParseException("playerAttachmentName value must be PlayerAttachment or RulesAttachment or "
          + "TriggerAttachment or TechAttachment or PoliticalActionAttachment or UserActionAttachment"
          + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException("playerAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("PlayerAttachment") && !s[0].startsWith(Constants.PLAYER_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("RulesAttachment") && !(s[0].startsWith(Constants.RULES_ATTACHMENT_NAME)
        || s[0].startsWith(Constants.RULES_OBJECTIVE_PREFIX) || s[0].startsWith(Constants.RULES_CONDITION_PREFIX))) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("TriggerAttachment") && !s[0].startsWith(Constants.TRIGGER_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("TechAttachment") && !s[0].startsWith(Constants.TECH_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("PoliticalActionAttachment") && !s[0].startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("UserActionAttachment") && !s[0].startsWith(Constants.USERACTION_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    m_playerAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setPlayerAttachmentName(final Tuple<String, String> value) {
    m_playerAttachmentName = value;
  }

  private Tuple<String, String> getPlayerAttachmentName() {
    if (m_playerAttachmentName == null) {
      return Tuple.of("PlayerAttachment", Constants.PLAYER_ATTACHMENT_NAME);
    }
    return m_playerAttachmentName;
  }

  private void resetPlayerAttachmentName() {
    m_playerAttachmentName = null;
  }

  private void setPlayerProperty(final String prop) {
    if (prop == null) {
      m_playerProperty = null;
      return;
    }
    final String[] s = prop.split(":");
    if (m_playerProperty == null) {
      m_playerProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it to
    final String property = s[s.length - 1];
    m_playerProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setPlayerProperty(final List<Tuple<String, String>> value) {
    m_playerProperty = value;
  }

  private List<Tuple<String, String>> getPlayerProperty() {
    return m_playerProperty;
  }

  private void resetPlayerProperty() {
    m_playerProperty = null;
  }

  private void setRelationshipTypes(final String names) throws GameParseException {
    final String[] s = names.split(":");
    for (final String element : s) {
      final RelationshipType relation = getData().getRelationshipTypeList().getRelationshipType(element);
      if (relation == null) {
        throw new GameParseException("Could not find relationshipType. name:" + element + thisErrorMsg());
      }
      m_relationshipTypes.add(relation);
    }
  }

  private void setRelationshipTypes(final List<RelationshipType> value) {
    m_relationshipTypes = value;
  }

  private List<RelationshipType> getRelationshipTypes() {
    return m_relationshipTypes;
  }

  private void resetRelationshipTypes() {
    m_relationshipTypes = new ArrayList<>();
  }

  private void setRelationshipTypeAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      m_relationshipTypeAttachmentName = null;
      return;
    }
    final String[] s = name.split(":");
    if (s.length != 2) {
      throw new GameParseException(
          "relationshipTypeAttachmentName must have 2 entries, the type of attachment and the name of the attachment."
              + thisErrorMsg());
    }
    // covers RelationshipTypeAttachment
    if (!(s[1].equals("RelationshipTypeAttachment"))) {
      throw new GameParseException(
          "relationshipTypeAttachmentName value must be RelationshipTypeAttachment" + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException(
          "relationshipTypeAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("RelationshipTypeAttachment") && !s[0].startsWith(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    m_relationshipTypeAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setRelationshipTypeAttachmentName(final Tuple<String, String> value) {
    m_relationshipTypeAttachmentName = value;
  }

  private Tuple<String, String> getRelationshipTypeAttachmentName() {
    if (m_relationshipTypeAttachmentName == null) {
      return Tuple.of("RelationshipTypeAttachment", Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME);
    }
    return m_relationshipTypeAttachmentName;
  }

  private void resetRelationshipTypeAttachmentName() {
    m_relationshipTypeAttachmentName = null;
  }

  private void setRelationshipTypeProperty(final String prop) {
    if (prop == null) {
      m_relationshipTypeProperty = null;
      return;
    }
    final String[] s = prop.split(":");
    if (m_relationshipTypeProperty == null) {
      m_relationshipTypeProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it to
    final String property = s[s.length - 1];
    m_relationshipTypeProperty
        .add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setRelationshipTypeProperty(final List<Tuple<String, String>> value) {
    m_relationshipTypeProperty = value;
  }

  private List<Tuple<String, String>> getRelationshipTypeProperty() {
    return m_relationshipTypeProperty;
  }

  private void resetRelationshipTypeProperty() {
    m_relationshipTypeProperty = null;
  }

  private void setTerritoryEffects(final String names) throws GameParseException {
    final String[] s = names.split(":");
    for (final String element : s) {
      final TerritoryEffect effect = getData().getTerritoryEffectList().get(element);
      if (effect == null) {
        throw new GameParseException("Could not find territoryEffect. name:" + element + thisErrorMsg());
      }
      m_territoryEffects.add(effect);
    }
  }

  private void setTerritoryEffects(final List<TerritoryEffect> value) {
    m_territoryEffects = value;
  }

  private List<TerritoryEffect> getTerritoryEffects() {
    return m_territoryEffects;
  }

  private void resetTerritoryEffects() {
    m_territoryEffects = new ArrayList<>();
  }

  private void setTerritoryEffectAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      m_territoryEffectAttachmentName = null;
      return;
    }
    final String[] s = name.split(":");
    if (s.length != 2) {
      throw new GameParseException(
          "territoryEffectAttachmentName must have 2 entries, the type of attachment and the name of the attachment."
              + thisErrorMsg());
    }
    // covers TerritoryEffectAttachment
    if (!(s[1].equals("TerritoryEffectAttachment"))) {
      throw new GameParseException(
          "territoryEffectAttachmentName value must be TerritoryEffectAttachment" + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException(
          "territoryEffectAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("TerritoryEffectAttachment") && !s[0].startsWith(Constants.TERRITORYEFFECT_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    m_territoryEffectAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setTerritoryEffectAttachmentName(final Tuple<String, String> value) {
    m_territoryEffectAttachmentName = value;
  }

  private Tuple<String, String> getTerritoryEffectAttachmentName() {
    if (m_territoryEffectAttachmentName == null) {
      return Tuple.of("TerritoryEffectAttachment", Constants.TERRITORYEFFECT_ATTACHMENT_NAME);
    }
    return m_territoryEffectAttachmentName;
  }

  private void resetTerritoryEffectAttachmentName() {
    m_territoryEffectAttachmentName = null;
  }

  private void setTerritoryEffectProperty(final String prop) {
    if (prop == null) {
      m_territoryEffectProperty = null;
      return;
    }
    final String[] s = prop.split(":");
    if (m_territoryEffectProperty == null) {
      m_territoryEffectProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it to
    final String property = s[s.length - 1];
    m_territoryEffectProperty
        .add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setTerritoryEffectProperty(final List<Tuple<String, String>> value) {
    m_territoryEffectProperty = value;
  }

  private List<Tuple<String, String>> getTerritoryEffectProperty() {
    return m_territoryEffectProperty;
  }

  private void resetTerritoryEffectProperty() {
    m_territoryEffectProperty = null;
  }

  /**
   * Fudging this, it really represents adding placements.
   */
  private void setPlacement(final String place) throws GameParseException {
    if (place == null) {
      m_placement = null;
      return;
    }
    final String[] s = place.split(":");
    if (s.length < 1) {
      throw new GameParseException("Empty placement list" + thisErrorMsg());
    }
    int count;
    int i = 0;
    try {
      count = getInt(s[0]);
      i++;
    } catch (final Exception e) {
      count = 1;
    }
    if (s.length == 1 && count != -1) {
      throw new GameParseException("Empty placement list" + thisErrorMsg());
    }
    final Territory territory = getData().getMap().getTerritory(s[i]);
    if (territory == null) {
      throw new GameParseException("Territory does not exist " + s[i] + thisErrorMsg());
    }

    i++;
    final IntegerMap<UnitType> map = new IntegerMap<>();
    for (; i < s.length; i++) {
      final UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
      if (type == null) {
        throw new GameParseException("UnitType does not exist " + s[i] + thisErrorMsg());
      }
      map.add(type, count);
    }
    if (m_placement == null) {
      m_placement = new HashMap<>();
    }
    if (m_placement.containsKey(territory)) {
      map.add(m_placement.get(territory));
    }
    m_placement.put(territory, map);
  }

  private void setPlacement(final Map<Territory, IntegerMap<UnitType>> value) {
    m_placement = value;
  }

  private Map<Territory, IntegerMap<UnitType>> getPlacement() {
    return m_placement;
  }

  private void resetPlacement() {
    m_placement = null;
  }

  private void setRemoveUnits(final String value) throws GameParseException {
    if (value == null) {
      m_removeUnits = null;
      return;
    }
    if (m_removeUnits == null) {
      m_removeUnits = new HashMap<>();
    }
    final String[] s = value.split(":");
    if (s.length < 1) {
      throw new GameParseException("Empty removeUnits list" + thisErrorMsg());
    }
    int count;
    int i = 0;
    try {
      count = getInt(s[0]);
      i++;
    } catch (final Exception e) {
      count = 1;
    }
    if (s.length == 1 && count != -1) {
      throw new GameParseException("Empty removeUnits list" + thisErrorMsg());
    }
    final Collection<Territory> territories = new ArrayList<>();
    final Territory terr = getData().getMap().getTerritory(s[i]);
    if (terr == null) {
      if (s[i].equalsIgnoreCase("all")) {
        territories.addAll(getData().getMap().getTerritories());
      } else {
        throw new GameParseException("Territory does not exist " + s[i] + thisErrorMsg());
      }
    } else {
      territories.add(terr);
    }
    i++;
    final IntegerMap<UnitType> map = new IntegerMap<>();
    for (; i < s.length; i++) {
      final Collection<UnitType> types = new ArrayList<>();
      final UnitType tp = getData().getUnitTypeList().getUnitType(s[i]);
      if (tp == null) {
        if (s[i].equalsIgnoreCase("all")) {
          types.addAll(getData().getUnitTypeList().getAllUnitTypes());
        } else {
          throw new GameParseException("UnitType does not exist " + s[i] + thisErrorMsg());
        }
      } else {
        types.add(tp);
      }
      for (final UnitType type : types) {
        map.add(type, count);
      }
    }
    for (final Territory t : territories) {
      if (m_removeUnits.containsKey(t)) {
        map.add(m_removeUnits.get(t));
      }
      m_removeUnits.put(t, map);
    }
  }

  private void setRemoveUnits(final Map<Territory, IntegerMap<UnitType>> value) {
    m_removeUnits = value;
  }

  private Map<Territory, IntegerMap<UnitType>> getRemoveUnits() {
    return m_removeUnits;
  }

  private void resetRemoveUnits() {
    m_removeUnits = null;
  }

  private void setPurchase(final String place) throws GameParseException {
    if (place == null) {
      m_purchase = null;
      return;
    }
    final String[] s = place.split(":");
    if (s.length < 1) {
      throw new GameParseException("Empty purchase list" + thisErrorMsg());
    }
    int count;
    int i = 0;
    try {
      count = getInt(s[0]);
      i++;
    } catch (final Exception e) {
      count = 1;
    }
    if (s.length == 1 && count != -1) {
      throw new GameParseException("Empty purchase list" + thisErrorMsg());
    }

    if (m_purchase == null) {
      m_purchase = new IntegerMap<>();
    }
    for (; i < s.length; i++) {
      final UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
      if (type == null) {
        throw new GameParseException("UnitType does not exist " + s[i] + thisErrorMsg());
      }
      m_purchase.add(type, count);
    }
  }

  private void setPurchase(final IntegerMap<UnitType> value) {
    m_purchase = value;
  }

  private IntegerMap<UnitType> getPurchase() {
    return m_purchase;
  }

  private void resetPurchase() {
    m_purchase = null;
  }

  private void setChangeOwnership(final String value) throws GameParseException {
    // territory:oldOwner:newOwner:booleanConquered
    // can have "all" for territory and "any" for oldOwner
    final String[] s = value.split(":");
    if (s.length < 4) {
      throw new GameParseException(
          "changeOwnership must have 4 fields: territory:oldOwner:newOwner:booleanConquered" + thisErrorMsg());
    }
    if (!s[0].equalsIgnoreCase("all")) {
      final Territory t = getData().getMap().getTerritory(s[0]);
      if (t == null) {
        throw new GameParseException("No such territory: " + s[0] + thisErrorMsg());
      }
    }
    if (!s[1].equalsIgnoreCase("any")) {
      final PlayerID oldOwner = getData().getPlayerList().getPlayerId(s[1]);
      if (oldOwner == null) {
        throw new GameParseException("No such player: " + s[1] + thisErrorMsg());
      }
    }
    final PlayerID newOwner = getData().getPlayerList().getPlayerId(s[2]);
    if (newOwner == null) {
      throw new GameParseException("No such player: " + s[2] + thisErrorMsg());
    }
    getBool(s[3]);
    m_changeOwnership.add(value);
  }

  private void setChangeOwnership(final List<String> value) {
    m_changeOwnership = value;
  }

  private List<String> getChangeOwnership() {
    return m_changeOwnership;
  }

  private void resetChangeOwnership() {
    m_changeOwnership = new ArrayList<>();
  }

  private static void removeUnits(final TriggerAttachment t, final Territory terr, final IntegerMap<UnitType> utMap,
      final PlayerID player, final IDelegateBridge bridge) {
    final CompositeChange change = new CompositeChange();
    final Collection<Unit> totalRemoved = new ArrayList<>();
    for (final UnitType ut : utMap.keySet()) {
      final int removeNum = utMap.getInt(ut);
      final Collection<Unit> toRemove = CollectionUtils.getNMatches(terr.getUnits().getUnits(), removeNum,
          Matches.unitIsOwnedBy(player).and(Matches.unitIsOfType(ut)));
      if (!toRemove.isEmpty()) {
        totalRemoved.addAll(toRemove);
        change.add(ChangeFactory.removeUnits(terr, toRemove));
      }
    }
    if (!change.isEmpty()) {
      final String transcriptText = MyFormatter.attachmentNameToText(t.getName()) + ": has removed "
          + MyFormatter.unitsToTextNoOwner(totalRemoved) + " owned by " + player.getName() + " in " + terr.getName();
      bridge.getHistoryWriter().startEvent(transcriptText, totalRemoved);
      bridge.addChange(change);
    }
  }

  private static void placeUnits(final TriggerAttachment t, final Territory terr, final IntegerMap<UnitType> utMap,
      final PlayerID player, final IDelegateBridge bridge) {
    // createUnits
    final List<Unit> units = new ArrayList<>();
    for (final UnitType u : utMap.keySet()) {
      units.addAll(u.create(utMap.getInt(u), player));
    }
    final CompositeChange change = new CompositeChange();
    // mark no movement
    for (final Unit unit : units) {
      change.add(ChangeFactory.markNoMovementChange(unit));
    }
    // place units
    final Collection<Unit> factoryAndInfrastructure = CollectionUtils.getMatches(units, Matches.unitIsInfrastructure());
    change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, player));
    final String transcriptText = MyFormatter.attachmentNameToText(t.getName()) + ": " + player.getName() + " has "
        + MyFormatter.unitsToTextNoOwner(units) + " placed in " + terr.getName();
    bridge.getHistoryWriter().startEvent(transcriptText, units);
    final Change place = ChangeFactory.addUnits(terr, units);
    change.add(place);
    bridge.addChange(change);
  }

  // And now for the actual triggers, as called throughout the engine.
  // Each trigger should be called exactly twice, once in BaseDelegate (for use with 'when'), and a second time as the
  // default location for
  // when 'when' is not used.
  // Should be void.
  public static void triggerNotifications(final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, notificationMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final Set<String> notifications = new HashSet<>();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      if (!notifications.contains(t.getNotification())) {
        notifications.add(t.getNotification());
        final String notificationMessageKey = t.getNotification().trim();
        final String sounds = NotificationMessages.getInstance().getSoundsKey(notificationMessageKey);
        if (sounds != null) {
          // play to observers if we are playing to everyone
          bridge.getSoundChannelBroadcaster().playSoundToPlayers(
              SoundPath.CLIP_TRIGGERED_NOTIFICATION_SOUND + sounds.trim(), t.getPlayers(), null,
              t.getPlayers().containsAll(data.getPlayerList().getPlayers()));
        }
        final String message = NotificationMessages.getInstance().getMessage(notificationMessageKey);
        if (message != null) {
          String messageForRecord = message.trim();
          if (messageForRecord.length() > 190) {
            // We don't want to record a giant string in the history panel, so just put a shortened version in instead.
            messageForRecord = messageForRecord.replaceAll("\\<br.*?>", " ");
            messageForRecord = messageForRecord.replaceAll("\\<.*?>", "");
            if (messageForRecord.length() > 195) {
              messageForRecord = messageForRecord.substring(0, 190) + "....";
            }
          }
          bridge.getHistoryWriter().startEvent(
              "Note to players " + MyFormatter.defaultNamedToTextList(t.getPlayers()) + ": " + messageForRecord);
          ((ITripleADisplay) bridge.getDisplayChannelBroadcaster()).reportMessageToPlayers(t.getPlayers(), null,
              ("<html>" + message.trim() + "</html>"), NOTIFICATION);
        }
      }
    }
  }

  public static void triggerPlayerPropertyChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, playerPropertyMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getPlayerProperty()) {
        for (final PlayerID player : t.getPlayers()) {
          String newValue = property.getSecond();
          boolean clearFirst = false;
          // test if we are resetting the variable first, and if so, remove the leading "-reset-" or "-clear-"
          if (newValue.length() > 0 && (newValue.startsWith(PREFIX_CLEAR) || newValue.startsWith(PREFIX_RESET))) {
            newValue = newValue.replaceFirst(PREFIX_CLEAR, "").replaceFirst(PREFIX_RESET, "");
            clearFirst = true;
          }
          // covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment
          if (t.getPlayerAttachmentName().getFirst().equals("PlayerAttachment")) {
            final PlayerAttachment attachment = PlayerAttachment.get(player, t.getPlayerAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getPlayerAttachmentName().getSecond() + " attached to " + player.getName());
          } else if (t.getPlayerAttachmentName().getFirst().equals("RulesAttachment")) {
            final RulesAttachment attachment = RulesAttachment.get(player, t.getPlayerAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getPlayerAttachmentName().getSecond() + " attached to " + player.getName());
          } else if (t.getPlayerAttachmentName().getFirst().equals("TriggerAttachment")) {
            final TriggerAttachment attachment =
                TriggerAttachment.get(player, t.getPlayerAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getPlayerAttachmentName().getSecond() + " attached to " + player.getName());
          } else if (t.getPlayerAttachmentName().getFirst().equals("TechAttachment")) {
            final TechAttachment attachment = TechAttachment.get(player, t.getPlayerAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getPlayerAttachmentName().getSecond() + " attached to " + player.getName());
          } else if (t.getPlayerAttachmentName().getFirst().equals("PoliticalActionAttachment")) {
            final PoliticalActionAttachment attachment =
                PoliticalActionAttachment.get(player, t.getPlayerAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getPlayerAttachmentName().getSecond() + " attached to " + player.getName());
          } else if (t.getPlayerAttachmentName().getFirst().equals("UserActionAttachment")) {
            final UserActionAttachment attachment =
                UserActionAttachment.get(player, t.getPlayerAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getPlayerAttachmentName().getSecond() + " attached to " + player.getName());
          }
          // TODO add other attachment changes here if they attach to a player
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  public static void triggerRelationshipTypePropertyChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs =
        CollectionUtils.getMatches(satisfiedTriggers, relationshipTypePropertyMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getRelationshipTypeProperty()) {
        for (final RelationshipType relationshipType : t.getRelationshipTypes()) {
          String newValue = property.getSecond();
          boolean clearFirst = false;
          // test if we are resetting the variable first, and if so, remove the leading "-reset-" or "-clear-"
          if (newValue.length() > 0 && (newValue.startsWith(PREFIX_CLEAR) || newValue.startsWith(PREFIX_RESET))) {
            newValue = newValue.replaceFirst(PREFIX_CLEAR, "").replaceFirst(PREFIX_RESET, "");
            clearFirst = true;
          }
          // covers RelationshipTypeAttachment
          if (t.getRelationshipTypeAttachmentName().getFirst().equals("RelationshipTypeAttachment")) {
            final RelationshipTypeAttachment attachment =
                RelationshipTypeAttachment.get(relationshipType, t.getRelationshipTypeAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting "
                + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                + t.getRelationshipTypeAttachmentName().getSecond() + " attached to " + relationshipType.getName());
          }
          // TODO add other attachment changes here if they attach to a territory
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  public static void triggerTerritoryPropertyChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, territoryPropertyMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    final HashSet<Territory> territoriesNeedingReDraw = new HashSet<>();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getTerritoryProperty()) {
        for (final Territory territory : t.getTerritories()) {
          territoriesNeedingReDraw.add(territory);
          String newValue = property.getSecond();
          boolean clearFirst = false;
          // test if we are resetting the variable first, and if so, remove the leading "-reset-" or "-clear-"
          if (newValue.length() > 0 && (newValue.startsWith(PREFIX_CLEAR) || newValue.startsWith(PREFIX_RESET))) {
            newValue = newValue.replaceFirst(PREFIX_CLEAR, "").replaceFirst(PREFIX_RESET, "");
            clearFirst = true;
          }
          // covers TerritoryAttachment, CanalAttachment
          if (t.getTerritoryAttachmentName().getFirst().equals("TerritoryAttachment")) {
            final TerritoryAttachment attachment =
                TerritoryAttachment.get(territory, t.getTerritoryAttachmentName().getSecond());
            if (attachment == null) {
              // water territories may not have an attachment, so this could be null
              throw new IllegalStateException("Triggers: No territory attachment for:" + territory.getName());
            }
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getTerritoryAttachmentName().getSecond() + " attached to " + territory.getName());
          } else if (t.getTerritoryAttachmentName().getFirst().equals("CanalAttachment")) {
            final CanalAttachment attachment =
                CanalAttachment.get(territory, t.getTerritoryAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getTerritoryAttachmentName().getSecond() + " attached to " + territory.getName());
          }
          // TODO add other attachment changes here if they attach to a territory
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
      for (final Territory territory : territoriesNeedingReDraw) {
        territory.notifyAttachmentChanged();
      }
    }
  }

  public static void triggerTerritoryEffectPropertyChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, territoryEffectPropertyMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getTerritoryEffectProperty()) {
        for (final TerritoryEffect territoryEffect : t.getTerritoryEffects()) {
          String newValue = property.getSecond();
          boolean clearFirst = false;
          // test if we are resetting the variable first, and if so, remove the leading "-reset-" or "-clear-"
          if (newValue.length() > 0 && (newValue.startsWith(PREFIX_CLEAR) || newValue.startsWith(PREFIX_RESET))) {
            newValue = newValue.replaceFirst(PREFIX_CLEAR, "").replaceFirst(PREFIX_RESET, "");
            clearFirst = true;
          }
          // covers TerritoryEffectAttachment
          if (t.getTerritoryEffectAttachmentName().getFirst().equals("TerritoryEffectAttachment")) {
            final TerritoryEffectAttachment attachment =
                TerritoryEffectAttachment.get(territoryEffect, t.getTerritoryEffectAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getTerritoryEffectAttachmentName().getSecond() + " attached to " + territoryEffect.getName());
          }
          // TODO add other attachment changes here if they attach to a territory
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  public static void triggerUnitPropertyChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, unitPropertyMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getUnitProperty()) {
        for (final UnitType unitType : t.getUnitType()) {
          String newValue = property.getSecond();
          boolean clearFirst = false;
          // test if we are resetting the variable first, and if so, remove the leading "-reset-" or "-clear-"
          if (newValue.length() > 0 && (newValue.startsWith(PREFIX_CLEAR) || newValue.startsWith(PREFIX_RESET))) {
            newValue = newValue.replaceFirst(PREFIX_CLEAR, "").replaceFirst(PREFIX_RESET, "");
            clearFirst = true;
          }
          // covers UnitAttachment, UnitSupportAttachment
          if (t.getUnitAttachmentName().getFirst().equals("UnitAttachment")) {
            final UnitAttachment attachment = UnitAttachment.get(unitType, t.getUnitAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getUnitAttachmentName().getSecond() + " attached to " + unitType.getName());
          } else if (t.getUnitAttachmentName().getFirst().equals("UnitSupportAttachment")) {
            final UnitSupportAttachment attachment =
                UnitSupportAttachment.get(unitType, t.getUnitAttachmentName().getSecond());
            if (newValue.equals(attachment.getRawPropertyString(property.getFirst()))) {
              continue;
            }
            if (clearFirst && newValue.length() < 1) {
              change.add(ChangeFactory.attachmentPropertyReset(attachment, property.getFirst()));
            } else {
              change.add(
                  ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), clearFirst));
            }
            bridge.getHistoryWriter()
                .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst()
                    + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
                    + t.getUnitAttachmentName().getSecond() + " attached to " + unitType.getName());
          }
          // TODO add other attachment changes here if they attach to a unitType
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  public static void triggerRelationshipChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, relationshipChangeMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final String relationshipChange : t.getRelationshipChange()) {
        final String[] s = relationshipChange.split(":");
        final PlayerID player1 = data.getPlayerList().getPlayerId(s[0]);
        final PlayerID player2 = data.getPlayerList().getPlayerId(s[1]);
        final RelationshipType currentRelation = data.getRelationshipTracker().getRelationshipType(player1, player2);
        if (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY)
            || (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
                && Matches.relationshipTypeIsNeutral().test(currentRelation))
            || (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
                && Matches.relationshipTypeIsAllied().test(currentRelation))
            || (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
                && Matches.relationshipTypeIsAtWar().test(currentRelation))
            || currentRelation.equals(data.getRelationshipTypeList().getRelationshipType(s[2]))) {
          final RelationshipType triggerNewRelation = data.getRelationshipTypeList().getRelationshipType(s[3]);
          change.add(ChangeFactory.relationshipChange(player1, player2, currentRelation, triggerNewRelation));
          bridge.getHistoryWriter()
              .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": Changing Relationship for "
                  + player1.getName() + " and " + player2.getName() + " from " + currentRelation.getName() + " to "
                  + triggerNewRelation.getName());
          AbstractMoveDelegate.getBattleTracker(data).addRelationshipChangesThisTurn(player1, player2, currentRelation,
              triggerNewRelation);
          /*
           * creation of new battles is handled at the beginning of the battle delegate, in
           * "setupUnitsInSameTerritoryBattles", not here.
           * if (Matches.relationshipTypeIsAtWar().test(triggerNewRelation))
           * triggerMustFightBattle(player1, player2, aBridge);
           */
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  public static void triggerAvailableTechChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, techAvailableMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final PlayerID player : t.getPlayers()) {
        for (final String cat : t.getAvailableTech().keySet()) {
          final TechnologyFrontier tf = player.getTechnologyFrontierList().getTechnologyFrontier(cat);
          if (tf == null) {
            throw new IllegalStateException("Triggers: tech category doesn't exist:" + cat + " for player:" + player);
          }
          for (final TechAdvance ta : t.getAvailableTech().get(cat).keySet()) {
            if (t.getAvailableTech().get(cat).get(ta)) {
              bridge.getHistoryWriter().startEvent(
                  MyFormatter.attachmentNameToText(t.getName()) + ": " + player.getName() + " gains access to " + ta);
              final Change change = ChangeFactory.addAvailableTech(tf, ta, player);
              bridge.addChange(change);
            } else {
              bridge.getHistoryWriter().startEvent(
                  MyFormatter.attachmentNameToText(t.getName()) + ": " + player.getName() + " loses access to " + ta);
              final Change change = ChangeFactory.removeAvailableTech(tf, ta, player);
              bridge.addChange(change);
            }
          }
        }
      }
    }
  }

  public static void triggerTechChange(final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    // final GameData data = aBridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, techMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final PlayerID player : t.getPlayers()) {
        for (final TechAdvance ta : t.getTech()) {
          if (ta.hasTech(TechAttachment.get(player))) {
            continue;
          }
          bridge.getHistoryWriter().startEvent(
              MyFormatter.attachmentNameToText(t.getName()) + ": " + player.getName() + " activates " + ta);
          TechTracker.addAdvance(player, bridge, ta);
        }
      }
    }
  }

  public static void triggerProductionChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, prodMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final PlayerID player : t.getPlayers()) {
        change.add(ChangeFactory.changeProductionFrontier(player, t.getFrontier()));
        bridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + player.getName()
            + " has their production frontier changed to: " + t.getFrontier().getName());
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  public static void triggerProductionFrontierEditChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, prodFrontierEditMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment triggerAttachment : trigs) {
      if (testChance && !triggerAttachment.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        triggerAttachment.use(bridge);
      }
      triggerAttachment.getProductionRule().stream()
          .map(s -> s.split(":"))
          .forEach(array -> {
            final ProductionFrontier front = data.getProductionFrontierList().getProductionFrontier(array[0]);
            final String rule = array[1];
            final String ruleName = rule.replaceFirst("^-", "");
            final ProductionRule productionRule = data.getProductionRuleList().getProductionRule(ruleName);
            final boolean ruleAdded = !rule.startsWith("-");
            if (ruleAdded) {
              if (!front.getRules().contains(productionRule)) {
                change.add(ChangeFactory.addProductionRule(productionRule, front));
                bridge.getHistoryWriter()
                    .startEvent(MyFormatter.attachmentNameToText(triggerAttachment.getName()) + ": "
                        + productionRule.getName() + " added to " + front.getName());
              }
            } else {
              if (front.getRules().contains(productionRule)) {
                change.add(ChangeFactory.removeProductionRule(productionRule, front));
                bridge.getHistoryWriter()
                    .startEvent(MyFormatter.attachmentNameToText(triggerAttachment.getName()) + ": "
                        + productionRule.getName() + " removed from " + front.getName());
              }
            }
          });
    }
    if (!change.isEmpty()) {
      bridge.addChange(change); // TODO: we should sort the frontier list if we make changes to it...
    }
  }

  public static void triggerSupportChange(final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, supportMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final PlayerID player : t.getPlayers()) {
        for (final String usaString : t.getSupport().keySet()) {
          UnitSupportAttachment usa = null;
          for (final UnitSupportAttachment support : UnitSupportAttachment.get(data)) {
            if (support.getName().equals(usaString)) {
              usa = support;
              break;
            }
          }
          if (usa == null) {
            throw new IllegalStateException("Could not find unitSupportAttachment. name:" + usaString);
          }
          final List<PlayerID> p = new ArrayList<>(usa.getPlayers());
          if (p.contains(player)) {
            if (!t.getSupport().get(usa.getName())) {
              p.remove(player);
              change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
              bridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": "
                  + player.getName() + " is removed from " + usa.toString());
            }
          } else {
            if (t.getSupport().get(usa.getName())) {
              p.add(player);
              change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
              bridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": "
                  + player.getName() + " is added to " + usa.toString());
            }
          }
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  public static void triggerChangeOwnership(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, changeOwnershipMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final BattleTracker bt = DelegateFinder.battleDelegate(data).getBattleTracker();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      for (final String value : t.getChangeOwnership()) {
        final String[] s = value.split(":");
        final Collection<Territory> territories = new ArrayList<>();
        if (s[0].equalsIgnoreCase("all")) {
          territories.addAll(data.getMap().getTerritories());
        } else {
          final Territory territorySet = data.getMap().getTerritory(s[0]);
          territories.add(territorySet);
        }
        // if null, then is must be "any", so then any player
        final PlayerID oldOwner = data.getPlayerList().getPlayerId(s[1]);
        final PlayerID newOwner = data.getPlayerList().getPlayerId(s[2]);
        final boolean captured = getBool(s[3]);
        for (final Territory terr : territories) {
          final PlayerID currentOwner = terr.getOwner();
          if (TerritoryAttachment.get(terr) == null) {
            continue; // any territory that has no territory attachment should definitely not be changed
          }
          if (oldOwner != null && !oldOwner.equals(currentOwner)) {
            continue;
          }
          bridge.getHistoryWriter()
              .startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + newOwner.getName()
                  + (captured ? " captures territory " : " takes ownership of territory ") + terr.getName());
          if (!captured) {
            bridge.addChange(ChangeFactory.changeOwner(terr, newOwner));
          } else {
            bt.takeOver(terr, newOwner, bridge, null, null);
          }
        }
      }
    }
  }

  public static void triggerPurchase(final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, purchaseMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final PlayerID player : t.getPlayers()) {
        for (int i = 0; i < eachMultiple; ++i) {
          final List<Unit> units = new ArrayList<>();
          for (final UnitType u : t.getPurchase().keySet()) {
            units.addAll(u.create(t.getPurchase().getInt(u), player));
          }
          if (!units.isEmpty()) {
            final String transcriptText = MyFormatter.attachmentNameToText(t.getName()) + ": "
                + MyFormatter.unitsToTextNoOwner(units) + " gained by " + player;
            bridge.getHistoryWriter().startEvent(transcriptText, units);
            final Change place = ChangeFactory.addUnits(player, units);
            bridge.addChange(place);
          }
        }
      }
    }
  }

  public static void triggerUnitRemoval(final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, removeUnitsMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final PlayerID player : t.getPlayers()) {
        for (final Territory ter : t.getRemoveUnits().keySet()) {
          for (int i = 0; i < eachMultiple; ++i) {
            removeUnits(t, ter, t.getRemoveUnits().get(ter), player, bridge);
          }
        }
      }
    }
  }

  public static void triggerUnitPlacement(final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, placeMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final PlayerID player : t.getPlayers()) {
        for (final Territory ter : t.getPlacement().keySet()) {
          for (int i = 0; i < eachMultiple; ++i) {
            placeUnits(t, ter, t.getPlacement().get(ter), player, bridge);
          }
        }
      }
    }
  }

  /**
   * Returns projected resource income for satisfied triggers. Should pass dummy bridge in.
   */
  public static IntegerMap<Resource> findResourceIncome(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge) {
    return triggerResourceChange(satisfiedTriggers, bridge, null, null, true, true, true, true, new StringBuilder());
  }

  /**
   * Triggers all resource changes based on satisfied triggers and returns string summary of the changes.
   */
  public static String triggerResourceChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen) {
    final StringBuilder endOfTurnReport = new StringBuilder();
    triggerResourceChange(satisfiedTriggers, bridge, beforeOrAfter, stepName, useUses, testUses, testChance, testWhen,
        endOfTurnReport);
    return endOfTurnReport.toString();
  }

  private static IntegerMap<Resource> triggerResourceChange(final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge, final String beforeOrAfter, final String stepName, final boolean useUses,
      final boolean testUses, final boolean testChance, final boolean testWhen, final StringBuilder endOfTurnReport) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, resourceMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    final IntegerMap<Resource> resources = new IntegerMap<>();
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final PlayerID player : t.getPlayers()) {
        for (int i = 0; i < eachMultiple; ++i) {
          int toAdd = t.getResourceCount();
          if (t.getResource().equals(Constants.PUS)) {
            toAdd *= Properties.getPuMultiplier(data);
          }
          resources.add(data.getResourceList().getResource(t.getResource()), toAdd);
          int total = player.getResources().getQuantity(t.getResource()) + toAdd;
          if (total < 0) {
            toAdd -= total;
            total = 0;
          }
          bridge.addChange(
              ChangeFactory.changeResourcesChange(player, data.getResourceList().getResource(t.getResource()), toAdd));
          final String puMessage = MyFormatter.attachmentNameToText(t.getName()) + ": " + player.getName()
              + " met a national objective for an additional " + t.getResourceCount() + " " + t.getResource()
              + "; end with " + total + " " + t.getResource();
          bridge.getHistoryWriter().startEvent(puMessage);
          endOfTurnReport.append(puMessage).append(" <br />");
        }
      }
    }
    return resources;
  }

  public static void triggerActivateTriggerOther(final HashMap<ICondition, Boolean> testedConditionsSoFar,
      final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge, final String beforeOrAfter,
      final String stepName, final boolean useUses, final boolean testUses, final boolean testChance,
      final boolean testWhen) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, activateTriggerMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final Tuple<String, String> tuple : t.getActivateTrigger()) {
        // numberOfTimes:useUses:testUses:testConditions:testChance
        TriggerAttachment toFire = null;
        for (final PlayerID player : data.getPlayerList().getPlayers()) {
          for (final TriggerAttachment ta : TriggerAttachment.getTriggers(player, null)) {
            if (ta.getName().equals(tuple.getFirst())) {
              toFire = ta;
              break;
            }
          }
          if (toFire != null) {
            break;
          }
        }
        final HashSet<TriggerAttachment> toFireSet = new HashSet<>();
        toFireSet.add(toFire);
        final String[] options = tuple.getSecond().split(":");
        final int numberOfTimesToFire = getInt(options[0]);
        final boolean useUsesToFire = getBool(options[1]);
        final boolean testUsesToFire = getBool(options[2]);
        final boolean testConditionsToFire = getBool(options[3]);
        final boolean testChanceToFire = getBool(options[4]);
        if (testConditionsToFire) {
          if (!testedConditionsSoFar.containsKey(toFire)) {
            // this should directly add the new tests to testConditionsToFire...
            collectTestsForAllTriggers(toFireSet, bridge, new HashSet<>(testedConditionsSoFar.keySet()),
                testedConditionsSoFar);
          }
          if (!isSatisfiedMatch(testedConditionsSoFar).test(toFire)) {
            continue;
          }
        }
        for (int i = 0; i < numberOfTimesToFire * eachMultiple; ++i) {
          bridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName())
              + " activates a trigger called: " + MyFormatter.attachmentNameToText(toFire.getName()));
          fireTriggers(toFireSet, testedConditionsSoFar, bridge, beforeOrAfter, stepName, useUsesToFire,
              testUsesToFire, testChanceToFire, false);
        }
      }
    }
  }

  public static void triggerVictory(final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge,
      final String beforeOrAfter, final String stepName, final boolean useUses, final boolean testUses,
      final boolean testChance, final boolean testWhen) {
    final GameData data = bridge.getData();
    Collection<TriggerAttachment> trigs = CollectionUtils.getMatches(satisfiedTriggers, victoryMatch());
    if (testWhen) {
      trigs = CollectionUtils.getMatches(trigs, whenOrDefaultMatch(beforeOrAfter, stepName));
    }
    if (testUses) {
      trigs = CollectionUtils.getMatches(trigs, availableUses);
    }
    for (final TriggerAttachment t : trigs) {
      if (testChance && !t.testChance(bridge)) {
        continue;
      }
      if (useUses) {
        t.use(bridge);
      }
      if (t.getVictory() == null || t.getPlayers() == null) {
        continue;
      }
      final String victoryMessage = NotificationMessages.getInstance().getMessage(t.getVictory().trim());
      final String sounds = NotificationMessages.getInstance().getSoundsKey(t.getVictory().trim());
      if (victoryMessage != null) {
        if (sounds != null) { // only play the sound if we are also notifying everyone
          bridge.getSoundChannelBroadcaster().playSoundToPlayers(
              SoundPath.CLIP_TRIGGERED_VICTORY_SOUND + sounds.trim(), t.getPlayers(), null, true);
          bridge.getSoundChannelBroadcaster().playSoundToPlayers(SoundPath.CLIP_TRIGGERED_DEFEAT_SOUND + sounds.trim(),
              data.getPlayerList().getPlayers(), t.getPlayers(), false);
        }
        String messageForRecord = victoryMessage.trim();
        if (messageForRecord.length() > 150) {
          messageForRecord = messageForRecord.replaceAll("\\<br.*?>", " ");
          messageForRecord = messageForRecord.replaceAll("\\<.*?>", "");
          if (messageForRecord.length() > 155) {
            messageForRecord = messageForRecord.substring(0, 150) + "....";
          }
        }
        try {
          bridge.getHistoryWriter().startEvent("Players: " + MyFormatter.defaultNamedToTextList(t.getPlayers())
              + " have just won the game, with this victory: " + messageForRecord);
          final IDelegate delegateEndRound = data.getDelegateList().getDelegate("endRound");
          ((EndRoundDelegate) delegateEndRound).signalGameOver(victoryMessage.trim(), t.getPlayers(), bridge);
        } catch (final Exception e) {
          log.log(Level.SEVERE, "Failed to signal game over", e);
        }
      }
    }
  }

  public static Predicate<TriggerAttachment> prodMatch() {
    return t -> t.getFrontier() != null;
  }

  public static Predicate<TriggerAttachment> prodFrontierEditMatch() {
    return t -> t.getProductionRule() != null && t.getProductionRule().size() > 0;
  }

  public static Predicate<TriggerAttachment> techMatch() {
    return t -> !t.getTech().isEmpty();
  }

  public static Predicate<TriggerAttachment> techAvailableMatch() {
    return t -> t.getAvailableTech() != null;
  }

  public static Predicate<TriggerAttachment> removeUnitsMatch() {
    return t -> t.getRemoveUnits() != null;
  }

  public static Predicate<TriggerAttachment> placeMatch() {
    return t -> t.getPlacement() != null;
  }

  public static Predicate<TriggerAttachment> purchaseMatch() {
    return t -> t.getPurchase() != null;
  }

  public static Predicate<TriggerAttachment> resourceMatch() {
    return t -> t.getResource() != null && t.getResourceCount() != 0;
  }

  public static Predicate<TriggerAttachment> supportMatch() {
    return t -> t.getSupport() != null;
  }

  public static Predicate<TriggerAttachment> changeOwnershipMatch() {
    return t -> !t.getChangeOwnership().isEmpty();
  }

  public static Predicate<TriggerAttachment> unitPropertyMatch() {
    return t -> !t.getUnitType().isEmpty() && t.getUnitProperty() != null;
  }

  public static Predicate<TriggerAttachment> territoryPropertyMatch() {
    return t -> !t.getTerritories().isEmpty() && t.getTerritoryProperty() != null;
  }

  public static Predicate<TriggerAttachment> playerPropertyMatch() {
    return t -> t.getPlayerProperty() != null;
  }

  public static Predicate<TriggerAttachment> relationshipTypePropertyMatch() {
    return t -> !t.getRelationshipTypes().isEmpty() && t.getRelationshipTypeProperty() != null;
  }

  public static Predicate<TriggerAttachment> territoryEffectPropertyMatch() {
    return t -> !t.getTerritoryEffects().isEmpty() && t.getTerritoryEffectProperty() != null;
  }

  public static Predicate<TriggerAttachment> relationshipChangeMatch() {
    return t -> !t.getRelationshipChange().isEmpty();
  }

  public static Predicate<TriggerAttachment> victoryMatch() {
    return t -> t.getVictory() != null && t.getVictory().length() > 0;
  }

  public static Predicate<TriggerAttachment> activateTriggerMatch() {
    return t -> !t.getActivateTrigger().isEmpty();
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("frontier",
            MutableProperty.of(
                this::setFrontier,
                this::setFrontier,
                this::getFrontier,
                this::resetFrontier))
        .put("productionRule",
            MutableProperty.of(
                this::setProductionRule,
                this::setProductionRule,
                this::getProductionRule,
                this::resetProductionRule))
        .put("tech",
            MutableProperty.of(
                this::setTech,
                this::setTech,
                this::getTech,
                this::resetTech))
        .put("availableTech",
            MutableProperty.of(
                this::setAvailableTech,
                this::setAvailableTech,
                this::getAvailableTech,
                this::resetAvailableTech))
        .put("placement",
            MutableProperty.of(
                this::setPlacement,
                this::setPlacement,
                this::getPlacement,
                this::resetPlacement))
        .put("removeUnits",
            MutableProperty.of(
                this::setRemoveUnits,
                this::setRemoveUnits,
                this::getRemoveUnits,
                this::resetRemoveUnits))
        .put("purchase",
            MutableProperty.of(
                this::setPurchase,
                this::setPurchase,
                this::getPurchase,
                this::resetPurchase))
        .put("resource",
            MutableProperty.ofString(
                this::setResource,
                this::getResource,
                this::resetResource))
        .put("resourceCount",
            MutableProperty.of(
                this::setResourceCount,
                this::setResourceCount,
                this::getResourceCount,
                this::resetResourceCount))
        .put("support",
            MutableProperty.of(
                this::setSupport,
                this::setSupport,
                this::getSupport,
                this::resetSupport))
        .put("relationshipChange",
            MutableProperty.of(
                this::setRelationshipChange,
                this::setRelationshipChange,
                this::getRelationshipChange,
                this::resetRelationshipChange))
        .put("victory",
            MutableProperty.ofString(
                this::setVictory,
                this::getVictory,
                this::resetVictory))
        .put("activateTrigger",
            MutableProperty.of(
                this::setActivateTrigger,
                this::setActivateTrigger,
                this::getActivateTrigger,
                this::resetActivateTrigger))
        .put("changeOwnership",
            MutableProperty.of(
                this::setChangeOwnership,
                this::setChangeOwnership,
                this::getChangeOwnership,
                this::resetChangeOwnership))
        .put("unitType",
            MutableProperty.of(
                this::setUnitType,
                this::setUnitType,
                this::getUnitType,
                this::resetUnitType))
        .put("unitAttachmentName",
            MutableProperty.of(
                this::setUnitAttachmentName,
                this::setUnitAttachmentName,
                this::getUnitAttachmentName,
                this::resetUnitAttachmentName))
        .put("unitProperty",
            MutableProperty.of(
                this::setUnitProperty,
                this::setUnitProperty,
                this::getUnitProperty,
                this::resetUnitProperty))
        .put("territories",
            MutableProperty.of(
                this::setTerritories,
                this::setTerritories,
                this::getTerritories,
                this::resetTerritories))
        .put("territoryAttachmentName",
            MutableProperty.of(
                this::setTerritoryAttachmentName,
                this::setTerritoryAttachmentName,
                this::getTerritoryAttachmentName,
                this::resetTerritoryAttachmentName))
        .put("territoryProperty",
            MutableProperty.of(
                this::setTerritoryProperty,
                this::setTerritoryProperty,
                this::getTerritoryProperty,
                this::resetTerritoryProperty))
        .put("players",
            MutableProperty.of(
                this::setPlayers,
                this::setPlayers,
                this::getPlayers,
                this::resetPlayers))
        .put("playerAttachmentName",
            MutableProperty.of(
                this::setPlayerAttachmentName,
                this::setPlayerAttachmentName,
                this::getPlayerAttachmentName,
                this::resetPlayerAttachmentName))
        .put("playerProperty",
            MutableProperty.of(
                this::setPlayerProperty,
                this::setPlayerProperty,
                this::getPlayerProperty,
                this::resetPlayerProperty))
        .put("relationshipTypes",
            MutableProperty.of(
                this::setRelationshipTypes,
                this::setRelationshipTypes,
                this::getRelationshipTypes,
                this::resetRelationshipTypes))
        .put("relationshipTypeAttachmentName",
            MutableProperty.of(
                this::setRelationshipTypeAttachmentName,
                this::setRelationshipTypeAttachmentName,
                this::getRelationshipTypeAttachmentName,
                this::resetRelationshipTypeAttachmentName))
        .put("relationshipTypeProperty",
            MutableProperty.of(
                this::setRelationshipTypeProperty,
                this::setRelationshipTypeProperty,
                this::getRelationshipTypeProperty,
                this::resetRelationshipTypeProperty))
        .put("territoryEffects",
            MutableProperty.of(
                this::setTerritoryEffects,
                this::setTerritoryEffects,
                this::getTerritoryEffects,
                this::resetTerritoryEffects))
        .put("territoryEffectAttachmentName",
            MutableProperty.of(
                this::setTerritoryEffectAttachmentName,
                this::setTerritoryEffectAttachmentName,
                this::getTerritoryEffectAttachmentName,
                this::resetTerritoryEffectAttachmentName))
        .put("territoryEffectProperty",
            MutableProperty.of(
                this::setTerritoryEffectProperty,
                this::setTerritoryEffectProperty,
                this::getTerritoryEffectProperty,
                this::resetTerritoryEffectProperty))
        .build();
  }
}
