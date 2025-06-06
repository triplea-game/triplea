package games.strategy.triplea.attachments;

import static games.strategy.engine.data.TechnologyFrontierList.getTechnologyFrontierOrThrow;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
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
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.NotificationMessages;
import java.text.MessageFormat;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.ObjectUtils;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.ISound;
import org.triplea.sound.SoundPath;
import org.triplea.util.Tuple;

/**
 * An attachment for instances of {@link GamePlayer} that defines actions to be triggered upon
 * various events. Note: Empty collection fields default to null to minimize memory use and
 * serialization size.
 */
@Slf4j
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

  private @Nullable ProductionFrontier frontier = null;
  private @Nullable List<String> productionRule = null;
  private @Nullable List<TechAdvance> tech = null;
  private @Nullable Map<String, Map<TechAdvance, Boolean>> availableTech = null;
  private @Nullable Map<Territory, IntegerMap<UnitType>> placement = null;
  private @Nullable Map<Territory, IntegerMap<UnitType>> removeUnits = null;
  private @Nullable IntegerMap<UnitType> purchase = null;
  private @Nullable @NonNls String resource = null;
  private int resourceCount = 0;
  // never use a map of other attachments, inside of an attachment. java will not be able to
  // deserialize it.
  private @Nullable Map<String, Boolean> support = null;
  // List of relationshipChanges that should be executed when this trigger hits.
  private @Nullable List<String> relationshipChange = null;
  private @Nullable String victory = null;
  private @Nullable List<Tuple<String, String>> activateTrigger = null;
  private @Nullable List<String> changeOwnership = null;
  // raw property changes below:
  private @Nullable List<UnitType> unitTypes = null;
  // covers UnitAttachment, UnitSupportAttachment
  private @Nullable Tuple<String, String> unitAttachmentName = null;
  private @Nullable List<Tuple<String, String>> unitProperty = null;
  private @Nullable List<Territory> territories = null;
  // covers TerritoryAttachment, CanalAttachment
  private @Nullable Tuple<String, String> territoryAttachmentName = null;
  private @Nullable List<Tuple<String, String>> territoryProperty = null;
  private @Nullable List<GamePlayer> players = null;
  // covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment,
  // UserActionAttachment
  private @Nullable Tuple<String, String> playerAttachmentName = null;
  private @Nullable List<Tuple<String, String>> playerProperty = null;
  private @Nullable List<RelationshipType> relationshipTypes = null;
  // covers RelationshipTypeAttachment
  private @Nullable Tuple<String, String> relationshipTypeAttachmentName = null;
  private @Nullable List<Tuple<String, String>> relationshipTypeProperty = null;
  private @Nullable List<TerritoryEffect> territoryEffects = null;
  // covers TerritoryEffectAttachment
  private @Nullable Tuple<String, String> territoryEffectAttachmentName = null;
  private @Nullable List<Tuple<String, String>> territoryEffectProperty = null;

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
    Preconditions.checkNotNull(cond);
    final Set<TriggerAttachment> trigs = new HashSet<>();
    for (final IAttachment a : player.getAttachments().values()) {
      if (a instanceof TriggerAttachment && cond.test((TriggerAttachment) a)) {
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
    Preconditions.checkNotNull(triggerMatch);

    return players.stream()
        .map(player -> TriggerAttachment.getTriggers(player, triggerMatch))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
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
      setUsesForWhenTriggers(triggersToFire, bridge);
    }
  }

  protected static void setUsesForWhenTriggers(
      final Set<TriggerAttachment> triggersToBeFired, final IDelegateBridge bridge) {
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
      final TriggerAttachment triggerAttachment = (TriggerAttachment) player.getAttachment(s[0]);
      if (triggerAttachment != null) {
        trigger = triggerAttachment;
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
    if (activateTrigger == null) {
      activateTrigger = new ArrayList<>();
    }
    activateTrigger.add(Tuple.of(s[0].intern(), options.intern()));
  }

  private void setActivateTrigger(final List<Tuple<String, String>> value) {
    activateTrigger = value;
  }

  private List<Tuple<String, String>> getActivateTrigger() {
    return getListProperty(activateTrigger);
  }

  private void resetActivateTrigger() {
    activateTrigger = null;
  }

  private void setFrontier(final String s) throws GameParseException {
    final ProductionFrontier front = getData().getProductionFrontierList().getProductionFrontier(s);
    if (front == null) {
      throw new GameParseException("Could not find frontier. name: " + s + thisErrorMsg());
    }
    frontier = front;
  }

  private void setFrontier(final ProductionFrontier value) {
    frontier = value;
  }

  private Optional<ProductionFrontier> getFrontier() {
    return Optional.ofNullable(frontier);
  }

  private @Nullable ProductionFrontier getFrontierOrNull() {
    return frontier;
  }

  private void resetFrontier() {
    frontier = null;
  }

  private void setProductionRule(final String prop) throws GameParseException {
    final String[] s = splitOnColon(prop);
    if (s.length != 2) {
      throw new GameParseException("Invalid productionRule declaration: " + prop + thisErrorMsg());
    }
    if (getData().getProductionFrontierList().getProductionFrontier(s[0]) == null) {
      throw new GameParseException("Could not find frontier. name: " + s[0] + thisErrorMsg());
    }
    String rule = s[1];
    if (rule.startsWith("-")) {
      rule = rule.replaceFirst("-", "");
    }
    if (getData().getProductionRuleList().getProductionRule(rule) == null) {
      throw new GameParseException(
          "Could not find production rule. name: " + rule + thisErrorMsg());
    }
    if (productionRule == null) {
      productionRule = new ArrayList<>();
    }
    productionRule.add(prop.intern());
  }

  private void setProductionRule(final List<String> value) {
    productionRule = value;
  }

  List<String> getProductionRule() {
    return getListProperty(productionRule);
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
    victory = s.intern();
  }

  private Optional<String> getVictory() {
    return Optional.ofNullable(victory);
  }

  private String getVictoryOrThrow() {
    return getVictory()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    MessageFormat.format("No expected victory for TriggerAttachment {0}", this)));
  }

  private String getVictoryOrNull() {
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
        throw new GameParseException("Technology not found : " + subString + thisErrorMsg());
      }
      if (tech == null) {
        tech = new ArrayList<>();
      }
      tech.add(ta);
    }
  }

  private void setTech(final List<TechAdvance> value) {
    tech = value;
  }

  private List<TechAdvance> getTech() {
    return getListProperty(tech);
  }

  private void resetTech() {
    tech = null;
  }

  private void setAvailableTech(final String techs) throws GameParseException {
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
        throw new GameParseException("Technology not found : " + s[i] + thisErrorMsg());
      }
      tlist.put(ta, add);
    }
    if (availableTech == null) {
      availableTech = new HashMap<>();
    }
    if (availableTech.containsKey(cat)) {
      tlist.putAll(availableTech.get(cat));
    }
    availableTech.put(cat.intern(), tlist);
  }

  private void setAvailableTech(final Map<String, Map<TechAdvance, Boolean>> value) {
    availableTech = value;
  }

  private Map<String, Map<TechAdvance, Boolean>> getAvailableTech() {
    return getMapProperty(availableTech);
  }

  private void resetAvailableTech() {
    availableTech = null;
  }

  private void setSupport(final String value) throws GameParseException {
    for (final String entry : splitOnColon(value)) {
      final boolean remove = entry.startsWith("-");
      final String name = remove ? entry.substring(1) : entry;
      UnitSupportAttachment.get(getData().getUnitTypeList()).stream()
          .filter(support -> support.getName().equals(name))
          .findAny()
          .orElseThrow(
              () ->
                  new GameParseException(
                      "Could not find unitSupportAttachment. name: " + name + thisErrorMsg()));
      if (support == null) {
        support = new LinkedHashMap<>();
      }
      support.put(name.intern(), !remove);
    }
  }

  private void setSupport(final Map<String, Boolean> value) {
    support = value;
  }

  private Map<String, Boolean> getSupport() {
    return getMapProperty(support);
  }

  private void resetSupport() {
    support = null;
  }

  private void setResource(final String s) {
    getData().getResourceList().getResourceOrThrow(s);
    resource = s.intern();
  }

  private Optional<@NonNls String> getResource() {
    return Optional.ofNullable(resource);
  }

  private @Nullable @NonNls String getResourceOrNull() {
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
    for (int i = 0; i < 2; i++) {
      if (getData().getPlayerList().getPlayerId(s[i]) == null) {
        throw new GameParseException(
            "Invalid relationshipChange declaration: "
                + relChange
                + " \n player: "
                + s[i]
                + " unknown "
                + thisErrorMsg());
      }
    }
    if (!(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED)
        || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR)
        || Matches.isValidRelationshipName(getData().getRelationshipTypeList()).test(s[2]))) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n relationshipType: "
              + s[2]
              + " unknown "
              + thisErrorMsg());
    }
    if (Matches.isValidRelationshipName(getData().getRelationshipTypeList()).negate().test(s[3])) {
      throw new GameParseException(
          "Invalid relationshipChange declaration: "
              + relChange
              + " \n relationshipType: "
              + s[3]
              + " unknown "
              + thisErrorMsg());
    }
    if (relationshipChange == null) {
      relationshipChange = new ArrayList<>();
    }
    relationshipChange.add(relChange.intern());
  }

  private void setRelationshipChange(final List<String> value) {
    relationshipChange = value;
  }

  private List<String> getRelationshipChange() {
    return getListProperty(relationshipChange);
  }

  private void resetRelationshipChange() {
    relationshipChange = null;
  }

  private void setUnitType(final String names) throws GameParseException {
    for (final String element : splitOnColon(names)) {
      if (unitTypes == null) {
        unitTypes = new ArrayList<>();
      }
      unitTypes.add(getUnitTypeOrThrow(element));
    }
  }

  private void setUnitType(final List<UnitType> value) {
    unitTypes = value;
  }

  private List<UnitType> getUnitType() {
    return getListProperty(unitTypes);
  }

  private void resetUnitType() {
    unitTypes = null;
  }

  private void setUnitAttachmentName(final String name) throws GameParseException {
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
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    if (s[1].equals("UnitSupportAttachment")
        && !s[0].startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    unitAttachmentName = Tuple.of(s[1].intern(), s[0].intern());
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
    final String[] s = splitOnColon(prop);
    if (unitProperty == null) {
      unitProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1].intern();
    unitProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setUnitProperty(final List<Tuple<String, String>> value) {
    unitProperty = value;
  }

  private List<Tuple<String, String>> getUnitProperty() {
    return getListProperty(unitProperty);
  }

  private void resetUnitProperty() {
    unitProperty = null;
  }

  private void setTerritories(final String names) throws GameParseException {
    final String[] s = splitOnColon(names);
    for (final String element : s) {
      if (territories == null) {
        territories = new ArrayList<>();
      }
      territories.add(getTerritoryOrThrow(element));
    }
  }

  private void setTerritories(final List<Territory> value) {
    territories = value;
  }

  private List<Territory> getTerritories() {
    return getListProperty(territories);
  }

  private void resetTerritories() {
    territories = null;
  }

  private void setTerritoryAttachmentName(final String name) throws GameParseException {
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
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    if (s[1].equals("CanalAttachment") && !s[0].startsWith(Constants.CANAL_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    territoryAttachmentName = Tuple.of(s[1].intern(), s[0].intern());
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
    final String[] s = splitOnColon(prop);
    if (territoryProperty == null) {
      territoryProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1].intern();
    territoryProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setTerritoryProperty(final List<Tuple<String, String>> value) {
    territoryProperty = value;
  }

  private List<Tuple<String, String>> getTerritoryProperty() {
    return getListProperty(territoryProperty);
  }

  private void resetTerritoryProperty() {
    territoryProperty = null;
  }

  private void setPlayers(final String names) throws GameParseException {
    players = parsePlayerList(names, players);
  }

  private void setPlayers(final List<GamePlayer> value) {
    players = value;
  }

  private List<GamePlayer> getPlayers() {
    List<GamePlayer> result = getListProperty(players);
    return result.isEmpty() ? List.of((GamePlayer) getAttachedTo()) : result;
  }

  private void resetPlayers() {
    players = null;
  }

  private void setPlayerAttachmentName(final String playerAttachmentName)
      throws GameParseException {
    // replace-all to automatically correct legacy (1.8) attachment spelling
    final String name = playerAttachmentName.replaceAll("ttatch", "ttach");
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
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    if (s[1].equals("RulesAttachment")
        && !(s[0].startsWith(Constants.RULES_ATTACHMENT_NAME)
            || s[0].startsWith(Constants.RULES_OBJECTIVE_PREFIX)
            || s[0].startsWith(Constants.RULES_CONDITION_PREFIX))) {
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    if (s[1].equals("TriggerAttachment") && !s[0].startsWith(Constants.TRIGGER_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    if (s[1].equals("TechAttachment") && !s[0].startsWith(Constants.TECH_ATTACHMENT_NAME)) {
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    if (s[1].equals("PoliticalActionAttachment")
        && !s[0].startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    if (s[1].equals("UserActionAttachment")
        && !s[0].startsWith(Constants.USERACTION_ATTACHMENT_PREFIX)) {
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    this.playerAttachmentName = Tuple.of(s[1].intern(), s[0].intern());
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
    final String[] s = splitOnColon(prop);
    if (playerProperty == null) {
      playerProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1].intern();
    playerProperty.add(Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setPlayerProperty(final List<Tuple<String, String>> value) {
    playerProperty = value;
  }

  private List<Tuple<String, String>> getPlayerProperty() {
    return getListProperty(playerProperty);
  }

  private void resetPlayerProperty() {
    playerProperty = null;
  }

  private void setRelationshipTypes(final String names) throws GameParseException {
    for (final String element : splitOnColon(names)) {
      final RelationshipType relation =
          getData().getRelationshipTypeList().getRelationshipType(element);
      if (relation == null) {
        throw new GameParseException(
            "Could not find relationshipType. name: " + element + thisErrorMsg());
      }
      if (relationshipTypes == null) {
        relationshipTypes = new ArrayList<>();
      }
      relationshipTypes.add(relation);
    }
  }

  private void setRelationshipTypes(final List<RelationshipType> value) {
    relationshipTypes = value;
  }

  private List<RelationshipType> getRelationshipTypes() {
    return getListProperty(relationshipTypes);
  }

  private void resetRelationshipTypes() {
    relationshipTypes = null;
  }

  private void setRelationshipTypeAttachmentName(final String name) throws GameParseException {
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
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    relationshipTypeAttachmentName = Tuple.of(s[1].intern(), s[0].intern());
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
    final String[] s = splitOnColon(prop);
    if (relationshipTypeProperty == null) {
      relationshipTypeProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1].intern();
    relationshipTypeProperty.add(
        Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setRelationshipTypeProperty(final List<Tuple<String, String>> value) {
    relationshipTypeProperty = value;
  }

  private List<Tuple<String, String>> getRelationshipTypeProperty() {
    return getListProperty(relationshipTypeProperty);
  }

  private void resetRelationshipTypeProperty() {
    relationshipTypeProperty = null;
  }

  private void setTerritoryEffects(final String effectNames) throws GameParseException {
    for (final String name : splitOnColon(effectNames)) {
      final TerritoryEffect effect = getData().getTerritoryEffectList().get(name);
      if (effect == null) {
        throw new GameParseException(
            "Could not find territoryEffect. name: " + name + thisErrorMsg());
      }
      if (territoryEffects == null) {
        territoryEffects = new ArrayList<>();
      }
      territoryEffects.add(effect);
    }
  }

  private void setTerritoryEffects(final List<TerritoryEffect> value) {
    territoryEffects = value;
  }

  private List<TerritoryEffect> getTerritoryEffects() {
    return getListProperty(territoryEffects);
  }

  private void resetTerritoryEffects() {
    territoryEffects = null;
  }

  private void setTerritoryEffectAttachmentName(final String name) throws GameParseException {
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
      throw new GameParseException("attachment incorrectly named: " + s[0] + thisErrorMsg());
    }
    territoryEffectAttachmentName = Tuple.of(s[1].intern(), s[0].intern());
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
    final String[] s = splitOnColon(prop);
    if (territoryEffectProperty == null) {
      territoryEffectProperty = new ArrayList<>();
    }
    // the last one is the property we are changing, while the rest is the string we are changing it
    // to
    final String property = s[s.length - 1].intern();
    territoryEffectProperty.add(
        Tuple.of(property, getValueFromStringArrayForAllExceptLastSubstring(s)));
  }

  private void setTerritoryEffectProperty(final List<Tuple<String, String>> value) {
    territoryEffectProperty = value;
  }

  private List<Tuple<String, String>> getTerritoryEffectProperty() {
    return getListProperty(territoryEffectProperty);
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
    final Territory territory = getTerritoryOrThrow(s[i]);

    i++;
    final IntegerMap<UnitType> map = new IntegerMap<>();
    for (; i < s.length; i++) {
      map.add(getUnitTypeOrThrow(s[i]), count);
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
    return getMapProperty(placement);
  }

  private void resetPlacement() {
    placement = null;
  }

  private void setRemoveUnits(final String value) throws GameParseException {
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
      final Optional<UnitType> optionalUnitType =
          getDataOrThrow().getUnitTypeList().getUnitType(s[i]);
      if (optionalUnitType.isEmpty()) {
        if (s[i].equalsIgnoreCase("all")) {
          types.addAll(getData().getUnitTypeList().getAllUnitTypes());
        } else {
          throw new GameParseException("UnitType does not exist " + s[i] + thisErrorMsg());
        }
      } else {
        types.add(optionalUnitType.get());
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
    return getMapProperty(removeUnits);
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
      purchase.add(getUnitTypeOrThrow(s[i]), count);
    }
  }

  private void setPurchase(final IntegerMap<UnitType> value) {
    purchase = value;
  }

  private IntegerMap<UnitType> getPurchase() {
    return getIntegerMapProperty(purchase);
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
      getTerritoryOrThrow(s[0]);
    }
    if (!s[1].equalsIgnoreCase("any")) {
      getPlayerOrThrow(s[1]);
    }
    getPlayerOrThrow(s[2]);
    getBool(s[3]);
    if (changeOwnership == null) {
      changeOwnership = new ArrayList<>();
    }
    changeOwnership.add(value.intern());
  }

  private void setChangeOwnership(final List<String> value) {
    changeOwnership = value;
  }

  private List<String> getChangeOwnership() {
    return getListProperty(changeOwnership);
  }

  private void resetChangeOwnership() {
    changeOwnership = null;
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
    // create the units
    final List<Unit> units = new ArrayList<>();
    for (final Map.Entry<UnitType, Integer> entry : utMap.entrySet()) {
      units.addAll(entry.getKey().create(entry.getValue(), player));
    }
    final CompositeChange change = new CompositeChange();
    // mark no movement and original owner
    final Predicate<Unit> unitIsInfra = Matches.unitIsInfrastructure();
    for (final Unit unit : units) {
      change.add(ChangeFactory.markNoMovementChange(unit));
      if (unitIsInfra.test(unit)) {
        change.add(OriginalOwnerTracker.addOriginalOwnerChange(unit, player));
      }
    }
    // place units
    change.add(ChangeFactory.addUnits(terr, units));
    bridge.addChange(change);
    final String transcriptText =
        MyFormatter.attachmentNameToText(t.getName())
            + ": "
            + player.getName()
            + " has "
            + MyFormatter.unitsToTextNoOwner(units)
            + " placed in "
            + terr.getName();
    bridge.getHistoryWriter().startEvent(transcriptText, units);
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
              + propertyAttachmentName,
          e);
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
    // 'new NotificationMessages(..)') is
    // not executed when unit testing non-UI related code such as 'MustFightBattleTest'.
    if (satisfiedTriggers.stream().anyMatch(notificationMatch())) {
      bridge
          .getResourceLoader()
          .ifPresent(
              resourceLoader ->
                  triggerNotifications(
                      satisfiedTriggers,
                      bridge,
                      fireTriggerParams,
                      new NotificationMessages(resourceLoader)));
    }
  }

  @VisibleForTesting
  static void triggerNotifications(
      final Set<TriggerAttachment> satisfiedTriggers,
      final IDelegateBridge bridge,
      final FireTriggerParams fireTriggerParams,
      final NotificationMessages notificationMessages) {

    final GameState data = bridge.getData();
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
                TerritoryAttachment.get(territory, t.getTerritoryAttachmentName().getSecond())
                    .orElseThrow(
                        () ->
                            // water territories may not have an attachment, so this could be null
                            new IllegalStateException(
                                "Triggers: No territory attachment for: " + territory.getName()));

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
    IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final GamePlayer player : t.getPlayers()) {
        for (final String cat : t.getAvailableTech().keySet()) {
          final TechnologyFrontier tf = getTechnologyFrontierOrThrow(player, cat);
          for (final TechAdvance ta : t.getAvailableTech().get(cat).keySet()) {
            if (t.getAvailableTech().get(cat).get(ta)) {
              historyWriter.startEvent(
                  MyFormatter.attachmentNameToText(t.getName())
                      + ": "
                      + player.getName()
                      + " gains access to "
                      + ta);
              final Change change = ChangeFactory.addAvailableTech(tf, ta, player);
              bridge.addChange(change);
            } else {
              historyWriter.startEvent(
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
    IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      for (final GamePlayer player : t.getPlayers()) {
        for (final TechAdvance ta : t.getTech()) {
          if (ta.hasTech(player.getTechAttachment())) {
            continue;
          }
          historyWriter.startEvent(
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
      IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
      t.getFrontier()
          .ifPresent(
              productionFrontier -> {
                for (final GamePlayer player : t.getPlayers()) {
                  change.add(ChangeFactory.changeProductionFrontier(player, productionFrontier));
                  historyWriter.startEvent(
                      MyFormatter.attachmentNameToText(t.getName())
                          + ": "
                          + player.getName()
                          + " has their production frontier changed to: "
                          + productionFrontier.getName());
                }
              });
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
    final GameState data = bridge.getData();
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
                final IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
                if (ruleAdded) {
                  if (!front.getRules().contains(productionRule)) {
                    change.add(ChangeFactory.addProductionRule(productionRule, front));
                    historyWriter.startEvent(
                        MyFormatter.attachmentNameToText(triggerAttachment.getName())
                            + ": "
                            + productionRule.getName()
                            + " added to "
                            + front.getName());
                  }
                } else {
                  if (front.getRules().contains(productionRule)) {
                    change.add(ChangeFactory.removeProductionRule(productionRule, front));
                    historyWriter.startEvent(
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
        for (final Map.Entry<String, Boolean> entry : t.getSupport().entrySet()) {
          final UnitSupportAttachment usa =
              UnitSupportAttachment.get(data.getUnitTypeList()).stream()
                  .filter(s -> s.getName().equals(entry.getKey()))
                  .findAny()
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Could not find unitSupportAttachment. name: " + entry.getKey()));
          final List<GamePlayer> p = new ArrayList<>(usa.getPlayers());
          final boolean addPlayer = entry.getValue();
          if (p.contains(player) == addPlayer) {
            continue;
          }
          if (addPlayer) {
            p.add(player);
          } else {
            p.remove(player);
          }
          change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
          final String historyText =
              MyFormatter.attachmentNameToText(t.getName())
                  + ": "
                  + player.getName()
                  + (addPlayer ? " is added to " : " is removed from ")
                  + usa;
          bridge.getHistoryWriter().startEvent(historyText);
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
    final BattleTracker bt = data.getBattleDelegate().getBattleTracker();
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
        final IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
        for (final Territory terr : territories) {
          final GamePlayer currentOwner = terr.getOwner();
          if (TerritoryAttachment.get(terr) == null) {
            continue; // any territory that has no territory attachment should definitely not be
            // changed
          }
          if (oldOwner != null && !oldOwner.equals(currentOwner)) {
            continue;
          }
          historyWriter.startEvent(
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
    final GameState data = bridge.getData();
    final Collection<TriggerAttachment> trigs =
        filterSatisfiedTriggers(satisfiedTriggers, resourceMatch(), fireTriggerParams);
    final IntegerMap<Resource> resources = new IntegerMap<>();
    for (final TriggerAttachment t : trigs) {
      if (fireTriggerParams.testChance && !t.testChance(bridge)) {
        continue;
      }
      final Optional<String> optionalResource = t.getResource();
      if (optionalResource.isEmpty()) {
        continue;
      }
      final String resource = optionalResource.get();
      if (fireTriggerParams.useUses) {
        t.use(bridge);
      }
      final int eachMultiple = getEachMultiple(t);
      for (final GamePlayer player : t.getPlayers()) {
        for (int i = 0; i < eachMultiple; ++i) {
          int toAdd = t.getResourceCount();
          if (resource.equals(Constants.PUS)) {
            toAdd *= Properties.getPuMultiplier(data.getProperties());
          }
          resources.add(data.getResourceList().getResourceOrThrow(t.getResource()), toAdd);
          int total = player.getResources().getQuantity(t.getResource()) + toAdd;
          if (total < 0) {
            toAdd -= total;
            total = 0;
          }
          bridge.addChange(
              ChangeFactory.changeResourcesChange(
                  player, data.getResourceList().getResourceOrThrow(t.getResource()), toAdd));
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
    final GameState data = bridge.getData();
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
          final TriggerAttachment ta = (TriggerAttachment) player.getAttachment(tuple.getFirst());
          if (ta != null) {
            toFire = ta;
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
        IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
        for (int i = 0; i < numberOfTimesToFire * eachMultiple; ++i) {
          historyWriter.startEvent(
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
    // 'new NotificationMessages(...)') is
    // not executed when unit testing non-UI related code such as 'MustFightBattleTest'.
    if (satisfiedTriggers.stream().anyMatch(victoryMatch())) {
      bridge
          .getResourceLoader()
          .ifPresent(
              resourceLoader ->
                  triggerVictory(
                      satisfiedTriggers,
                      bridge,
                      fireTriggerParams,
                      new NotificationMessages(resourceLoader)));
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
      final String victoryMessage = notificationMessages.getMessage(t.getVictoryOrThrow().trim());
      final String sounds = notificationMessages.getSoundsKey(t.getVictoryOrThrow().trim());
      if (victoryMessage != null) {
        if (sounds != null) { // only play the sound if we are also notifying everyone
          ISound sound = bridge.getSoundChannelBroadcaster();
          sound.playSoundToPlayers(
              SoundPath.CLIP_TRIGGERED_VICTORY_SOUND + sounds.trim(), t.getPlayers(), null, true);
          sound.playSoundToPlayers(
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
          IDelegateHistoryWriter historyWriter = bridge.getHistoryWriter();
          historyWriter.startEvent(
              "Players: "
                  + MyFormatter.defaultNamedToTextList(t.getPlayers())
                  + " have just won the game, with this victory: "
                  + messageForRecord);
          final EndRoundDelegate delegateEndRound = (EndRoundDelegate) data.getDelegate("endRound");
          delegateEndRound.signalGameOver(victoryMessage.trim(), t.getPlayers(), bridge);
        } catch (final Exception e) {
          log.error("Failed to signal game over", e);
        }
      }
    }
  }

  public static Predicate<TriggerAttachment> prodMatch() {
    return t -> t.getFrontier().isPresent();
  }

  public static Predicate<TriggerAttachment> prodFrontierEditMatch() {
    return t -> !t.getProductionRule().isEmpty();
  }

  public static Predicate<TriggerAttachment> techMatch() {
    return t -> !t.getTech().isEmpty();
  }

  public static Predicate<TriggerAttachment> techAvailableMatch() {
    return t -> !t.getAvailableTech().isEmpty();
  }

  public static Predicate<TriggerAttachment> removeUnitsMatch() {
    return t -> !t.getRemoveUnits().isEmpty();
  }

  public static Predicate<TriggerAttachment> placeMatch() {
    return t -> !t.getPlacement().isEmpty();
  }

  public static Predicate<TriggerAttachment> purchaseMatch() {
    return t -> !t.getPurchase().isEmpty();
  }

  public static Predicate<TriggerAttachment> resourceMatch() {
    return t -> t.getResource() != null && t.getResourceCount() != 0;
  }

  public static Predicate<TriggerAttachment> supportMatch() {
    return t -> !t.getSupport().isEmpty();
  }

  public static Predicate<TriggerAttachment> changeOwnershipMatch() {
    return t -> !t.getChangeOwnership().isEmpty();
  }

  public static Predicate<TriggerAttachment> unitPropertyMatch() {
    return t -> !t.getUnitType().isEmpty() && !t.getUnitProperty().isEmpty();
  }

  public static Predicate<TriggerAttachment> territoryPropertyMatch() {
    return t -> !t.getTerritories().isEmpty() && !t.getTerritoryProperty().isEmpty();
  }

  public static Predicate<TriggerAttachment> playerPropertyMatch() {
    return t -> !t.getPlayerProperty().isEmpty();
  }

  public static Predicate<TriggerAttachment> relationshipTypePropertyMatch() {
    return t -> !t.getRelationshipTypes().isEmpty() && !t.getRelationshipTypeProperty().isEmpty();
  }

  public static Predicate<TriggerAttachment> territoryEffectPropertyMatch() {
    return t -> !t.getTerritoryEffects().isEmpty() && !t.getTerritoryEffectProperty().isEmpty();
  }

  public static Predicate<TriggerAttachment> relationshipChangeMatch() {
    return t -> !t.getRelationshipChange().isEmpty();
  }

  public static Predicate<TriggerAttachment> victoryMatch() {
    return t -> !t.getVictory().orElse("").isEmpty();
  }

  public static Predicate<TriggerAttachment> activateTriggerMatch() {
    return t -> !t.getActivateTrigger().isEmpty();
  }

  @Override
  public Optional<MutableProperty<?>> getPropertyOrEmpty(final @NonNls String propertyName) {
    return switch (propertyName) {
      case "frontier" ->
          Optional.of(
              MutableProperty.of(
                  this::setFrontier,
                  this::setFrontier,
                  this::getFrontierOrNull,
                  this::resetFrontier));
      case "productionRule" ->
          Optional.of(
              MutableProperty.of(
                  this::setProductionRule,
                  this::setProductionRule,
                  this::getProductionRule,
                  this::resetProductionRule));
      case "tech" ->
          Optional.of(
              MutableProperty.of(this::setTech, this::setTech, this::getTech, this::resetTech));
      case "availableTech" ->
          Optional.of(
              MutableProperty.of(
                  this::setAvailableTech,
                  this::setAvailableTech,
                  this::getAvailableTech,
                  this::resetAvailableTech));
      case "placement" ->
          Optional.of(
              MutableProperty.of(
                  this::setPlacement,
                  this::setPlacement,
                  this::getPlacement,
                  this::resetPlacement));
      case "removeUnits" ->
          Optional.of(
              MutableProperty.of(
                  this::setRemoveUnits,
                  this::setRemoveUnits,
                  this::getRemoveUnits,
                  this::resetRemoveUnits));
      case "purchase" ->
          Optional.of(
              MutableProperty.of(
                  this::setPurchase, this::setPurchase, this::getPurchase, this::resetPurchase));
      case "resource" ->
          Optional.of(
              MutableProperty.ofString(
                  this::setResource, this::getResourceOrNull, this::resetResource));
      case "resourceCount" ->
          Optional.of(
              MutableProperty.of(
                  this::setResourceCount,
                  this::setResourceCount,
                  this::getResourceCount,
                  this::resetResourceCount));
      case "support" ->
          Optional.of(
              MutableProperty.of(
                  this::setSupport, this::setSupport, this::getSupport, this::resetSupport));
      case "relationshipChange" ->
          Optional.of(
              MutableProperty.of(
                  this::setRelationshipChange,
                  this::setRelationshipChange,
                  this::getRelationshipChange,
                  this::resetRelationshipChange));
      case "victory" ->
          Optional.of(
              MutableProperty.ofString(
                  this::setVictory, this::getVictoryOrNull, this::resetVictory));
      case "activateTrigger" ->
          Optional.of(
              MutableProperty.of(
                  this::setActivateTrigger,
                  this::setActivateTrigger,
                  this::getActivateTrigger,
                  this::resetActivateTrigger));
      case "changeOwnership" ->
          Optional.of(
              MutableProperty.of(
                  this::setChangeOwnership,
                  this::setChangeOwnership,
                  this::getChangeOwnership,
                  this::resetChangeOwnership));
      case "unitType" ->
          Optional.of(
              MutableProperty.of(
                  this::setUnitType, this::setUnitType, this::getUnitType, this::resetUnitType));
      case "unitAttachmentName" ->
          Optional.of(
              MutableProperty.of(
                  this::setUnitAttachmentName,
                  this::setUnitAttachmentName,
                  this::getUnitAttachmentName,
                  this::resetUnitAttachmentName));
      case "unitProperty" ->
          Optional.of(
              MutableProperty.of(
                  this::setUnitProperty,
                  this::setUnitProperty,
                  this::getUnitProperty,
                  this::resetUnitProperty));
      case "territories" ->
          Optional.of(
              MutableProperty.of(
                  this::setTerritories,
                  this::setTerritories,
                  this::getTerritories,
                  this::resetTerritories));
      case "territoryAttachmentName" ->
          Optional.of(
              MutableProperty.of(
                  this::setTerritoryAttachmentName,
                  this::setTerritoryAttachmentName,
                  this::getTerritoryAttachmentName,
                  this::resetTerritoryAttachmentName));
      case "territoryProperty" ->
          Optional.of(
              MutableProperty.of(
                  this::setTerritoryProperty,
                  this::setTerritoryProperty,
                  this::getTerritoryProperty,
                  this::resetTerritoryProperty));
      case "players" ->
          Optional.of(
              MutableProperty.of(
                  this::setPlayers, this::setPlayers, this::getPlayers, this::resetPlayers));
      case "playerAttachmentName" ->
          Optional.of(
              MutableProperty.of(
                  this::setPlayerAttachmentName,
                  this::setPlayerAttachmentName,
                  this::getPlayerAttachmentName,
                  this::resetPlayerAttachmentName));
      case "playerProperty" ->
          Optional.of(
              MutableProperty.of(
                  this::setPlayerProperty,
                  this::setPlayerProperty,
                  this::getPlayerProperty,
                  this::resetPlayerProperty));
      case "relationshipTypes" ->
          Optional.of(
              MutableProperty.of(
                  this::setRelationshipTypes,
                  this::setRelationshipTypes,
                  this::getRelationshipTypes,
                  this::resetRelationshipTypes));
      case "relationshipTypeAttachmentName" ->
          Optional.of(
              MutableProperty.of(
                  this::setRelationshipTypeAttachmentName,
                  this::setRelationshipTypeAttachmentName,
                  this::getRelationshipTypeAttachmentName,
                  this::resetRelationshipTypeAttachmentName));
      case "relationshipTypeProperty" ->
          Optional.of(
              MutableProperty.of(
                  this::setRelationshipTypeProperty,
                  this::setRelationshipTypeProperty,
                  this::getRelationshipTypeProperty,
                  this::resetRelationshipTypeProperty));
      case "territoryEffects" ->
          Optional.of(
              MutableProperty.of(
                  this::setTerritoryEffects,
                  this::setTerritoryEffects,
                  this::getTerritoryEffects,
                  this::resetTerritoryEffects));
      case "territoryEffectAttachmentName" ->
          Optional.of(
              MutableProperty.of(
                  this::setTerritoryEffectAttachmentName,
                  this::setTerritoryEffectAttachmentName,
                  this::getTerritoryEffectAttachmentName,
                  this::resetTerritoryEffectAttachmentName));
      case "territoryEffectProperty" ->
          Optional.of(
              MutableProperty.of(
                  this::setTerritoryEffectProperty,
                  this::setTerritoryEffectProperty,
                  this::getTerritoryEffectProperty,
                  this::resetTerritoryEffectProperty));
      default -> super.getPropertyOrEmpty(propertyName);
    };
  }
}
