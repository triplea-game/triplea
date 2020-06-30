package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Named;
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
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.NotificationMessages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.java.Log;
import org.triplea.java.ObjectUtils;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;
import org.triplea.util.Tuple;

/**
 * An attachment for instances of {@link GamePlayer} that defines actions to be triggered upon
 * various events.
 */
@Log
public class TriggerAttachment extends AbstractTriggerAttachment {
  private static final long serialVersionUID = -3327739180569606093L;
  private static final Map<String, BiFunction<GamePlayer, String, DefaultAttachment>>
      playerPropertyChangeAttachmentNameToAttachmentGetter =
          ImmutableMap.<String, BiFunction<GamePlayer, String, DefaultAttachment>>builder()
              .put("PlayerAttachment", PlayerAttachment::get)
              .put("RulesAttachment", RulesAttachment::get)
              .put("TriggerAttachment", TriggerAttachment::get)
              .put("TechAttachment", TechAttachment::get)
              .put("PoliticalActionAttachment", PoliticalActionAttachment::get)
              .put("UserActionAttachment", UserActionAttachment::get)
              .build();
  private static final Map<String, BiFunction<UnitType, String, DefaultAttachment>>
      unitPropertyChangeAttachmentNameToAttachmentGetter =
          ImmutableMap.<String, BiFunction<UnitType, String, DefaultAttachment>>builder()
              .put("UnitAttachment", UnitAttachment::get)
              .put("UnitSupportAttachment", UnitSupportAttachment::get)
              .build();

  // Matches prefixes of "-clear-" and "-reset-". Non-capture-group.
  private static final Pattern clearFirstNewValueRegex = Pattern.compile("^-(:?clear|reset)-");

  private ProductionFrontier frontier = null;
  private List<String> productionRule = null;
  private List<TechAdvance> tech = new ArrayList<>();
  private Map<String, Map<TechAdvance, Boolean>> availableTech = null;
  private Map<Territory, IntegerMap<UnitType>> placement = null;
  private Map<Territory, IntegerMap<UnitType>> removeUnits = null;
  private IntegerMap<UnitType> purchase = null;
  private String resource = null;
  private int resourceCount = 0;
  // never use a map of other attachments, inside of an attachment. java will not be able to
  // deserialize it.
  private Map<String, Boolean> support = null;
  // List of relationshipChanges that should be executed when this trigger hits.
  private List<String> relationshipChange = new ArrayList<>();
  private String victory = null;
  private List<Tuple<String, String>> activateTrigger = new ArrayList<>();
  private List<String> changeOwnership = new ArrayList<>();
  // raw property changes below:
  private List<UnitType> unitTypes = new ArrayList<>();
  private Tuple<String, String> unitAttachmentName =
      null; // covers UnitAttachment, UnitSupportAttachment
  private List<Tuple<String, String>> unitProperty = null;
  private List<Territory> territories = new ArrayList<>();
  private Tuple<String, String> territoryAttachmentName =
      null; // covers TerritoryAttachment, CanalAttachment
  private List<Tuple<String, String>> territoryProperty = null;
  private List<GamePlayer> players = new ArrayList<>();
  // covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment,
  // UserActionAttachment
  private Tuple<String, String> playerAttachmentName = null;
  private List<Tuple<String, String>> playerProperty = null;
  private List<RelationshipType> relationshipTypes = new ArrayList<>();
  private Tuple<String, String> relationshipTypeAttachmentName =
      null; // covers RelationshipTypeAttachment
  private List<Tuple<String, String>> relationshipTypeProperty = null;
  private List<TerritoryEffect> territoryEffects = new ArrayList<>();
  private Tuple<String, String> territoryEffectAttachmentName =
      null; // covers TerritoryEffectAttachment
  private List<Tuple<String, String>> territoryEffectProperty = null;

  public TriggerAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Convenience method for returning TriggerAttachments.
   *
   * @return a new trigger attachment
   */
  public static TriggerAttachment get(final GamePlayer player, final String nameOfAttachment) {
    return get(player, nameOfAttachment, null);
  }

  static TriggerAttachment get(
      final GamePlayer player,
      final String nameOfAttachment,
      final Collection<GamePlayer> playersToSearch) {
    TriggerAttachment ta = (TriggerAttachment) player.getAttachment(nameOfAttachment);
    if (ta == null) {
      if (playersToSearch == null) {
        throw new IllegalStateException(
            "Triggers: No trigger attachment for:"
                + player.getName()
                + " with name: "
                + nameOfAttachment);
      }

      for (final GamePlayer otherPlayer : playersToSearch) {
        if (otherPlayer.equals(player)) {
          continue;
        }
        ta = (TriggerAttachment) otherPlayer.getAttachment(nameOfAttachment);
        if (ta != null) {
          return ta;
        }
      }
      throw new IllegalStateException(
          "Triggers: No trigger attachment for:"
              + player.getName()
              + " with name: "
              + nameOfAttachment);
    }
    return ta;
  }

  /**
   * Convenience method for return all TriggerAttachments attached to a player.
   *
   * @return set of trigger attachments (If you use null for the match condition, you will get all
   *     triggers for this player)
   */
  static Set<TriggerAttachment> getTriggers(
      final GamePlayer player, final Predicate<TriggerAttachment> cond) {
    final Set<TriggerAttachment> trigs = new HashSet<>();
    for (final IAttachment a : player.getAttachments().values()) {
      if (a instanceof TriggerAttachment && (cond == null || cond.test((TriggerAttachment) a))) {
        trigs.add((TriggerAttachment) a);
      }
    }
    return trigs;
  }

  /**
   * This will collect all triggers for the desired players, based on a match provided, and then it
   * will gather all the conditions necessary, then test all the conditions, and then it will fire
   * all the conditions which are satisfied.
   */
  public static void collectAndFireTriggers(
      final Set<GamePlayer> players,
      final Predicate<TriggerAttachment> triggerMatch,
      final IDelegateBridge bridge,
      final String beforeOrAfter,
      final String stepName) {
    final Set<TriggerAttachment> toFirePossible =
        collectForAllTriggersMatching(players, triggerMatch);
    if (toFirePossible.isEmpty()) {
      return;
    }
    final Map<ICondition, Boolean> testedConditions =
        collectTestsForAllTriggers(toFirePossible, bridge);
    final List<TriggerAttachment> toFireTestedAndSatisfied =
        CollectionUtils.getMatches(
            toFirePossible, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions));
    if (toFireTestedAndSatisfied.isEmpty()) {
      return;
    }
    TriggerAttachment.fireTriggers(
        new HashSet<>(toFireTestedAndSatisfied),
        testedConditions,
        bridge,
        new FireTriggerParams(beforeOrAfter, stepName, true, true, true, true));
  }

  public static Set<TriggerAttachment> collectForAllTriggersMatching(
      final Set<GamePlayer> players, final Predicate<TriggerAttachment> triggerMatch) {
    final Set<TriggerAttachment> toFirePossible = new HashSet<>();
    for (final GamePlayer player : players) {
      toFirePossible.addAll(TriggerAttachment.getTriggers(player, triggerMatch));
    }
    return toFirePossible;
  }

  public static Map<ICondition, Boolean> collectTestsForAllTriggers(
      final Set<TriggerAttachment> toFirePossible, final IDelegateBridge bridge) {
    return collectTestsForAllTriggers(toFirePossible, bridge, null, null);
  }

  static Map<ICondition, Boolean> collectTestsForAllTriggers(
      final Set<TriggerAttachment> toFirePossible,
      final IDelegateBridge bridge,
      final Set<ICondition> allConditionsNeededSoFar,
      final Map<ICondition, Boolean> allConditionsTestedSoFar) {
    final Set<ICondition> allConditionsNeeded =
        AbstractConditionsAttachment.getAllConditionsRecursive(
            Set.copyOf(toFirePossible), allConditionsNeededSoFar);
    return AbstractConditionsAttachment.testAllConditionsRecursive(
        allConditionsNeeded, allConditionsTestedSoFar, bridge);
  }

  /**
   * This will fire all triggers, and it will not test to see if they are satisfied or not first.
   * Please use collectAndFireTriggers instead of using this directly. To see if they are satisfied,
   * first create the list of triggers using Matches + TriggerAttachment.getTriggers. Then test the
   * triggers using RulesAttachment.getAllConditionsRecursive, and RulesAttachment.testAllConditions
   */
  public static void fireTriggers(
      final Set<TriggerAttachment> triggersToBeFired,
      final Map<ICondition, Boolean> testedConditionsSoFar,
      final IDelegateBridge bridge,
      final FireTriggerParams initialFireTriggerParams) {
    // all triggers at this point have their conditions satisfied so we now test chance (because we
    // test chance last),
    // and remove any conditions that do not succeed in their dice rolls
    final Set<TriggerAttachment> triggersToFire = new HashSet<>();
    for (final TriggerAttachment t : triggersToBeFired) {
      if (initialFireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      triggersToFire.add(t);
    }
    final FireTriggerParams noChanceFireTriggerParams =
        new FireTriggerParams(
            initialFireTriggerParams.beforeOrAfter,
            initialFireTriggerParams.stepName,
            initialFireTriggerParams.useUses,
            initialFireTriggerParams.testUses,
            false,
            initialFireTriggerParams.testWhen);
    // Order: Notifications, Attachment Property Changes (Player, Relationship, Territory,
    // TerritoryEffect, Unit),
    // Relationship, AvailableTech, Tech, ProductionFrontier, ProductionEdit, Support, Purchase,
    // UnitPlacement,
    // Resource, Victory Notifications to current player
    triggerNotifications(triggersToFire, bridge, noChanceFireTriggerParams);
    // Attachment property changes
    triggerPlayerPropertyChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerRelationshipTypePropertyChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerTerritoryPropertyChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerTerritoryEffectPropertyChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerUnitPropertyChange(triggersToFire, bridge, noChanceFireTriggerParams);
    // Misc changes that only need to happen once (twice or more is meaningless)
    triggerRelationshipChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerAvailableTechChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerTechChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerProductionChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerProductionFrontierEditChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerSupportChange(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerChangeOwnership(triggersToFire, bridge, noChanceFireTriggerParams);
    // Misc changes that can happen multiple times, because they add or subtract, something from the
    // game (and therefore
    // can use "each")
    triggerUnitRemoval(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerPurchase(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerUnitPlacement(triggersToFire, bridge, noChanceFireTriggerParams);
    triggerResourceChange(triggersToFire, bridge, noChanceFireTriggerParams);
    // Activating other triggers, and trigger victory, should ALWAYS be LAST in this list!
    // Triggers firing other triggers
    triggerActivateTriggerOther(
        testedConditionsSoFar, triggersToFire, bridge, noChanceFireTriggerParams);
    // Victory messages and recording of winners
    triggerVictory(triggersToFire, bridge, noChanceFireTriggerParams);
    // for both 'when' and 'activated triggers', we can change the uses now. (for other triggers, we
    // change at end of each round)
    if (initialFireTriggerParams.useUses) {
      setUsesForWhenTriggers(triggersToFire, bridge, true);
    }
  }

  protected static void setUsesForWhenTriggers(
      final Set<TriggerAttachment> triggersToBeFired,
      final IDelegateBridge bridge,
      final boolean useUses) {
    if (!useUses) {
      return;
    }
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment trig : triggersToBeFired) {
      final int currentUses = trig.getUses();
      // we only care about triggers that have WHEN set. Triggers without When set are changed
      // during EndRoundDelegate.
      if (currentUses > 0 && !trig.getWhen().isEmpty()) {
        change.add(
            ChangeFactory.attachmentPropertyChange(
                trig, Integer.toString(currentUses - 1), "uses"));
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
    final String[] s = splitOnColon(value);
    if (s.length != 6) {
      throw new GameParseException(
          "activateTrigger must have 6 parts: triggerName:numberOfTimes:useUses:"
              + "testUses:testConditions:testChance"
              + thisErrorMsg());
    }
    TriggerAttachment trigger = null;
    for (final GamePlayer player : getData().getPlayerList().getPlayers()) {
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
    if (ObjectUtils.referenceEquals(trigger, this)) {
      throw new GameParseException("Cannot have a trigger activate itself!" + thisErrorMsg());
    }
    String options = value;
    options = options.replaceFirst((s[0] + ":"), "");
    final int numberOfTimes = getInt(s[1]);
    if (numberOfTimes < 0) {
      throw new GameParseException(
          "activateTrigger must be positive for the number of times to fire: "
              + s[1]
              + thisErrorMsg());
    }
    getBool(s[2]);
    getBool(s[3]);
    getBool(s[4]);
    getBool(s[5]);
    activateTrigger.add(Tuple.of(s[0], options));
  }

  private void setActivateTrigger(final List<Tuple<String, String>> value) {
    activateTrigger = value;
  }

  private List<Tuple<String, String>> getActivateTrigger() {
    return activateTrigger;
  }

  private void resetActivateTrigger() {
    activateTrigger = new ArrayList<>();
  }

  private void setFrontier(final String s) throws GameParseException {
    if (s == null) {
      frontier = null;
      return;
    }
    final ProductionFrontier front = getData().getProductionFrontierList().getProductionFrontier(s);
    if (front == null) {
      throw new GameParseException("Could not find frontier. name:" + s + thisErrorMsg());
    }
    frontier = front;
  }

  private void setFrontier(final ProductionFrontier value) {
    frontier = value;
  }

  private ProductionFrontier getFrontier() {
    return frontier;
  }

  private void resetFrontier() {
    frontier = null;
  }

  private void setProductionRule(final String prop) throws GameParseException {
    if (prop == null) {
      productionRule = null;
      return;
    }
    final String[] s = splitOnColon(prop);
    if (s.length != 2) {
      throw new GameParseException("Invalid productionRule declaration: " + prop + thisErrorMsg());
    }
    if (productionRule == null) {
      productionRule = new ArrayList<>();
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
    productionRule.add(prop);
  }

  private void setProductionRule(final List<String> value) {
    productionRule = value;
  }

  List<String> getProductionRule() {
    return productionRule;
  }

  private void resetProductionRule() {
    productionRule = null;
  }

  private void setResourceCount(final String s) {
    resourceCount = getInt(s);
  }

  private void setResourceCount(final Integer s) {
    resourceCount = s;
  }

  private int getResourceCount() {
    return resourceCount;
  }

  private void resetResourceCount() {
    resourceCount = 0;
  }

  private void setVictory(final String s) {
    if (s == null) {
      victory = null;
      return;
    }
    victory = s;
  }

  private String getVictory() {
    return victory;
  }

  private void resetVictory() {
    victory = null;
  }

  private void setTech(final String techs) throws GameParseException {
    for (final String subString : splitOnColon(techs)) {
      TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(subString);
      if (ta == null) {
        ta = getData().getTechnologyFrontier().getAdvanceByName(subString);
      }
      if (ta == null) {
        throw new GameParseException("Technology not found :" + subString + thisErrorMsg());
      }
      tech.add(ta);
    }
  }

  private void setTech(final List<TechAdvance> value) {
    tech = value;
  }

  private List<TechAdvance> getTech() {
    return tech;
  }

  private void resetTech() {
    tech = new ArrayList<>();
  }

  private void setAvailableTech(final String techs) throws GameParseException {
    if (techs == null) {
      availableTech = null;
      return;
    }
    final String[] s = splitOnColon(techs);
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
    if (availableTech == null) {
      availableTech = new HashMap<>();
    }
    if (availableTech.containsKey(cat)) {
      tlist.putAll(availableTech.get(cat));
    }
    availableTech.put(cat, tlist);
  }

  private void setAvailableTech(final Map<String, Map<TechAdvance, Boolean>> value) {
    availableTech = value;
  }

  private Map<String, Map<TechAdvance, Boolean>> getAvailableTech() {
    return availableTech;
  }

  private void resetAvailableTech() {
    availableTech = null;
  }

  private void setSupport(final String sup) throws GameParseException {
    if (sup == null) {
      support = null;
      return;
    }
    final String[] s = splitOnColon(sup);
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
          if (this.support == null) {
            this.support = new LinkedHashMap<>();
          }
          this.support.put(s[i], add);
          break;
        }
      }
      if (!found) {
        throw new GameParseException(
            "Could not find unitSupportAttachment. name:" + s[i] + thisErrorMsg());
      }
    }
  }

  private void setSupport(final Map<String, Boolean> value) {
    support = value;
  }

  private Map<String, Boolean> getSupport() {
    return support;
  }

  private void resetSupport() {
    support = null;
  }

  private void setResource(final String s) throws GameParseException {
    if (s == null) {
      resource = null;
      return;
    }
    final Resource r = getData().getResourceList().getResource(s);
    if (r == null) {
      throw new GameParseException("Invalid resource: " + s + thisErrorMsg());
    }
    resource = s;
  }

  private String getResource() {
    return resource;
  }

  private void resetResource() {
    resource = null;
  }

  private void setRelationshipChange(final String relChange) throws GameParseException {
    final String[] s = splitOnColon(relChange);
    if (s.length != 4) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n Use: player1:player2:oldRelation:newRelation\n"
              + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[0]) == null) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n player: "
              + s[0]
              + " unknown "
              + thisErrorMsg());
    }
    if (getData().getPlayerList().getPlayerId(s[1]) == null) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n player: "
              + s[1]
              + " unknown "
              + thisErrorMsg());
    }
    if (!(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
        || Matches.isValidRelationshipName(getData()).test(s[2]))) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n relationshipType: "
              + s[2]
              + " unknown "
              + thisErrorMsg());
    }
    if (Matches.isValidRelationshipName(getData()).negate().test(s[3])) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n relationshipType: "
              + s[3]
              + " unknown "
              + thisErrorMsg());
    }
    relationshipChange.add(relChange);
  }

  private void setRelationshipChange(final List<String> value) {
    relationshipChange = value;
  }

  private List<String> getRelationshipChange() {
    return relationshipChange;
  }

  private void resetRelationshipChange() {
    relationshipChange = new ArrayList<>();
  }

  private void setUnitType(final String names) throws GameParseException {
    final String[] s = splitOnColon(names);
    for (final String element : s) {
      final UnitType type = getData().getUnitTypeList().getUnitType(element);
      if (type == null) {
        throw new GameParseException("Could not find unitType. name:" + element + thisErrorMsg());
      }
      unitTypes.add(type);
    }
  }

  private void setUnitType(final List<UnitType> value) {
    unitTypes = value;
  }

  private List<UnitType> getUnitType() {
    return unitTypes;
  }

  private void resetUnitType() {
    unitTypes = new ArrayList<>();
  }

  private void setUnitAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      unitAttachmentName = null;
      return;
    }
    final String[] s = splitOnColon(name);
    if (s.length != 2) {
      throw new GameParseException(
          "unitAttachmentName must have 2 entries, the type of attachment and the "
              + "name of the attachment."
              + thisErrorMsg());
    }
    // covers UnitAttachment, UnitSupportAttachment
    if (!(s[1].equals("UnitAttachment") || s[1].equals("UnitSupportAttachment"))) {
      throw new GameParseException(
          "unitAttachmentName value must be UnitAttachment or UnitSupportAttachment"
              + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException(
          "unitAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("UnitAttachment") && !s[0].startsWith(Constants.UNIT_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("UnitSupportAttachment")
        && !s[0].startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    unitAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setUnitAttachmentName(final Tuple<String, String> value) {
    unitAttachmentName = value;
  }

  private Tuple<String, String> getUnitAttachmentName() {
    if (unitAttachmentName == null) {
      return Tuple.of("UnitAttachment", Constants.UNIT_ATTACHMENT_NAME);
    }
    return unitAttachmentName;
  }

  private void resetUnitAttachmentName() {
    unitAttachmentName = null;
  }

  private void setUnitProperty(final String prop) {
    if (prop == null) {
      unitProperty = null;
      return;
    }
    final String[] s = splitOnColon(prop);
    if (unitProperty == null) {
      unitProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1];
    unitProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setUnitProperty(final List<Tuple<String, String>> value) {
    unitProperty = value;
  }

  private List<Tuple<String, String>> getUnitProperty() {
    return unitProperty;
  }

  private void resetUnitProperty() {
    unitProperty = null;
  }

  private void setTerritories(final String names) throws GameParseException {
    final String[] s = splitOnColon(names);
    for (final String element : s) {
      final Territory terr = getData().getMap().getTerritory(element);
      if (terr == null) {
        throw new GameParseException("Could not find territory. name:" + element + thisErrorMsg());
      }
      territories.add(terr);
    }
  }

  private void setTerritories(final List<Territory> value) {
    territories = value;
  }

  private List<Territory> getTerritories() {
    return territories;
  }

  private void resetTerritories() {
    territories = new ArrayList<>();
  }

  private void setTerritoryAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      territoryAttachmentName = null;
      return;
    }
    final String[] s = splitOnColon(name);
    if (s.length != 2) {
      throw new GameParseException(
          "territoryAttachmentName must have 2 entries, "
              + "the type of attachment and the name of the attachment."
              + thisErrorMsg());
    }
    // covers TerritoryAttachment, CanalAttachment
    if (!(s[1].equals("TerritoryAttachment") || s[1].equals("CanalAttachment"))) {
      throw new GameParseException(
          "territoryAttachmentName value must be TerritoryAttachment or CanalAttachment"
              + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException(
          "territoryAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("TerritoryAttachment")
        && !s[0].startsWith(Constants.TERRITORY_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("CanalAttachment") && !s[0].startsWith(Constants.CANAL_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    territoryAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setTerritoryAttachmentName(final Tuple<String, String> value) {
    territoryAttachmentName = value;
  }

  private Tuple<String, String> getTerritoryAttachmentName() {
    if (territoryAttachmentName == null) {
      return Tuple.of("TerritoryAttachment", Constants.TERRITORY_ATTACHMENT_NAME);
    }
    return territoryAttachmentName;
  }

  private void resetTerritoryAttachmentName() {
    territoryAttachmentName = null;
  }

  private void setTerritoryProperty(final String prop) {
    if (prop == null) {
      territoryProperty = null;
      return;
    }
    final String[] s = splitOnColon(prop);
    if (territoryProperty == null) {
      territoryProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1];
    territoryProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setTerritoryProperty(final List<Tuple<String, String>> value) {
    territoryProperty = value;
  }

  private List<Tuple<String, String>> getTerritoryProperty() {
    return territoryProperty;
  }

  private void resetTerritoryProperty() {
    territoryProperty = null;
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

  private List<GamePlayer> getPlayers() {
    return players.isEmpty() ? new ArrayList<>(List.of((GamePlayer) getAttachedTo())) : players;
  }

  private void resetPlayers() {
    players = new ArrayList<>();
  }

  private void setPlayerAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      playerAttachmentName = null;
      return;
    }
    final String[] s = splitOnColon(name);
    if (s.length != 2) {
      throw new GameParseException(
          "playerAttachmentName must have 2 entries, the type of attachment and "
              + "the name of the attachment."
              + thisErrorMsg());
    }
    // covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment
    if (!(s[1].equals("PlayerAttachment")
        || s[1].equals("RulesAttachment")
        || s[1].equals("TriggerAttachment")
        || s[1].equals("TechAttachment")
        || s[1].equals("PoliticalActionAttachment")
        || s[1].equals("UserActionAttachment"))) {
      throw new GameParseException(
          "playerAttachmentName value must be PlayerAttachment or RulesAttachment or "
              + "TriggerAttachment or TechAttachment or PoliticalActionAttachment or "
              + "UserActionAttachment"
              + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException(
          "playerAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (s[1].equals("PlayerAttachment") && !s[0].startsWith(Constants.PLAYER_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("RulesAttachment")
        && !(s[0].startsWith(Constants.RULES_ATTACHMENT_NAME)
            || s[0].startsWith(Constants.RULES_OBJECTIVE_PREFIX)
            || s[0].startsWith(Constants.RULES_CONDITION_PREFIX))) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("TriggerAttachment") && !s[0].startsWith(Constants.TRIGGER_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("TechAttachment") && !s[0].startsWith(Constants.TECH_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("PoliticalActionAttachment")
        && !s[0].startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    if (s[1].equals("UserActionAttachment")
        && !s[0].startsWith(Constants.USERACTION_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    playerAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setPlayerAttachmentName(final Tuple<String, String> value) {
    playerAttachmentName = value;
  }

  private Tuple<String, String> getPlayerAttachmentName() {
    if (playerAttachmentName == null) {
      return Tuple.of("PlayerAttachment", Constants.PLAYER_ATTACHMENT_NAME);
    }
    return playerAttachmentName;
  }

  private void resetPlayerAttachmentName() {
    playerAttachmentName = null;
  }

  private void setPlayerProperty(final String prop) {
    if (prop == null) {
      playerProperty = null;
      return;
    }
    final String[] s = splitOnColon(prop);
    if (playerProperty == null) {
      playerProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1];
    playerProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setPlayerProperty(final List<Tuple<String, String>> value) {
    playerProperty = value;
  }

  private List<Tuple<String, String>> getPlayerProperty() {
    return playerProperty;
  }

  private void resetPlayerProperty() {
    playerProperty = null;
  }

  private void setRelationshipTypes(final String names) throws GameParseException {
    final String[] s = splitOnColon(names);
    for (final String element : s) {
      final RelationshipType relation =
          getData().getRelationshipTypeList().getRelationshipType(element);
      if (relation == null) {
        throw new GameParseException(
            "Could not find relationshipType. name:" + element + thisErrorMsg());
      }
      relationshipTypes.add(relation);
    }
  }

  private void setRelationshipTypes(final List<RelationshipType> value) {
    relationshipTypes = value;
  }

  private List<RelationshipType> getRelationshipTypes() {
    return relationshipTypes;
  }

  private void resetRelationshipTypes() {
    relationshipTypes = new ArrayList<>();
  }

  private void setRelationshipTypeAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      relationshipTypeAttachmentName = null;
      return;
    }
    final String[] s = splitOnColon(name);
    if (s.length != 2) {
      throw new GameParseException(
          "relationshipTypeAttachmentName must have 2 entries, the type of attachment and "
              + "the name of the attachment."
              + thisErrorMsg());
    }
    // covers RelationshipTypeAttachment
    if (!s[1].equals("RelationshipTypeAttachment")) {
      throw new GameParseException(
          "relationshipTypeAttachmentName value must be RelationshipTypeAttachment"
              + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException(
          "relationshipTypeAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (!s[0].startsWith(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    relationshipTypeAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setRelationshipTypeAttachmentName(final Tuple<String, String> value) {
    relationshipTypeAttachmentName = value;
  }

  private Tuple<String, String> getRelationshipTypeAttachmentName() {
    if (relationshipTypeAttachmentName == null) {
      return Tuple.of("RelationshipTypeAttachment", Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME);
    }
    return relationshipTypeAttachmentName;
  }

  private void resetRelationshipTypeAttachmentName() {
    relationshipTypeAttachmentName = null;
  }

  private void setRelationshipTypeProperty(final String prop) {
    if (prop == null) {
      relationshipTypeProperty = null;
      return;
    }
    final String[] s = splitOnColon(prop);
    if (relationshipTypeProperty == null) {
      relationshipTypeProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1];
    relationshipTypeProperty.add(
        Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setRelationshipTypeProperty(final List<Tuple<String, String>> value) {
    relationshipTypeProperty = value;
  }

  private List<Tuple<String, String>> getRelationshipTypeProperty() {
    return relationshipTypeProperty;
  }

  private void resetRelationshipTypeProperty() {
    relationshipTypeProperty = null;
  }

  private void setTerritoryEffects(final String names) throws GameParseException {
    final String[] s = splitOnColon(names);
    for (final String element : s) {
      final TerritoryEffect effect = getData().getTerritoryEffectList().get(element);
      if (effect == null) {
        throw new GameParseException(
            "Could not find territoryEffect. name:" + element + thisErrorMsg());
      }
      territoryEffects.add(effect);
    }
  }

  private void setTerritoryEffects(final List<TerritoryEffect> value) {
    territoryEffects = value;
  }

  private List<TerritoryEffect> getTerritoryEffects() {
    return territoryEffects;
  }

  private void resetTerritoryEffects() {
    territoryEffects = new ArrayList<>();
  }

  private void setTerritoryEffectAttachmentName(final String name) throws GameParseException {
    if (name == null) {
      territoryEffectAttachmentName = null;
      return;
    }
    final String[] s = splitOnColon(name);
    if (s.length != 2) {
      throw new GameParseException(
          "territoryEffectAttachmentName must have 2 entries, the type of "
              + "attachment and the name of the attachment."
              + thisErrorMsg());
    }
    // covers TerritoryEffectAttachment
    if (!s[1].equals("TerritoryEffectAttachment")) {
      throw new GameParseException(
          "territoryEffectAttachmentName value must be TerritoryEffectAttachment" + thisErrorMsg());
    }
    // TODO validate attachment exists?
    if (s[0].length() < 1) {
      throw new GameParseException(
          "territoryEffectAttachmentName count must be a valid attachment name" + thisErrorMsg());
    }
    if (!s[0].startsWith(Constants.TERRITORYEFFECT_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named:" + s[0] + thisErrorMsg());
    }
    territoryEffectAttachmentName = Tuple.of(s[1], s[0]);
  }

  private void setTerritoryEffectAttachmentName(final Tuple<String, String> value) {
    territoryEffectAttachmentName = value;
  }

  private Tuple<String, String> getTerritoryEffectAttachmentName() {
    if (territoryEffectAttachmentName == null) {
      return Tuple.of("TerritoryEffectAttachment", Constants.TERRITORYEFFECT_ATTACHMENT_NAME);
    }
    return territoryEffectAttachmentName;
  }

  private void resetTerritoryEffectAttachmentName() {
    territoryEffectAttachmentName = null;
  }

  private void setTerritoryEffectProperty(final String prop) {
    if (prop == null) {
      territoryEffectProperty = null;
      return;
    }
    final String[] s = splitOnColon(prop);
    if (territoryEffectProperty == null) {
      territoryEffectProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1];
    territoryEffectProperty.add(
        Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setTerritoryEffectProperty(final List<Tuple<String, String>> value) {
    territoryEffectProperty = value;
  }

  private List<Tuple<String, String>> getTerritoryEffectProperty() {
    return territoryEffectProperty;
  }

  private void resetTerritoryEffectProperty() {
    territoryEffectProperty = null;
  }

  /** Fudging this, it really represents adding placements. */
  private void setPlacement(final String place) throws GameParseException {
    if (place == null) {
      placement = null;
      return;
    }
    final String[] s = splitOnColon(place);
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
    if (placement == null) {
      placement = new HashMap<>();
    }
    if (placement.containsKey(territory)) {
      map.add(placement.get(territory));
    }
    placement.put(territory, map);
  }

  private void setPlacement(final Map<Territory, IntegerMap<UnitType>> value) {
    placement = value;
  }

  private Map<Territory, IntegerMap<UnitType>> getPlacement() {
    return placement;
  }

  private void resetPlacement() {
    placement = null;
  }

  private void setRemoveUnits(final String value) throws GameParseException {
    if (value == null) {
      removeUnits = null;
      return;
    }
    if (removeUnits == null) {
      removeUnits = new HashMap<>();
    }
    final String[] s = splitOnColon(value);
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
      if (removeUnits.containsKey(t)) {
        map.add(removeUnits.get(t));
      }
      removeUnits.put(t, map);
    }
  }

  private void setRemoveUnits(final Map<Territory, IntegerMap<UnitType>> value) {
    removeUnits = value;
  }

  private Map<Territory, IntegerMap<UnitType>> getRemoveUnits() {
    return removeUnits;
  }

  private void resetRemoveUnits() {
    removeUnits = null;
  }

  private void setPurchase(final String place) throws GameParseException {
    if (place == null) {
      purchase = null;
      return;
    }
    final String[] s = splitOnColon(place);
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

    if (purchase == null) {
      purchase = new IntegerMap<>();
    }
    for (; i < s.length; i++) {
      final UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
      if (type == null) {
        throw new GameParseException("UnitType does not exist " + s[i] + thisErrorMsg());
      }
      purchase.add(type, count);
    }
  }

  private void setPurchase(final IntegerMap<UnitType> value) {
    purchase = value;
  }

  private IntegerMap<UnitType> getPurchase() {
    return purchase;
  }

  private void resetPurchase() {
    purchase = null;
  }

  private void setChangeOwnership(final String value) throws GameParseException {
    // territory:oldOwner:newOwner:booleanConquered
    // can have "all" for territory and "any" for oldOwner
    final String[] s = splitOnColon(value);
    if (s.length < 4) {
      throw new GameParseException(
          "changeOwnership must have 4 fields: territory:oldOwner:newOwner:booleanConquered"
              + thisErrorMsg());
    }
    if (!s[0].equalsIgnoreCase("all")) {
      final Territory t = getData().getMap().getTerritory(s[0]);
      if (t == null) {
        throw new GameParseException("No such territory: " + s[0] + thisErrorMsg());
      }
    }
    if (!s[1].equalsIgnoreCase("any")) {
      final GamePlayer oldOwner = getData().getPlayerList().getPlayerId(s[1]);
      if (oldOwner == null) {
        throw new GameParseException("No such player: " + s[1] + thisErrorMsg());
      }
    }
    final GamePlayer newOwner = getData().getPlayerList().getPlayerId(s[2]);
    if (newOwner == null) {
      throw new GameParseException("No such player: " + s[2] + thisErrorMsg());
    }
    getBool(s[3]);
    changeOwnership.add(value);
  }

  private void setChangeOwnership(final List<String> value) {
    changeOwnership = value;
  }

  private List<String> getChangeOwnership() {
    return changeOwnership;
  }

  private void resetChangeOwnership() {
    changeOwnership = new ArrayList<>();
  }

  private static void removeUnits(
      final TriggerAttachment t,
      final Territory terr,
      final IntegerMap<UnitType> utMap,
      final GamePlayer player,
      final IDelegateBridge bridge) {
    final CompositeChange change = new CompositeChange();
    final Collection<Unit> totalRemoved = new ArrayList<>();
    for (final UnitType ut : utMap.keySet()) {
      final int removeNum = utMap.getInt(ut);
      final Collection<Unit> toRemove =
          CollectionUtils.getNMatches(
              terr.getUnits(),
              removeNum,
              Matches.unitIsOwnedBy(player).and(Matches.unitIsOfType(ut)));
      if (!toRemove.isEmpty()) {
        totalRemoved.addAll(toRemove);
        change.add(ChangeFactory.removeUnits(terr, toRemove));
      }
    }
    if (!change.isEmpty()) {
      final String transcriptText =
          MyFormatter.attachmentNameToText(t.getName())
              + ": has removed "
              + MyFormatter.unitsToTextNoOwner(totalRemoved)
              + " owned by "
              + player.getName()
              + " in "
              + terr.getName();
      bridge.getHistoryWriter().startEvent(transcriptText, totalRemoved);
      bridge.addChange(change);
    }
  }

  private static void placeUnits(
      final TriggerAttachment t,
      final Territory terr,
      final IntegerMap<UnitType> utMap,
      final GamePlayer player,
      final IDelegateBridge bridge) {
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
    final Collection<Unit> factoryAndInfrastructure =
        CollectionUtils.getMatches(units, Matches.unitIsInfrastructure());
    change.add(OriginalOwnerTracker.addOriginalOwnerChange(factoryAndInfrastructure, player));
    final String transcriptText =
        MyFormatter.attachmentNameToText(t.getName())
            + ": "
            + player.getName()
            + " has "
            + MyFormatter.unitsToTextNoOwner(units)
            + " placed in "
            + terr.getName();
    bridge.getHistoryWriter().startEvent(transcriptText, units);
    final Change place = ChangeFactory.addUnits(terr, units);
    change.add(place);
    bridge.addChange(change);
  }

  /**
   * Test if we are resetting/clearing the variable first, and if so, remove the leading "-reset-"
   * or "-clear-" from the new value.
   */
  public static Tuple<Boolean, String> getClearFirstNewValue(final String preNewValue) {
    final Matcher matcher = clearFirstNewValueRegex.matcher(preNewValue);
    final boolean clearFirst = matcher.lookingAt();
    // Remove any leading reset/clear-instruction part.
    final String newValue = matcher.replaceFirst("");
    return Tuple.of(clearFirst, newValue);
  }

  /**
   * Returns the property change as well as history start event message.
   *
   * <p>No side-effects.
   */
  @VisibleForTesting
  static Optional<Tuple<Change, String>> getPropertyChangeHistoryStartEvent(
      final TriggerAttachment triggerAttachment,
      final DefaultAttachment propertyAttachment,
      final String propertyName,
      final Tuple<Boolean, String> clearFirstNewValue,
      final String propertyAttachmentName,
      final Named attachedTo) {
    final boolean clearFirst = clearFirstNewValue.getFirst();
    final String newValue = clearFirstNewValue.getSecond();

    final boolean isValueTheSame;
    try {
      isValueTheSame = newValue.equals(propertyAttachment.getRawPropertyString(propertyName));
    } catch (final UnsupportedOperationException e) {
      throw new UnsupportedOperationException(
          triggerAttachment.getName()
              + ": Unable to get the value for "
              + propertyName
              + " from "
              + propertyAttachmentName, e);
    }

    if (!isValueTheSame) {

      final Change change =
          clearFirst && newValue.isEmpty()
              ? ChangeFactory.attachmentPropertyReset(propertyAttachment, propertyName)
              : ChangeFactory.attachmentPropertyChange(
                  propertyAttachment, newValue, propertyName, clearFirst);

      final String startEvent =
          String.format(
              "%s: Setting %s %s for %s attached to %s",
              MyFormatter.attachmentNameToText(triggerAttachment.getName()),
              propertyName,
              (newValue.isEmpty() ? "cleared" : "to " + newValue),
              propertyAttachmentName,
              attachedTo.getName());

      return Optional.of(Tuple.of(change, startEvent));
    }

    return Optional.empty();
  }

  private static Consumer<Tuple<Change, String>> appendChangeWriteEvent(
      final IDelegateBridge bridge, final CompositeChange compositeChange) {
    return propertyChangeEvent -> {
      compositeChange.add(propertyChangeEvent.getFirst());
      bridge.getHistoryWriter().startEvent(propertyChangeEvent.getSecond());
    };
  }

  /**
   * Filters the given satisfied triggers.
   *
   * <p>No side-effects.
   *
   * @param customPredicate Must have no side-effects.
   */
  private static Collection<TriggerAttachment> filterSatisfiedTriggers(
      final Set<TriggerAttachment> satisfiedTriggers,
      final Predicate<TriggerAttachment> customPredicate,
      final FireTriggerParams fireTriggerParams) {
    final Predicate<TriggerAttachment> combinedPredicate =
        PredicateBuilder.of(customPredicate)
            .andIf(
                fireTriggerParams.testWhen,
                whenOrDefaultMatch(fireTriggerParams.beforeOrAfter, fireTriggerParams.stepName))
            .andIf(fireTriggerParams.testUses, availableUses)
            .build();
    return CollectionUtils.getMatches(satisfiedTriggers, combinedPredicate);
  }

  // And now for the actual triggers, as called throughout the engine.
  // Each trigger should be called exactly twice, once in BaseDelegate (for use with 'when'), and a
  // second time as the
  // default location for when 'when' is not used.
  // Should be void.

  /** Triggers all notifications associated with {@code satisfiedTriggers}. */
  public static void triggerNotifications(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {

    // NOTE: The check is needed to ensure that UI-related code (namely
    // 'NotificationMessages.getInstance()') is
    // not executed when unit testing non-UI related code such as 'MustFightBattleTest'.
    if (satisfiedTriggers.stream().anyMatch(notificationMatch())) {
      triggerNotifications(
          satisfiedTriggers, bridge, fireTriggerParams, NotificationMessages.getInstance());
    }
  }

  @VisibleForTesting
  static void triggerNotifications(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams,
      final NotificationMessages notificationMessages) {

    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, notificationMatch(), fireTriggerParams);
    final Set<String> notifications = new HashSet<>();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      if (!notifications.contains(t.getNotification())) {
        notifications.add(t.getNotification());
        final String notificationMessageKey = t.getNotification().trim();
        final String sounds = notificationMessages.getSoundsKey(notificationMessageKey);
        if (sounds != null) {
          // play to observers if we are playing to everyone
          bridge
              .getSoundChannelBroadcaster()
              .playSoundToPlayers(
                  SoundPath.CLIP_TRIGGERED_NOTIFICATION_SOUND + sounds.trim(),
                  t.getPlayers(),
                  null,
                  t.getPlayers().containsAll(data.getPlayerList().getPlayers()));
        }
        final String message = notificationMessages.getMessage(notificationMessageKey);
        if (message != null) {
          String messageForRecord = message.trim();
          if (messageForRecord.length() > 190) {
            // We don't want to record a giant string in the history panel, so just put a shortened
            // version in instead.
            messageForRecord = messageForRecord.replaceAll("<br.*?>", " ");
            messageForRecord = messageForRecord.replaceAll("<.*?>", "");
            if (messageForRecord.length() > 195) {
              messageForRecord = messageForRecord.substring(0, 190) + "....";
            }
          }
          bridge
              .getHistoryWriter()
              .startEvent(
                  "Note to players "
                      + MyFormatter.defaultNamedToTextList(t.getPlayers())
                      + ": "
                      + messageForRecord);
          bridge
              .getDisplayChannelBroadcaster()
              .reportMessageToPlayers(
                  t.getPlayers(), null, ("<html>" + message.trim() + "</html>"), NOTIFICATION);
        }
      }
    }
  }

  /**
   * Triggers all player property changes associated with {@code satisfiedTriggers}. Only player
   * property changes associated with the following attachments will be triggered: {@link
   * PlayerAttachment}, {@link RulesAttachment}, {@link TriggerAttachment}, {@link TechAttachment},
   * {@link PoliticalActionAttachment}, and {@link UserActionAttachment}.
   */
  public static void triggerPlayerPropertyChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, playerPropertyMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getPlayerProperty()) {
        final Tuple<Boolean, String> clearFirstNewValue =
            getClearFirstNewValue(property.getSecond());

        for (final GamePlayer player : t.getPlayers()) {

          final String attachmentName = t.getPlayerAttachmentName().getFirst();
          if (playerPropertyChangeAttachmentNameToAttachmentGetter.containsKey(attachmentName)) {
            final DefaultAttachment attachment =
                playerPropertyChangeAttachmentNameToAttachmentGetter
                    .get(attachmentName)
                    .apply(player, t.getPlayerAttachmentName().getSecond());

            getPropertyChangeHistoryStartEvent(
                    t,
                    attachment,
                    property.getFirst(),
                    clearFirstNewValue,
                    t.getPlayerAttachmentName().getSecond(),
                    player)
                .ifPresent(appendChangeWriteEvent(bridge, change));
          }
        }
      }
    }

    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  /**
   * Triggers all relationship type property changes associated with {@code satisfiedTriggers}. Only
   * relationship type property changes associated with the following attachments will be triggered:
   * {@link RelationshipTypeAttachment}.
   */
  public static void triggerRelationshipTypePropertyChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(
            satisfiedTriggers, relationshipTypePropertyMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getRelationshipTypeProperty()) {
        final Tuple<Boolean, String> clearFirstNewValue =
            getClearFirstNewValue(property.getSecond());

        for (final RelationshipType relationshipType : t.getRelationshipTypes()) {

          // covers RelationshipTypeAttachment
          if (t.getRelationshipTypeAttachmentName()
              .getFirst()
              .equals("RelationshipTypeAttachment")) {
            final RelationshipTypeAttachment attachment =
                RelationshipTypeAttachment.get(
                    relationshipType, t.getRelationshipTypeAttachmentName().getSecond());

            getPropertyChangeHistoryStartEvent(
                    t,
                    attachment,
                    property.getFirst(),
                    clearFirstNewValue,
                    t.getRelationshipTypeAttachmentName().getSecond(),
                    relationshipType)
                .ifPresent(appendChangeWriteEvent(bridge, change));
          }
          // TODO add other attachment changes here if they attach to a territory
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  /**
   * Triggers all territory property changes associated with {@code satisfiedTriggers}. Only
   * territory property changes associated with the following attachments will be triggered: {@link
   * TerritoryAttachment} and {@link CanalAttachment}.
   */
  public static void triggerTerritoryPropertyChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, territoryPropertyMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    final Set<Territory> territoriesNeedingReDraw = new HashSet<>();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getTerritoryProperty()) {
        final Tuple<Boolean, String> clearFirstNewValue =
            getClearFirstNewValue(property.getSecond());

        for (final Territory territory : t.getTerritories()) {
          territoriesNeedingReDraw.add(territory);

          // covers TerritoryAttachment, CanalAttachment
          if (t.getTerritoryAttachmentName().getFirst().equals("TerritoryAttachment")) {
            final TerritoryAttachment attachment =
                TerritoryAttachment.get(territory, t.getTerritoryAttachmentName().getSecond());
            if (attachment == null) {
              // water territories may not have an attachment, so this could be null
              throw new IllegalStateException(
                  "Triggers: No territory attachment for:" + territory.getName());
            }

            getPropertyChangeHistoryStartEvent(
                    t,
                    attachment,
                    property.getFirst(),
                    clearFirstNewValue,
                    t.getTerritoryAttachmentName().getSecond(),
                    territory)
                .ifPresent(appendChangeWriteEvent(bridge, change));
          } else if (t.getTerritoryAttachmentName().getFirst().equals("CanalAttachment")) {
            final CanalAttachment attachment =
                CanalAttachment.get(territory, t.getTerritoryAttachmentName().getSecond());

            getPropertyChangeHistoryStartEvent(
                    t,
                    attachment,
                    property.getFirst(),
                    clearFirstNewValue,
                    t.getTerritoryAttachmentName().getSecond(),
                    territory)
                .ifPresent(appendChangeWriteEvent(bridge, change));
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

  /**
   * Triggers all territory effect property changes associated with {@code satisfiedTriggers}. Only
   * territory effect property changes associated with the following attachments will be triggered:
   * {@link TerritoryEffectAttachment}.
   */
  public static void triggerTerritoryEffectPropertyChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(
            satisfiedTriggers, territoryEffectPropertyMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getTerritoryEffectProperty()) {
        final Tuple<Boolean, String> clearFirstNewValue =
            getClearFirstNewValue(property.getSecond());

        for (final TerritoryEffect territoryEffect : t.getTerritoryEffects()) {

          // covers TerritoryEffectAttachment
          if (t.getTerritoryEffectAttachmentName().getFirst().equals("TerritoryEffectAttachment")) {
            final TerritoryEffectAttachment attachment =
                TerritoryEffectAttachment.get(
                    territoryEffect, t.getTerritoryEffectAttachmentName().getSecond());

            getPropertyChangeHistoryStartEvent(
                    t,
                    attachment,
                    property.getFirst(),
                    clearFirstNewValue,
                    t.getTerritoryEffectAttachmentName().getSecond(),
                    territoryEffect)
                .ifPresent(appendChangeWriteEvent(bridge, change));
          }
          // TODO add other attachment changes here if they attach to a territory
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  /**
   * Triggers all unit property changes associated with {@code satisfiedTriggers}. Only unit
   * property changes associated with the following attachments will be triggered: {@link
   * UnitAttachment} and {@link UnitSupportAttachment}.
   */
  public static void triggerUnitPropertyChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, unitPropertyMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final Tuple<String, String> property : t.getUnitProperty()) {
        final Tuple<Boolean, String> clearFirstNewValue =
            getClearFirstNewValue(property.getSecond());

        for (final UnitType unitType : t.getUnitType()) {

          final String attachmentName = t.getUnitAttachmentName().getFirst();
          if (unitPropertyChangeAttachmentNameToAttachmentGetter.containsKey(attachmentName)) {
            final DefaultAttachment attachment =
                unitPropertyChangeAttachmentNameToAttachmentGetter
                    .get(attachmentName)
                    .apply(unitType, t.getUnitAttachmentName().getSecond());

            getPropertyChangeHistoryStartEvent(
                    t,
                    attachment,
                    property.getFirst(),
                    clearFirstNewValue,
                    t.getUnitAttachmentName().getSecond(),
                    unitType)
                .ifPresent(appendChangeWriteEvent(bridge, change));
          }
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  /** Triggers all relationship changes associated with {@code satisfiedTriggers}. */
  public static void triggerRelationshipChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, relationshipChangeMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final String relationshipChange : t.getRelationshipChange()) {
        final String[] s = splitOnColon(relationshipChange);
        final GamePlayer player1 = data.getPlayerList().getPlayerId(s[0]);
        final GamePlayer player2 = data.getPlayerList().getPlayerId(s[1]);
        final RelationshipType currentRelation =
            data.getRelationshipTracker().getRelationshipType(player1, player2);
        if (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY)
            || (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
                && Matches.relationshipTypeIsNeutral().test(currentRelation))
            || (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
                && Matches.relationshipTypeIsAllied().test(currentRelation))
            || (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
                && Matches.relationshipTypeIsAtWar().test(currentRelation))
            || currentRelation.equals(data.getRelationshipTypeList().getRelationshipType(s[2]))) {
          final RelationshipType triggerNewRelation =
              data.getRelationshipTypeList().getRelationshipType(s[3]);
          change.add(
              ChangeFactory.relationshipChange(
                  player1, player2, currentRelation, triggerNewRelation));
          bridge
              .getHistoryWriter()
              .startEvent(
                  MyFormatter.attachmentNameToText(t.getName())
                      + ": Changing Relationship for "
                      + player1.getName()
                      + " and "
                      + player2.getName()
                      + " from "
                      + currentRelation.getName()
                      + " to "
                      + triggerNewRelation.getName());
          AbstractMoveDelegate.getBattleTracker(data)
              .addRelationshipChangesThisTurn(
                  player1, player2, currentRelation, triggerNewRelation);
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

  /**
   * Triggers all available technology advance changes associated with {@code satisfiedTriggers}.
   */
  public static void triggerAvailableTechChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, techAvailableMatch(), fireTriggerParams);
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final GamePlayer player : t.getPlayers()) {
        for (final String cat : t.getAvailableTech().keySet()) {
          final TechnologyFrontier tf =
              player.getTechnologyFrontierList().getTechnologyFrontier(cat);
          if (tf == null) {
            throw new IllegalStateException(
                "Triggers: tech category doesn't exist:" + cat + " for player:" + player);
          }
          for (final TechAdvance ta : t.getAvailableTech().get(cat).keySet()) {
            if (t.getAvailableTech().get(cat).get(ta)) {
              bridge
                  .getHistoryWriter()
                  .startEvent(
                      MyFormatter.attachmentNameToText(t.getName())
                          + ": "
                          + player.getName()
                          + " gains access to "
                          + ta);
              final Change change = ChangeFactory.addAvailableTech(tf, ta, player);
              bridge.addChange(change);
            } else {
              bridge
                  .getHistoryWriter()
                  .startEvent(
                      MyFormatter.attachmentNameToText(t.getName())
                          + ": "
                          + player.getName()
                          + " loses access to "
                          + ta);
              final Change change = ChangeFactory.removeAvailableTech(tf, ta, player);
              bridge.addChange(change);
            }
          }
        }
      }
    }
  }

  /** Triggers all technology advance changes associated with {@code satisfiedTriggers}. */
  public static void triggerTechChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, techMatch(), fireTriggerParams);
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final GamePlayer player : t.getPlayers()) {
        for (final TechAdvance ta : t.getTech()) {
          if (ta.hasTech(TechAttachment.get(player))) {
            continue;
          }
          bridge
              .getHistoryWriter()
              .startEvent(
                  MyFormatter.attachmentNameToText(t.getName())
                      + ": "
                      + player.getName()
                      + " activates "
                      + ta);
          TechTracker.addAdvance(player, bridge, ta);
        }
      }
    }
  }

  /** Triggers all production changes associated with {@code satisfiedTriggers}. */
  public static void triggerProductionChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, prodMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final GamePlayer player : t.getPlayers()) {
        change.add(ChangeFactory.changeProductionFrontier(player, t.getFrontier()));
        bridge
            .getHistoryWriter()
            .startEvent(
                MyFormatter.attachmentNameToText(t.getName())
                    + ": "
                    + player.getName()
                    + " has their production frontier changed to: "
                    + t.getFrontier().getName());
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  /** Triggers all production frontier edit changes associated with {@code satisfiedTriggers}. */
  public static void triggerProductionFrontierEditChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, prodFrontierEditMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment triggerAttachment : trigs) {
      if (fireTriggerParams.testChance && !triggerAttachment.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        triggerAttachment.use(bridge);
      }
      triggerAttachment.getProductionRule().stream()
          .map(DefaultAttachment::splitOnColon)
          .forEach(
              array -> {
                final ProductionFrontier front =
                    data.getProductionFrontierList().getProductionFrontier(array[0]);
                final String rule = array[1];
                final String ruleName = rule.replaceFirst("^-", "");
                final ProductionRule productionRule =
                    data.getProductionRuleList().getProductionRule(ruleName);
                final boolean ruleAdded = !rule.startsWith("-");
                if (ruleAdded) {
                  if (!front.getRules().contains(productionRule)) {
                    change.add(ChangeFactory.addProductionRule(productionRule, front));
                    bridge
                        .getHistoryWriter()
                        .startEvent(
                            MyFormatter.attachmentNameToText(triggerAttachment.getName())
                                + ": "
                                + productionRule.getName()
                                + " added to "
                                + front.getName());
                  }
                } else {
                  if (front.getRules().contains(productionRule)) {
                    change.add(ChangeFactory.removeProductionRule(productionRule, front));
                    bridge
                        .getHistoryWriter()
                        .startEvent(
                            MyFormatter.attachmentNameToText(triggerAttachment.getName())
                                + ": "
                                + productionRule.getName()
                                + " removed from "
                                + front.getName());
                  }
                }
              });
    }
    if (!change.isEmpty()) {
      bridge.addChange(
          change); // TODO: we should sort the frontier list if we make changes to it...
    }
  }

  /** Triggers all unit support changes associated with {@code satisfiedTriggers}. */
  public static void triggerSupportChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, supportMatch(), fireTriggerParams);
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final GamePlayer player : t.getPlayers()) {
        for (final String usaString : t.getSupport().keySet()) {
          UnitSupportAttachment usa = null;
          for (final UnitSupportAttachment support : UnitSupportAttachment.get(data)) {
            if (support.getName().equals(usaString)) {
              usa = support;
              break;
            }
          }
          if (usa == null) {
            throw new IllegalStateException(
                "Could not find unitSupportAttachment. name:" + usaString);
          }
          final List<GamePlayer> p = new ArrayList<>(usa.getPlayers());
          if (p.contains(player)) {
            if (!t.getSupport().get(usa.getName())) {
              p.remove(player);
              change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
              bridge
                  .getHistoryWriter()
                  .startEvent(
                      MyFormatter.attachmentNameToText(t.getName())
                          + ": "
                          + player.getName()
                          + " is removed from "
                          + usa.toString());
            }
          } else {
            if (t.getSupport().get(usa.getName())) {
              p.add(player);
              change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
              bridge
                  .getHistoryWriter()
                  .startEvent(
                      MyFormatter.attachmentNameToText(t.getName())
                          + ": "
                          + player.getName()
                          + " is added to "
                          + usa.toString());
            }
          }
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  /** Triggers all territory ownership changes associated with {@code satisfiedTriggers}. */
  public static void triggerChangeOwnership(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, changeOwnershipMatch(), fireTriggerParams);
    final BattleTracker bt = DelegateFinder.battleDelegate(data).getBattleTracker();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final String value : t.getChangeOwnership()) {
        final String[] s = splitOnColon(value);
        final Collection<Territory> territories = new ArrayList<>();
        if (s[0].equalsIgnoreCase("all")) {
          territories.addAll(data.getMap().getTerritories());
        } else {
          final Territory territorySet = data.getMap().getTerritory(s[0]);
          territories.add(territorySet);
        }
        // if null, then is must be "any", so then any player
        final GamePlayer oldOwner = data.getPlayerList().getPlayerId(s[1]);
        final GamePlayer newOwner = data.getPlayerList().getPlayerId(s[2]);
        final boolean captured = getBool(s[3]);
        for (final Territory terr : territories) {
          final GamePlayer currentOwner = terr.getOwner();
          if (TerritoryAttachment.get(terr) == null) {
            continue; // any territory that has no territory attachment should definitely not be
            // changed
          }
          if (oldOwner != null && !oldOwner.equals(currentOwner)) {
            continue;
          }
          bridge
              .getHistoryWriter()
              .startEvent(
                  MyFormatter.attachmentNameToText(t.getName())
                      + ": "
                      + newOwner.getName()
                      + (captured ? " captures territory " : " takes ownership of territory ")
                      + terr.getName());
          if (!captured) {
            bridge.addChange(ChangeFactory.changeOwner(terr, newOwner));
          } else {
            bt.takeOver(terr, newOwner, bridge, null, null);
          }
        }
      }
    }
  }

  /** Triggers all purchase changes associated with {@code satisfiedTriggers}. */
  public static void triggerPurchase(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, purchaseMatch(), fireTriggerParams);
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final GamePlayer player : t.getPlayers()) {
        for (int i = 0; i < eachMultiple; ++i) {
          final List<Unit> units = new ArrayList<>();
          for (final UnitType u : t.getPurchase().keySet()) {
            units.addAll(u.create(t.getPurchase().getInt(u), player));
          }
          if (!units.isEmpty()) {
            final String transcriptText =
                MyFormatter.attachmentNameToText(t.getName())
                    + ": "
                    + MyFormatter.unitsToTextNoOwner(units)
                    + " gained by "
                    + player;
            bridge.getHistoryWriter().startEvent(transcriptText, units);
            final Change place = ChangeFactory.addUnits(player, units);
            bridge.addChange(place);
          }
        }
      }
    }
  }

  /** Triggers all unit removal changes associated with {@code satisfiedTriggers}. */
  public static void triggerUnitRemoval(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, removeUnitsMatch(), fireTriggerParams);
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final GamePlayer player : t.getPlayers()) {
        for (final Territory ter : t.getRemoveUnits().keySet()) {
          for (int i = 0; i < eachMultiple; ++i) {
            removeUnits(t, ter, t.getRemoveUnits().get(ter), player, bridge);
          }
        }
      }
    }
  }

  /** Triggers all unit placement changes associated with {@code satisfiedTriggers}. */
  public static void triggerUnitPlacement(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, placeMatch(), fireTriggerParams);
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final GamePlayer player : t.getPlayers()) {
        for (final Territory ter : t.getPlacement().keySet()) {
          for (int i = 0; i < eachMultiple; ++i) {
            placeUnits(t, ter, t.getPlacement().get(ter), player, bridge);
          }
        }
      }
    }
  }

  /** Returns projected resource income for satisfied triggers. Should pass dummy bridge in. */
  public static IntegerMap<Resource> findResourceIncome(
      final Set<TriggerAttachment> satisfiedTriggers, final IDelegateBridge bridge) {
    return triggerResourceChange(
        satisfiedTriggers,
        bridge,
        new FireTriggerParams(null, null, true, true, true, true),
        new StringBuilder());
  }

  /**
   * Triggers all resource changes based on satisfied triggers and returns string summary of the
   * changes.
   */
  public static String triggerResourceChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {
    final StringBuilder endOfTurnReport = new StringBuilder();
    triggerResourceChange(satisfiedTriggers, bridge, fireTriggerParams, endOfTurnReport);
    return endOfTurnReport.toString();
  }

  private static IntegerMap<Resource> triggerResourceChange(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams,
      final StringBuilder endOfTurnReport) {
    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, resourceMatch(), fireTriggerParams);
    final IntegerMap<Resource> resources = new IntegerMap<>();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final GamePlayer player : t.getPlayers()) {
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
              ChangeFactory.changeResourcesChange(
                  player, data.getResourceList().getResource(t.getResource()), toAdd));
          final String puMessage =
              MyFormatter.attachmentNameToText(t.getName())
                  + ": "
                  + player.getName()
                  + " met a national objective for an additional "
                  + t.getResourceCount()
                  + " "
                  + t.getResource()
                  + "; end with "
                  + total
                  + " "
                  + t.getResource();
          bridge.getHistoryWriter().startEvent(puMessage);
          endOfTurnReport.append(puMessage).append(" <br />");
        }
      }
    }
    return resources;
  }

  /** Triggers all trigger activations associated with {@code satisfiedTriggers}. */
  public static void triggerActivateTriggerOther(
      final Map<ICondition, Boolean> testedConditionsSoFar,
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams activateFireTriggerParams) {
    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(
            satisfiedTriggers, activateTriggerMatch(), activateFireTriggerParams);
    for (final TriggerAttachment t : trigs) {
      if (activateFireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (activateFireTriggerParams.useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final Tuple<String, String> tuple : t.getActivateTrigger()) {
        // numberOfTimes:useUses:testUses:testConditions:testChance
        TriggerAttachment toFire = null;
        for (final GamePlayer player : data.getPlayerList().getPlayers()) {
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
        final Set<TriggerAttachment> toFireSet = new HashSet<>();
        toFireSet.add(toFire);
        final String[] options = splitOnColon(tuple.getSecond());
        final int numberOfTimesToFire = getInt(options[0]);
        final boolean useUsesToFire = getBool(options[1]);
        final boolean testUsesToFire = getBool(options[2]);
        final boolean testConditionsToFire = getBool(options[3]);
        final boolean testChanceToFire = getBool(options[4]);
        if (testConditionsToFire) {
          if (!testedConditionsSoFar.containsKey(toFire)) {
            // this should directly add the new tests to testConditionsToFire...
            collectTestsForAllTriggers(
                toFireSet,
                bridge,
                new HashSet<>(testedConditionsSoFar.keySet()),
                testedConditionsSoFar);
          }
          if (!isSatisfiedMatch(testedConditionsSoFar).test(toFire)) {
            continue;
          }
        }
        final FireTriggerParams toFireTriggerParams =
            new FireTriggerParams(
                activateFireTriggerParams.beforeOrAfter,
                activateFireTriggerParams.stepName,
                useUsesToFire,
                testUsesToFire,
                testChanceToFire,
                false);
        for (int i = 0; i < numberOfTimesToFire * eachMultiple; ++i) {
          bridge
              .getHistoryWriter()
              .startEvent(
                  MyFormatter.attachmentNameToText(t.getName())
                      + " activates a trigger called: "
                      + MyFormatter.attachmentNameToText(toFire.getName()));
          fireTriggers(toFireSet, testedConditionsSoFar, bridge, toFireTriggerParams);
        }
      }
    }
  }

  /** Triggers all victory notifications associated with {@code satisfiedTriggers}. */
  public static void triggerVictory(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams) {

    // NOTE: The check is needed to ensure that UI-related code (namely
    // 'NotificationMessages.getInstance()') is
    // not executed when unit testing non-UI related code such as 'MustFightBattleTest'.
    if (satisfiedTriggers.stream().anyMatch(victoryMatch())) {
      triggerVictory(
          satisfiedTriggers, bridge, fireTriggerParams, NotificationMessages.getInstance());
    }
  }

  @VisibleForTesting
  static void triggerVictory(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams,
      final NotificationMessages notificationMessages) {
    final GameData data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, victoryMatch(), fireTriggerParams);
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      if (t.getPlayers() == null) {
        continue;
      }
      final String victoryMessage = notificationMessages.getMessage(t.getVictory().trim());
      final String sounds = notificationMessages.getSoundsKey(t.getVictory().trim());
      if (victoryMessage != null) {
        if (sounds != null) { // only play the sound if we are also notifying everyone
          bridge
              .getSoundChannelBroadcaster()
              .playSoundToPlayers(
                  SoundPath.CLIP_TRIGGERED_VICTORY_SOUND + sounds.trim(),
                  t.getPlayers(),
                  null,
                  true);
          bridge
              .getSoundChannelBroadcaster()
              .playSoundToPlayers(
                  SoundPath.CLIP_TRIGGERED_DEFEAT_SOUND + sounds.trim(),
                  data.getPlayerList().getPlayers(),
                  t.getPlayers(),
                  false);
        }
        String messageForRecord = victoryMessage.trim();
        if (messageForRecord.length() > 150) {
          messageForRecord = messageForRecord.replaceAll("<br.*?>", " ");
          messageForRecord = messageForRecord.replaceAll("<.*?>", "");
          if (messageForRecord.length() > 155) {
            messageForRecord = messageForRecord.substring(0, 150) + "....";
          }
        }
        try {
          bridge
              .getHistoryWriter()
              .startEvent(
                  "Players: "
                      + MyFormatter.defaultNamedToTextList(t.getPlayers())
                      + " have just won the game, with this victory: "
                      + messageForRecord);
          final IDelegate delegateEndRound = data.getDelegate("endRound");
          ((EndRoundDelegate) delegateEndRound)
              .signalGameOver(victoryMessage.trim(), t.getPlayers(), bridge);
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
    return t -> t.getProductionRule() != null && !t.getProductionRule().isEmpty();
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
        .put(
            "frontier",
            MutableProperty.of(
                this::setFrontier, this::setFrontier, this::getFrontier, this::resetFrontier))
        .put(
            "productionRule",
            MutableProperty.of(
                this::setProductionRule,
                this::setProductionRule,
                this::getProductionRule,
                this::resetProductionRule))
        .put(
            "tech",
            MutableProperty.of(this::setTech, this::setTech, this::getTech, this::resetTech))
        .put(
            "availableTech",
            MutableProperty.of(
                this::setAvailableTech,
                this::setAvailableTech,
                this::getAvailableTech,
                this::resetAvailableTech))
        .put(
            "placement",
            MutableProperty.of(
                this::setPlacement, this::setPlacement, this::getPlacement, this::resetPlacement))
        .put(
            "removeUnits",
            MutableProperty.of(
                this::setRemoveUnits,
                this::setRemoveUnits,
                this::getRemoveUnits,
                this::resetRemoveUnits))
        .put(
            "purchase",
            MutableProperty.of(
                this::setPurchase, this::setPurchase, this::getPurchase, this::resetPurchase))
        .put(
            "resource",
            MutableProperty.ofString(this::setResource, this::getResource, this::resetResource))
        .put(
            "resourceCount",
            MutableProperty.of(
                this::setResourceCount,
                this::setResourceCount,
                this::getResourceCount,
                this::resetResourceCount))
        .put(
            "support",
            MutableProperty.of(
                this::setSupport, this::setSupport, this::getSupport, this::resetSupport))
        .put(
            "relationshipChange",
            MutableProperty.of(
                this::setRelationshipChange,
                this::setRelationshipChange,
                this::getRelationshipChange,
                this::resetRelationshipChange))
        .put(
            "victory",
            MutableProperty.ofString(this::setVictory, this::getVictory, this::resetVictory))
        .put(
            "activateTrigger",
            MutableProperty.of(
                this::setActivateTrigger,
                this::setActivateTrigger,
                this::getActivateTrigger,
                this::resetActivateTrigger))
        .put(
            "changeOwnership",
            MutableProperty.of(
                this::setChangeOwnership,
                this::setChangeOwnership,
                this::getChangeOwnership,
                this::resetChangeOwnership))
        .put(
            "unitType",
            MutableProperty.of(
                this::setUnitType, this::setUnitType, this::getUnitType, this::resetUnitType))
        .put(
            "unitAttachmentName",
            MutableProperty.of(
                this::setUnitAttachmentName,
                this::setUnitAttachmentName,
                this::getUnitAttachmentName,
                this::resetUnitAttachmentName))
        .put(
            "unitProperty",
            MutableProperty.of(
                this::setUnitProperty,
                this::setUnitProperty,
                this::getUnitProperty,
                this::resetUnitProperty))
        .put(
            "territories",
            MutableProperty.of(
                this::setTerritories,
                this::setTerritories,
                this::getTerritories,
                this::resetTerritories))
        .put(
            "territoryAttachmentName",
            MutableProperty.of(
                this::setTerritoryAttachmentName,
                this::setTerritoryAttachmentName,
                this::getTerritoryAttachmentName,
                this::resetTerritoryAttachmentName))
        .put(
            "territoryProperty",
            MutableProperty.of(
                this::setTerritoryProperty,
                this::setTerritoryProperty,
                this::getTerritoryProperty,
                this::resetTerritoryProperty))
        .put(
            "players",
            MutableProperty.of(
                this::setPlayers, this::setPlayers, this::getPlayers, this::resetPlayers))
        .put(
            "playerAttachmentName",
            MutableProperty.of(
                this::setPlayerAttachmentName,
                this::setPlayerAttachmentName,
                this::getPlayerAttachmentName,
                this::resetPlayerAttachmentName))
        .put(
            "playerProperty",
            MutableProperty.of(
                this::setPlayerProperty,
                this::setPlayerProperty,
                this::getPlayerProperty,
                this::resetPlayerProperty))
        .put(
            "relationshipTypes",
            MutableProperty.of(
                this::setRelationshipTypes,
                this::setRelationshipTypes,
                this::getRelationshipTypes,
                this::resetRelationshipTypes))
        .put(
            "relationshipTypeAttachmentName",
            MutableProperty.of(
                this::setRelationshipTypeAttachmentName,
                this::setRelationshipTypeAttachmentName,
                this::getRelationshipTypeAttachmentName,
                this::resetRelationshipTypeAttachmentName))
        .put(
            "relationshipTypeProperty",
            MutableProperty.of(
                this::setRelationshipTypeProperty,
                this::setRelationshipTypeProperty,
                this::getRelationshipTypeProperty,
                this::resetRelationshipTypeProperty))
        .put(
            "territoryEffects",
            MutableProperty.of(
                this::setTerritoryEffects,
                this::setTerritoryEffects,
                this::getTerritoryEffects,
                this::resetTerritoryEffects))
        .put(
            "territoryEffectAttachmentName",
            MutableProperty.of(
                this::setTerritoryEffectAttachmentName,
                this::setTerritoryEffectAttachmentName,
                this::getTerritoryEffectAttachmentName,
                this::resetTerritoryEffectAttachmentName))
        .put(
            "territoryEffectProperty",
            MutableProperty.of(
                this::setTerritoryEffectProperty,
                this::setTerritoryEffectProperty,
                this::getTerritoryEffectProperty,
                this::resetTerritoryEffectProperty))
        .build();
  }
}
