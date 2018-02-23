package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Interruptibles;
import games.strategy.util.PredicateBuilder;
import games.strategy.util.Tuple;

/**
 * Handles logic for battles in which fighting actually occurs.
 */
public class MustFightBattle extends DependentBattle implements BattleStepStrings {

  /**
   * Determines whether casualties can return fire for various battle phases.
   */
  public enum ReturnFire {
    ALL, SUBS, NONE
  }

  public enum RetreatType {
    DEFAULT, SUBS, PLANES, PARTIAL_AMPHIB
  }

  // this class exists for testing
  public abstract static class AttackSubs implements IExecutable {
    private static final long serialVersionUID = 4872551667582174716L;
  }

  // this class exists for testing
  public abstract static class DefendSubs implements IExecutable {
    private static final long serialVersionUID = 3768066729336520095L;
  }

  private static final long serialVersionUID = 5879502298361231540L;
  // maps Territory-> units (stores a collection of who is attacking from where, needed for undoing moves)
  private Map<Territory, Collection<Unit>> m_attackingFromMap = new HashMap<>();
  private final Collection<Unit> m_attackingWaitingToDie = new ArrayList<>();
  private Set<Territory> m_attackingFrom = new HashSet<>();
  private Collection<Territory> m_amphibiousAttackFrom = new ArrayList<>();
  private final Collection<Unit> m_defendingWaitingToDie = new ArrayList<>();
  // keep track of all the units that die in the battle to show in the history window
  private final Collection<Unit> m_killed = new ArrayList<>();

  // Our current execution state, we keep a stack of executables, this allows us to save our state and resume while in
  // the middle of a battle.
  private final ExecutionStack m_stack = new ExecutionStack();
  private List<String> m_stepStrings;
  protected List<Unit> m_defendingAA;
  protected List<Unit> m_offensiveAA;
  protected List<String> m_defendingAAtypes;
  protected List<String> m_offensiveAAtypes;
  private final List<Unit> m_attackingUnitsRetreated = new ArrayList<>();
  private final List<Unit> m_defendingUnitsRetreated = new ArrayList<>();
  // -1 would mean forever until one side is eliminated (the default is infinite)
  private final int m_maxRounds;

  public MustFightBattle(final Territory battleSite, final PlayerID attacker, final GameData data,
      final BattleTracker battleTracker) {
    super(battleSite, attacker, battleTracker, data);
    m_defendingUnits.addAll(m_battleSite.getUnits().getMatches(Matches.enemyUnit(attacker, data)));
    if (battleSite.isWater()) {
      m_maxRounds = Properties.getSeaBattleRounds(data);
    } else {
      m_maxRounds = Properties.getLandBattleRounds(data);
    }
    m_attackingFromMap = new HashMap<>();
    m_attackingFrom = new HashSet<>();
    m_amphibiousAttackFrom = new ArrayList<>();
  }

  public void resetDefendingUnits(final PlayerID attacker, final GameData data) {
    m_defendingUnits.clear();
    m_defendingUnits.addAll(m_battleSite.getUnits().getMatches(Matches.enemyUnit(attacker, data)));
  }

  /**
   * Used for head-less battles.
   */
  public void setUnits(final Collection<Unit> defending, final Collection<Unit> attacking,
      final Collection<Unit> bombarding, final Collection<Unit> amphibious, final PlayerID defender,
      final Collection<TerritoryEffect> territoryEffects) {
    m_defendingUnits = new ArrayList<>(defending);
    m_attackingUnits = new ArrayList<>(attacking);
    m_bombardingUnits = new ArrayList<>(bombarding);
    m_amphibiousLandAttackers = new ArrayList<>(amphibious);
    m_isAmphibious = m_amphibiousLandAttackers.size() > 0;
    m_defender = defender;
    m_territoryEffects = territoryEffects;
  }

  public boolean shouldEndBattleDueToMaxRounds() {
    return (m_maxRounds > 0) && (m_maxRounds <= m_round);
  }

  private boolean canSubsSubmerge() {
    return Properties.getSubmersibleSubs(m_data);
  }

  @Override
  public void removeAttack(final Route route, final Collection<Unit> units) {
    m_attackingUnits.removeAll(units);
    // the route could be null, in the case of a unit in a territory where a sub is submerged.
    if (route == null) {
      return;
    }
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    Collection<Unit> attackingFromMapUnits = m_attackingFromMap.get(attackingFrom);
    // handle possible null pointer
    if (attackingFromMapUnits == null) {
      attackingFromMapUnits = new ArrayList<>();
    }
    attackingFromMapUnits.removeAll(units);
    if (attackingFromMapUnits.isEmpty()) {
      m_attackingFrom.remove(attackingFrom);
    }
    // deal with amphibious assaults
    if (attackingFrom.isWater()) {
      if ((route.getEnd() != null) && !route.getEnd().isWater() && units.stream().anyMatch(Matches.unitIsLand())) {
        m_amphibiousLandAttackers.removeAll(CollectionUtils.getMatches(units, Matches.unitIsLand()));
      }
      // if none of the units is a land unit, the attack from
      // that territory is no longer an amphibious assault
      if (attackingFromMapUnits.stream().noneMatch(Matches.unitIsLand())) {
        getAmphibiousAttackTerritories().remove(attackingFrom);
        // do we have any amphibious attacks left?
        m_isAmphibious = !getAmphibiousAttackTerritories().isEmpty();
      }
    }
    for (final Collection<Unit> dependents : m_dependentUnits.values()) {
      dependents.removeAll(units);
    }
  }

  @Override
  public boolean isEmpty() {
    return m_attackingUnits.isEmpty() && m_attackingWaitingToDie.isEmpty();
  }

  @Override
  public Change addAttackChange(final Route route, final Collection<Unit> units,
      final HashMap<Unit, HashSet<Unit>> targets) {
    final CompositeChange change = new CompositeChange();
    // Filter out allied units if WW2V2
    final Predicate<Unit> ownedBy = Matches.unitIsOwnedBy(m_attacker);
    final Collection<Unit> attackingUnits = isWW2V2() ? CollectionUtils.getMatches(units, ownedBy) : units;
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    m_attackingFrom.add(attackingFrom);
    m_attackingUnits.addAll(attackingUnits);
    if (m_attackingFromMap.get(attackingFrom) == null) {
      m_attackingFromMap.put(attackingFrom, new ArrayList<>());
    }
    final Collection<Unit> attackingFromMapUnits = m_attackingFromMap.get(attackingFrom);
    attackingFromMapUnits.addAll(attackingUnits);
    // are we amphibious
    if (route.getStart().isWater() && (route.getEnd() != null) && !route.getEnd().isWater()
        && attackingUnits.stream().anyMatch(Matches.unitIsLand())) {
      getAmphibiousAttackTerritories().add(route.getTerritoryBeforeEnd());
      m_amphibiousLandAttackers.addAll(CollectionUtils.getMatches(attackingUnits, Matches.unitIsLand()));
      m_isAmphibious = true;
    }
    final Map<Unit, Collection<Unit>> dependencies = TransportTracker.transporting(units);
    if (!isAlliedAirIndependent()) {
      dependencies.putAll(MoveValidator.carrierMustMoveWith(units, units, m_data, m_attacker));
      for (final Unit carrier : dependencies.keySet()) {
        final UnitAttachment ua = UnitAttachment.get(carrier.getType());
        if (ua.getCarrierCapacity() == -1) {
          continue;
        }
        final Collection<Unit> fighters = dependencies.get(carrier);
        // Dependencies count both land and air units. Land units could be allied or owned, while air is just allied
        // since owned already launched at beginning of turn
        fighters.retainAll(CollectionUtils.getMatches(fighters, Matches.unitIsAir()));
        for (final Unit fighter : fighters) {
          // Set transportedBy for fighter
          change.add(ChangeFactory.unitPropertyChange(fighter, carrier, TripleAUnit.TRANSPORTED_BY));
        }
        // remove transported fighters from battle display
        m_attackingUnits.removeAll(fighters);
      }
    }
    addDependentUnits(dependencies);
    // mark units with no movement for all but air
    Collection<Unit> nonAir = CollectionUtils.getMatches(attackingUnits, Matches.unitIsNotAir());
    // we don't want to change the movement of transported land units if this is a sea battle
    // so restrict non air to remove land units
    if (m_battleSite.isWater()) {
      nonAir = CollectionUtils.getMatches(nonAir, Matches.unitIsNotLand());
    }
    // TODO: This checks for ignored sub/trns and skips the set of the attackers to 0 movement left
    // If attacker stops in an occupied territory, movement stops (battle is optional)
    if (MoveValidator.onlyIgnoredUnitsOnPath(route, m_attacker, m_data, false)) {
      return change;
    }
    change.add(ChangeFactory.markNoMovementChange(nonAir));
    return change;
  }

  void addDependentUnits(final Map<Unit, Collection<Unit>> dependencies) {
    for (final Unit holder : dependencies.keySet()) {
      final Collection<Unit> transporting = dependencies.get(holder);
      if (m_dependentUnits.get(holder) != null) {
        m_dependentUnits.get(holder).addAll(transporting);
      } else {
        m_dependentUnits.put(holder, new LinkedHashSet<>(transporting));
      }
    }
  }

  private String getBattleTitle() {
    return m_attacker.getName() + " attack " + m_defender.getName() + " in " + m_battleSite.getName();
  }

  private void updateDefendingAaUnits() {
    final Collection<Unit> canFire = new ArrayList<>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
    canFire.addAll(m_defendingUnits);
    canFire.addAll(m_defendingWaitingToDie);
    final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data);
    m_defendingAA = CollectionUtils.getMatches(canFire, Matches.unitIsAaThatCanFire(m_attackingUnits,
        airborneTechTargetsAllowed, m_attacker, Matches.unitIsAaForCombatOnly(), m_round, true, m_data));
    // comes ordered alphabetically
    m_defendingAAtypes = UnitAttachment.getAllOfTypeAAs(m_defendingAA);
    // stacks are backwards
    Collections.reverse(m_defendingAAtypes);
  }

  private void updateOffensiveAaUnits() {
    final Collection<Unit> canFire = new ArrayList<>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
    canFire.addAll(m_attackingUnits);
    canFire.addAll(m_attackingWaitingToDie);
    // no airborne targets for offensive aa
    m_offensiveAA = CollectionUtils.getMatches(canFire, Matches.unitIsAaThatCanFire(m_defendingUnits,
        new HashMap<>(), m_defender, Matches.unitIsAaForCombatOnly(), m_round, false, m_data));
    // comes ordered alphabetically
    m_offensiveAAtypes = UnitAttachment.getAllOfTypeAAs(m_offensiveAA);
    // stacks are backwards
    Collections.reverse(m_offensiveAAtypes);
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    // remove units that may already be dead due to a previous event (like they died from a strategic bombing raid,
    // rocket attack, etc)
    removeUnitsThatNoLongerExist();
    if (m_stack.isExecuting()) {
      final ITripleADisplay display = getDisplay(bridge);
      display.showBattle(m_battleID, m_battleSite, getBattleTitle(),
          removeNonCombatants(m_attackingUnits, true, false, false, false),
          removeNonCombatants(m_defendingUnits, false, false, false, false),
          m_killed, m_attackingWaitingToDie, m_defendingWaitingToDie, m_dependentUnits, m_attacker, m_defender,
          isAmphibious(), getBattleType(), m_amphibiousLandAttackers);
      display.listBattleSteps(m_battleID, m_stepStrings);
      m_stack.execute(bridge);
      return;
    }
    bridge.getHistoryWriter().startEvent("Battle in " + m_battleSite, m_battleSite);
    removeAirNoLongerInTerritory();
    writeUnitsToHistory(bridge);
    // it is possible that no attacking units are present, if so end now changed to only look at units that can be
    // destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
    if (CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsNotInfrastructure()).size() == 0) {
      endBattle(bridge);
      defenderWins(bridge);
      return;
    }
    // it is possible that no defending units exist, changed to only look at units that can be destroyed in combat, and
    // therefore not include factories, aaguns, and infrastructure.
    if (CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsNotInfrastructure()).size() == 0) {
      endBattle(bridge);
      attackerWins(bridge);
      return;
    }
    addDependentUnits(TransportTracker.transporting(m_defendingUnits));
    addDependentUnits(TransportTracker.transporting(m_attackingUnits));
    // determine any AA
    updateOffensiveAaUnits();
    updateDefendingAaUnits();
    m_stepStrings = determineStepStrings(true);
    final ITripleADisplay display = getDisplay(bridge);
    display.showBattle(m_battleID, m_battleSite, getBattleTitle(),
        removeNonCombatants(m_attackingUnits, true, false, false, false),
        removeNonCombatants(m_defendingUnits, false, false, false, false),
        m_killed, m_attackingWaitingToDie, m_defendingWaitingToDie, m_dependentUnits, m_attacker, m_defender,
        isAmphibious(), getBattleType(), m_amphibiousLandAttackers);
    display.listBattleSteps(m_battleID, m_stepStrings);
    if (!m_headless) {
      // take the casualties with least movement first
      if (isAmphibious()) {
        sortAmphib(m_attackingUnits);
      } else {
        BattleCalculator.sortPreBattle(m_attackingUnits);
      }
      BattleCalculator.sortPreBattle(m_defendingUnits);
      // play a sound
      if (m_attackingUnits.stream().anyMatch(Matches.unitIsSea())
          || m_defendingUnits.stream().anyMatch(Matches.unitIsSea())) {
        if ((!m_attackingUnits.isEmpty() && m_attackingUnits.stream().allMatch(Matches.unitIsSub()))
            || (m_attackingUnits.stream().anyMatch(Matches.unitIsSub())
                && m_defendingUnits.stream().anyMatch(Matches.unitIsSub()))) {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_SEA_SUBS, m_attacker);
        } else {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_SEA_NORMAL, m_attacker);
        }
      } else if (!m_attackingUnits.isEmpty() && m_attackingUnits.stream().allMatch(Matches.unitIsAir())
          && !m_defendingUnits.isEmpty() && m_defendingUnits.stream().allMatch(Matches.unitIsAir())) {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR, m_attacker);
      } else {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_LAND, m_attacker);
      }
    }
    // push on stack in opposite order of execution
    pushFightLoopOnStack(true);
    m_stack.execute(bridge);
  }

  private void writeUnitsToHistory(final IDelegateBridge bridge) {
    if (m_headless) {
      return;
    }
    final Set<PlayerID> playerSet = m_battleSite.getUnits().getPlayersWithUnits();
    // find all attacking players (unsorted)
    final Collection<PlayerID> attackers = new ArrayList<>();
    for (final PlayerID current : playerSet) {
      if (m_data.getRelationshipTracker().isAllied(m_attacker, current) || current.equals(m_attacker)) {
        attackers.add(current);
      }
    }
    final StringBuilder transcriptText = new StringBuilder();
    // find all attacking units (unsorted)
    final Collection<Unit> allAttackingUnits = new ArrayList<>();
    for (final Iterator<PlayerID> attackersIter = attackers.iterator(); attackersIter.hasNext();) {
      final PlayerID current = attackersIter.next();
      final String delim;
      if (attackersIter.hasNext()) {
        delim = "; ";
      } else {
        delim = "";
      }
      final Collection<Unit> attackingUnits =
          CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsOwnedBy(current));
      final String verb = current.equals(m_attacker) ? "attack" : "loiter and taunt";
      transcriptText
          .append(current.getName())
          .append(" ")
          .append(verb)
          .append(attackingUnits.isEmpty() ? "" : (" with " + MyFormatter.unitsToTextNoOwner(attackingUnits)))
          .append(delim);
      allAttackingUnits.addAll(attackingUnits);
      // If any attacking transports are in the battle, set their status to later restrict load/unload
      if (current.equals(m_attacker)) {
        final CompositeChange change = new CompositeChange();
        final Collection<Unit> transports = CollectionUtils.getMatches(attackingUnits, Matches.unitCanTransport());
        for (final Unit unit : transports) {
          change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.WAS_IN_COMBAT));
        }
        bridge.addChange(change);
      }
    }
    // write attacking units to history
    if (m_attackingUnits.size() > 0) {
      bridge.getHistoryWriter().addChildToEvent(transcriptText.toString(), allAttackingUnits);
    }
    // find all defending players (unsorted)
    final Collection<PlayerID> defenders = new ArrayList<>();
    for (final PlayerID current : playerSet) {
      if (m_data.getRelationshipTracker().isAllied(m_defender, current) || current.equals(m_defender)) {
        defenders.add(current);
      }
    }
    final StringBuilder transcriptBuilder = new StringBuilder();
    // find all defending units (unsorted)
    final Collection<Unit> allDefendingUnits = new ArrayList<>();
    for (final Iterator<PlayerID> defendersIter = defenders.iterator(); defendersIter.hasNext();) {
      final PlayerID current = defendersIter.next();
      final String delim;
      if (defendersIter.hasNext()) {
        delim = "; ";
      } else {
        delim = "";
      }
      final Collection<Unit> defendingUnits =
          CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsOwnedBy(current));
      transcriptBuilder
          .append(current.getName())
          .append(" defend with ")
          .append(MyFormatter.unitsToTextNoOwner(defendingUnits))
          .append(delim);
      allDefendingUnits.addAll(defendingUnits);
    }
    // write defending units to history
    if (m_defendingUnits.size() > 0) {
      bridge.getHistoryWriter().addChildToEvent(transcriptBuilder.toString(), allDefendingUnits);
    }
  }

  private void removeAirNoLongerInTerritory() {
    if (m_headless) {
      return;
    }
    // remove any air units that were once in this attack, but have now
    // moved out of the territory this is an inelegant way to handle this bug
    final Predicate<Unit> airNotInTerritory = Matches.unitIsInTerritory(m_battleSite).negate();
    m_attackingUnits.removeAll(CollectionUtils.getMatches(m_attackingUnits, airNotInTerritory));
  }

  List<String> determineStepStrings(final boolean showFirstRun) {
    final List<String> steps = new ArrayList<>();
    if (canFireOffensiveAa()) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAAs(m_offensiveAA)) {
        steps.add(m_attacker.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(m_defender.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(m_defender.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    if (canFireDefendingAa()) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAAs(m_defendingAA)) {
        steps.add(m_defender.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(m_attacker.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(m_attacker.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    if (showFirstRun) {
      if (!m_battleSite.isWater() && !getBombardingUnits().isEmpty()) {
        steps.add(NAVAL_BOMBARDMENT);
        steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
      }
      if (m_attackingUnits.stream().anyMatch(Matches.unitIsSuicide())) {
        steps.add(SUICIDE_ATTACK);
        steps.add(m_defender.getName() + SELECT_CASUALTIES_SUICIDE);
      }
      if (m_defendingUnits.stream().anyMatch(Matches.unitIsSuicide())
          && !isDefendingSuicideAndMunitionUnitsDoNotFire()) {
        steps.add(SUICIDE_DEFEND);
        steps.add(m_attacker.getName() + SELECT_CASUALTIES_SUICIDE);
      }
      if (!m_battleSite.isWater() && TechAttachment.isAirTransportable(m_attacker)) {
        final Collection<Unit> bombers =
            CollectionUtils.getMatches(m_battleSite.getUnits().getUnits(), Matches.unitIsAirTransport());
        if (!bombers.isEmpty()) {
          final Collection<Unit> dependents = getDependentUnits(bombers);
          if (!dependents.isEmpty()) {
            steps.add(LAND_PARATROOPS);
          }
        }
      }
    }
    // Check if defending subs can submerge before battle
    if (isSubRetreatBeforeBattle()) {
      if (!m_defendingUnits.stream().anyMatch(Matches.unitIsDestroyer())
          && m_attackingUnits.stream().anyMatch(Matches.unitIsSub())) {
        steps.add(m_attacker.getName() + SUBS_SUBMERGE);
      }
      if (!m_attackingUnits.stream().anyMatch(Matches.unitIsDestroyer())
          && m_defendingUnits.stream().anyMatch(Matches.unitIsSub())) {
        steps.add(m_defender.getName() + SUBS_SUBMERGE);
      }
    }
    // See if there any unescorted transports
    if (m_battleSite.isWater() && isTransportCasualtiesRestricted()) {
      if (m_attackingUnits.stream().anyMatch(Matches.unitIsTransport())
          || m_defendingUnits.stream().anyMatch(Matches.unitIsTransport())) {
        steps.add(REMOVE_UNESCORTED_TRANSPORTS);
      }
    }
    // if attacker has no sneak attack subs, then defender sneak attack subs fire first and remove casualties
    final boolean defenderSubsFireFirst = defenderSubsFireFirst();
    if (defenderSubsFireFirst && m_defendingUnits.stream().anyMatch(Matches.unitIsSub())) {
      steps.add(m_defender.getName() + SUBS_FIRE);
      steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    final boolean onlyAttackerSneakAttack = !defenderSubsFireFirst
        && (returnFireAgainstAttackingSubs() == ReturnFire.NONE) && (returnFireAgainstDefendingSubs()
        == ReturnFire.ALL);
    // attacker subs sneak attack, no sneak attack if destroyers are present
    if (m_battleSite.isWater()) {
      if (m_attackingUnits.stream().anyMatch(Matches.unitIsSub())) {
        steps.add(m_attacker.getName() + SUBS_FIRE);
        steps.add(m_defender.getName() + SELECT_SUB_CASUALTIES);
      }
      if (onlyAttackerSneakAttack) {
        steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
      }
    }
    // ww2v2 rules, all subs fire FIRST in combat, regardless of presence of destroyers.
    final boolean defendingSubsFireWithAllDefenders = !defenderSubsFireFirst
        && !Properties.getWW2V2(m_data) && (returnFireAgainstDefendingSubs() == ReturnFire.ALL);
    // defender subs sneak attack, no sneak attack in Pacific/Europe Theaters or if destroyers are present
    final boolean defendingSubsFireWithAllDefendersAlways = !defendingSubsSneakAttack3();
    if (m_battleSite.isWater()) {
      if (!defendingSubsFireWithAllDefendersAlways && !defendingSubsFireWithAllDefenders && !defenderSubsFireFirst
          && m_defendingUnits.stream().anyMatch(Matches.unitIsSub())) {
        steps.add(m_defender.getName() + SUBS_FIRE);
        steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
      }
    }
    if (m_battleSite.isWater() && !defenderSubsFireFirst && !onlyAttackerSneakAttack
        && ((returnFireAgainstDefendingSubs() != ReturnFire.ALL) || (returnFireAgainstAttackingSubs()
        != ReturnFire.ALL))) {
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    // Air units can't attack subs without Destroyers present
    if (isAirAttackSubRestricted() && m_attackingUnits.stream().anyMatch(Matches.unitIsAir())
        && !canAirAttackSubs(m_defendingUnits, m_attackingUnits)) {
      steps.add(SUBMERGE_SUBS_VS_AIR_ONLY);
      steps.add(AIR_ATTACK_NON_SUBS);
    }
    if (m_attackingUnits.stream().anyMatch(Matches.unitIsNotSub())) {
      steps.add(m_attacker.getName() + FIRE);
      steps.add(m_defender.getName() + SELECT_CASUALTIES);
    }
    // classic rules, subs fire with all defenders
    // also, ww2v3/global rules, defending subs without sneak attack fire with all defenders
    if (m_battleSite.isWater()) {
      final Collection<Unit> units = new ArrayList<>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
      units.addAll(m_defendingUnits);
      units.addAll(m_defendingWaitingToDie);
      if (units.stream().anyMatch(Matches.unitIsSub()) && !defenderSubsFireFirst
          && (defendingSubsFireWithAllDefenders || defendingSubsFireWithAllDefendersAlways)) {
        steps.add(m_defender.getName() + SUBS_FIRE);
        steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
      }
    }
    // Air Units can't attack subs without Destroyers present
    if (m_battleSite.isWater() && isAirAttackSubRestricted()) {
      final Collection<Unit> units = new ArrayList<>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
      units.addAll(m_defendingUnits);
      units.addAll(m_defendingWaitingToDie);
      if (m_defendingUnits.stream().anyMatch(Matches.unitIsAir()) && !canAirAttackSubs(m_attackingUnits, units)) {
        steps.add(AIR_DEFEND_NON_SUBS);
      }
    }
    if (m_defendingUnits.stream().anyMatch(Matches.unitIsNotSub())) {
      steps.add(m_defender.getName() + FIRE);
      steps.add(m_attacker.getName() + SELECT_CASUALTIES);
    }
    // remove casualties
    steps.add(REMOVE_CASUALTIES);
    // retreat subs
    if (m_battleSite.isWater()) {
      if (canSubsSubmerge()) {
        if (!isSubRetreatBeforeBattle()) {
          if (m_attackingUnits.stream().anyMatch(Matches.unitIsSub())) {
            steps.add(m_attacker.getName() + SUBS_SUBMERGE);
          }
          if (m_defendingUnits.stream().anyMatch(Matches.unitIsSub())) {
            steps.add(m_defender.getName() + SUBS_SUBMERGE);
          }
        }
      } else {
        if (canAttackerRetreatSubs()) {
          if (m_attackingUnits.stream().anyMatch(Matches.unitIsSub())) {
            steps.add(m_attacker.getName() + SUBS_WITHDRAW);
          }
        }
        if (canDefenderRetreatSubs()) {
          if (m_defendingUnits.stream().anyMatch(Matches.unitIsSub())) {
            steps.add(m_defender.getName() + SUBS_WITHDRAW);
          }
        }
      }
    }
    // if we are a sea zone, then we may not be able to retreat
    // (ie a sub traveled under another unit to get to the battle site)
    // or an enemy sub retreated to our sea zone
    // however, if all our sea units die, then
    // the air units can still retreat, so if we have any air units attacking in
    // a sea zone, we always have to have the retreat option shown
    // later, if our sea units die, we may ask the user to retreat
    final boolean someAirAtSea = m_battleSite.isWater() && m_attackingUnits.stream().anyMatch(Matches.unitIsAir());
    if (canAttackerRetreat() || someAirAtSea || canAttackerRetreatPartialAmphib() || canAttackerRetreatPlanes()) {
      steps.add(m_attacker.getName() + ATTACKER_WITHDRAW);
    }
    return steps;
  }

  private boolean defenderSubsFireFirst() {
    return (returnFireAgainstAttackingSubs() == ReturnFire.ALL) && (returnFireAgainstDefendingSubs()
        == ReturnFire.NONE);
  }

  private void addFightStartToStack(final boolean firstRun, final List<IExecutable> steps) {
    final boolean offensiveAa = canFireOffensiveAa();
    final boolean defendingAa = canFireDefendingAa();
    if (offensiveAa) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 3802352588499530533L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          fireOffensiveAaGuns();
        }
      });
    }
    if (defendingAa) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = -1370090785540214199L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          fireDefensiveAaGuns();
        }
      });
    }
    if (offensiveAa || defendingAa) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 8762796262264296436L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          clearWaitingToDieAndDamagedChangesInto(bridge);
        }
      });
    }
    if (m_round > 1) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 2781652892457063082L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          removeNonCombatants(bridge, false, false, true);
        }
      });
    }
    if (firstRun) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = -2255284529092427441L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          fireNavalBombardment(bridge);
        }
      });
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 6578267830066963474L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          fireSuicideUnitsAttack();
        }
      });
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 2731652892447063082L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          fireSuicideUnitsDefend();
        }
      });
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 3389635558184415797L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          removeNonCombatants(bridge, false, false, true);
        }
      });
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 7193353768857658286L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          landParatroops(bridge);
        }
      });
      steps.add(new IExecutable() {
        private static final long serialVersionUID = -6676316363537467594L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          markNoMovementLeft(bridge);
        }
      });
    }
  }

  private void pushFightLoopOnStack(final boolean firstRun) {
    if (m_isOver) {
      return;
    }
    final List<IExecutable> steps = getBattleExecutables(firstRun);
    // add in the reverse order we create them
    Collections.reverse(steps);
    for (final IExecutable step : steps) {
      m_stack.push(step);
    }
    return;
  }

  List<IExecutable> getBattleExecutables(final boolean firstRun) {
    // The code here is a bit odd to read but basically, we need to break the code into separate atomic pieces.
    // If there is a network error, or some other unfortunate event, then we need to keep track of what pieces we have
    // executed, and what is left to do.
    // Each atomic step is in its own IExecutable with the definition of atomic is that either:
    // 1) the code does not call to an IDisplay,IPlayer, or IRandomSource
    // 2) if the code calls to an IDisplay, IPlayer, IRandomSource, and an exception is called from one of those
    // methods, the exception will be propagated out of execute() and the execute method can be called again.
    // It is allowed for an IExecutable to add other IExecutables to the stack.
    // If you read the code in linear order, ignore wrapping stuff in anonymous IExecutables, then the code
    // can be read as it will execute. The steps are added to the stack and then reversed at the end.
    final List<IExecutable> steps = new ArrayList<>();
    addFightStartToStack(firstRun, steps);
    addFightStepsNonEditMode(steps);

    steps.add(new IExecutable() {
      private static final long serialVersionUID = 8611067962952500496L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        clearWaitingToDieAndDamagedChangesInto(bridge);
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 6387198382888361848L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        checkSuicideUnits(bridge);
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 5259103822937067667L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsNotInfrastructure()).size() == 0) {
          if (!isTransportCasualtiesRestricted()) {
            endBattle(bridge);
            defenderWins(bridge);
          } else {
            // Get all allied transports in the territory
            final Predicate<Unit> matchAllied = Matches.unitIsTransport()
                .and(Matches.unitIsNotCombatTransport())
                .and(Matches.isUnitAllied(m_attacker, m_data));
            final List<Unit> alliedTransports =
                CollectionUtils.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);
            // If no transports, just end the battle
            if (alliedTransports.isEmpty()) {
              endBattle(bridge);
              defenderWins(bridge);
            } else if (m_round <= 1) {
              m_attackingUnits =
                  CollectionUtils.getMatches(m_battleSite.getUnits().getUnits(), Matches.unitIsOwnedBy(m_attacker));
            } else {
              endBattle(bridge);
              defenderWins(bridge);
            }
          }
        } else if (CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsNotInfrastructure()).size() == 0) {
          if (isTransportCasualtiesRestricted()) {
            // If there are undefended attacking transports, determine if they automatically die
            checkUndefendedTransports(bridge, m_defender);
          }
          checkForUnitsThatCanRollLeft(bridge, false);
          endBattle(bridge);
          attackerWins(bridge);
        } else if (shouldEndBattleDueToMaxRounds()
            || (!m_attackingUnits.isEmpty()
                && m_attackingUnits.stream().allMatch(Matches.unitHasAttackValueOfAtLeast(1).negate())
                && !m_defendingUnits.isEmpty()
                && m_defendingUnits.stream().allMatch(Matches.unitHasDefendValueOfAtLeast(1).negate()))) {
          endBattle(bridge);
          nobodyWins(bridge);
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 6775880082912594489L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver && canAttackerRetreatSubs() && !isSubRetreatBeforeBattle()) {
          attackerRetreatSubs(bridge);
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -1544916305666912480L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver) {
          if (canDefenderRetreatSubs() && !isSubRetreatBeforeBattle()) {
            defenderRetreatSubs(bridge);
          }
          // If no defenders left, then battle is over. The reason we test a "second" time here, is because otherwise
          // the attackers can retreat even though the battle is over (illegal).
          if (m_defendingUnits.isEmpty()) {
            endBattle(bridge);
            attackerWins(bridge);
          }
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -1150863964807721395L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver && canAttackerRetreatPlanes() && !canAttackerRetreatPartialAmphib()) {
          attackerRetreatPlanes(bridge);
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -1150863964807721395L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver && canAttackerRetreatPartialAmphib()) {
          attackerRetreatNonAmphibUnits(bridge);
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 669349383898975048L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver) {
          attackerRetreat(bridge);
        }
      }
    });
    final IExecutable loop = new IExecutable() {
      private static final long serialVersionUID = 3118458517320468680L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        pushFightLoopOnStack(false);
      }
    };
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -3993599528368570254L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver) {
          m_round++;
          // determine any AA
          updateOffensiveAaUnits();
          updateDefendingAaUnits();
          m_stepStrings = determineStepStrings(false);
          final ITripleADisplay display = getDisplay(bridge);
          display.listBattleSteps(m_battleID, m_stepStrings);
          // continue fighting the recursive steps
          // this should always be the base of the stack
          // when we execute the loop, it will populate the stack with the battle steps
          if (!m_stack.isEmpty()) {
            throw new IllegalStateException("Stack not empty:" + m_stack);
          }
          m_stack.push(loop);
        }
      }
    });
    return steps;
  }

  private void addFightStepsNonEditMode(final List<IExecutable> steps) {
    // Ask to retreat defending subs before battle
    if (isSubRetreatBeforeBattle()) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 6775880082912594489L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          if (!m_isOver) {
            attackerRetreatSubs(bridge);
          }
        }
      });
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 7056448091800764539L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          if (!m_isOver) {
            defenderRetreatSubs(bridge);
          }
        }
      });
    }
    // Remove Suicide Units
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 99988L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        checkSuicideUnits(bridge);
      }
    });
    // Remove undefended transports
    if (isTransportCasualtiesRestricted()) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 99989L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          checkUndefendedTransports(bridge, m_defender);
          checkUndefendedTransports(bridge, m_attacker);
          checkForUnitsThatCanRollLeft(bridge, true);
          checkForUnitsThatCanRollLeft(bridge, false);
        }
      });
    }
    // Submerge subs if -vs air only & air restricted from attacking subs
    if (isAirAttackSubRestricted()) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 99990L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          submergeSubsVsOnlyAir(bridge);
        }
      });
    }
    final ReturnFire returnFireAgainstAttackingSubs = returnFireAgainstAttackingSubs();
    final ReturnFire returnFireAgainstDefendingSubs = returnFireAgainstDefendingSubs();
    if (defenderSubsFireFirst()) {
      steps.add(new DefendSubs() {
        private static final long serialVersionUID = 99992L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          defendSubs(returnFireAgainstDefendingSubs);
        }
      });
    }
    steps.add(new AttackSubs() {
      private static final long serialVersionUID = 99991L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        attackSubs(returnFireAgainstAttackingSubs);
      }
    });
    final boolean defendingSubsFireWithAllDefenders = !defenderSubsFireFirst()
        && !Properties.getWW2V2(m_data) && (returnFireAgainstDefendingSubs() == ReturnFire.ALL);
    if (defendingSubsSneakAttack3() && !defenderSubsFireFirst() && !defendingSubsFireWithAllDefenders) {
      steps.add(new DefendSubs() {
        private static final long serialVersionUID = 99992L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          defendSubs(returnFireAgainstDefendingSubs);
        }
      });
    }
    // Attacker air fire on non-subs
    if (isAirAttackSubRestricted()) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 99993L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          attackAirOnNonSubs();
        }
      });
    }
    // Attacker fire remaining units
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 99994L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        attackNonSubs();
      }
    });
    if (!defenderSubsFireFirst() && (!defendingSubsSneakAttack3() || defendingSubsFireWithAllDefenders)) {
      steps.add(new DefendSubs() {
        private static final long serialVersionUID = 999921L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          defendSubs(returnFireAgainstDefendingSubs);
        }
      });
    }
    // Defender air fire on non-subs
    if (isAirAttackSubRestricted()) {
      steps.add(new IExecutable() {
        private static final long serialVersionUID = 1560702114917865123L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          defendAirOnNonSubs();
        }
      });
    }
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 1560702114917865290L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        defendNonSubs();
      }
    });
  }

  private ReturnFire returnFireAgainstAttackingSubs() {
    final boolean attackingSubsSneakAttack = !m_defendingUnits.stream().anyMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack = defendingSubsSneakAttack2();
    final ReturnFire returnFireAgainstAttackingSubs;
    if (!attackingSubsSneakAttack) {
      returnFireAgainstAttackingSubs = ReturnFire.ALL;
    } else if (defendingSubsSneakAttack || isWW2V2()) {
      returnFireAgainstAttackingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstAttackingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstAttackingSubs;
  }

  private ReturnFire returnFireAgainstDefendingSubs() {
    final boolean attackingSubsSneakAttack = !m_defendingUnits.stream().anyMatch(Matches.unitIsDestroyer());
    final boolean defendingSubsSneakAttack = defendingSubsSneakAttack2();
    final ReturnFire returnFireAgainstDefendingSubs;
    if (!defendingSubsSneakAttack) {
      returnFireAgainstDefendingSubs = ReturnFire.ALL;
    } else if (attackingSubsSneakAttack || isWW2V2()) {
      returnFireAgainstDefendingSubs = ReturnFire.SUBS;
    } else {
      returnFireAgainstDefendingSubs = ReturnFire.NONE;
    }
    return returnFireAgainstDefendingSubs;
  }

  private boolean defendingSubsSneakAttack2() {
    return !m_attackingUnits.stream().anyMatch(Matches.unitIsDestroyer()) && defendingSubsSneakAttack3();
  }

  private boolean defendingSubsSneakAttack3() {
    return isWW2V2() || isDefendingSubsSneakAttack();
  }

  private boolean canAttackerRetreatPlanes() {
    return (isWW2V2() || isAttackerRetreatPlanes() || isPartialAmphibiousRetreat()) && m_isAmphibious
        && m_attackingUnits.stream().anyMatch(Matches.unitIsAir());
  }

  private boolean canAttackerRetreatPartialAmphib() {
    if (m_isAmphibious && isPartialAmphibiousRetreat()) {
      // Only include land units when checking for allow amphibious retreat
      final List<Unit> landUnits = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsLand());
      for (final Unit unit : landUnits) {
        final TripleAUnit taUnit = (TripleAUnit) unit;
        if (!taUnit.getWasAmphibious()) {
          return true;
        }
      }
    }
    return false;
  }

  Collection<Territory> getAttackerRetreatTerritories() {
    // TODO: when attacking with paratroopers (air + carried land), there are several bugs in retreating.
    // TODO: air should always be able to retreat. paratroopers can only retreat if there are other
    // non-paratrooper non-amphibious land units.

    // If attacker is all planes, just return collection of current territory
    if (m_headless || (!m_attackingUnits.isEmpty() && m_attackingUnits.stream().allMatch(Matches.unitIsAir()))
        || Properties.getRetreatingUnitsRemainInPlace(m_data)) {
      final Collection<Territory> oneTerritory = new ArrayList<>(2);
      oneTerritory.add(m_battleSite);
      return oneTerritory;
    }
    // its possible that a sub retreated to a territory we came from, if so we can no longer retreat there
    // or if we are moving out of a territory containing enemy units, we cannot retreat back there
    final Predicate<Unit> enemyUnitsThatPreventRetreat = PredicateBuilder
        .of(Matches.enemyUnit(m_attacker, m_data))
        .and(Matches.unitIsNotInfrastructure())
        .and(Matches.unitIsBeingTransported().negate())
        .and(Matches.unitIsSubmerged().negate())
        .andIf(Properties.getIgnoreSubInMovement(m_data), Matches.unitIsNotSub())
        .andIf(Properties.getIgnoreTransportInMovement(m_data), Matches.unitIsNotTransportButCouldBeCombatTransport())
        .build();
    Collection<Territory> possible = CollectionUtils.getMatches(m_attackingFrom,
        Matches.territoryHasUnitsThatMatch(enemyUnitsThatPreventRetreat).negate());
    // In WW2V2 and WW2V3 we need to filter out territories where only planes
    // came from since planes cannot define retreat paths
    if (isWW2V2() || isWW2V3()) {
      possible = CollectionUtils.getMatches(possible, t -> {
        final Collection<Unit> units = m_attackingFromMap.get(t);
        return units.isEmpty() || !units.stream().allMatch(Matches.unitIsAir());
      });
    }

    // the air unit may have come from a conquered or enemy territory, don't allow retreating
    final Predicate<Territory> conqueuredOrEnemy =
        Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(m_attacker, m_data)
            .or(Matches.territoryIsWater().and(Matches.territoryWasFoughOver(m_battleTracker)));
    possible.removeAll(CollectionUtils.getMatches(possible, conqueuredOrEnemy));

    // the battle site is in the attacking from if sea units are fighting a submerged sub
    possible.remove(m_battleSite);
    if (m_attackingUnits.stream().anyMatch(Matches.unitIsLand()) && !m_battleSite.isWater()) {
      possible = CollectionUtils.getMatches(possible, Matches.territoryIsLand());
    }
    if (m_attackingUnits.stream().anyMatch(Matches.unitIsSea())) {
      possible = CollectionUtils.getMatches(possible, Matches.territoryIsWater());
    }
    return possible;
  }

  private boolean canAttackerRetreat() {
    if (onlyDefenselessDefendingTransportsLeft()) {
      return false;
    }
    if (m_isAmphibious) {
      return false;
    }
    final Collection<Territory> options = getAttackerRetreatTerritories();
    return options.size() != 0;
  }

  private boolean onlyDefenselessDefendingTransportsLeft() {
    if (!isTransportCasualtiesRestricted()) {
      return false;
    }
    return !m_defendingUnits.isEmpty()
        && m_defendingUnits.stream().allMatch(Matches.unitIsTransportButNotCombatTransport());
  }

  private boolean canAttackerRetreatSubs() {
    if (m_defendingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    if (m_defendingWaitingToDie.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return canAttackerRetreat() || canSubsSubmerge();
  }

  // Added for test case calls
  void externalRetreat(final Collection<Unit> retreaters, final Territory retreatTo, final boolean defender,
      final IDelegateBridge bridge) {
    m_isOver = true;
    retreatUnits(retreaters, retreatTo, defender, bridge);
  }

  private void attackerRetreat(final IDelegateBridge bridge) {
    if (!canAttackerRetreat()) {
      return;
    }
    final Collection<Territory> possible = getAttackerRetreatTerritories();
    if (!m_isOver) {
      if (m_isAmphibious) {
        queryRetreat(false, RetreatType.PARTIAL_AMPHIB, bridge, possible);
      } else {
        queryRetreat(false, RetreatType.DEFAULT, bridge, possible);
      }
    }
  }

  private void attackerRetreatPlanes(final IDelegateBridge bridge) {
    // planes retreat to the same square the battle is in, and then should move during non combat to their landing site,
    // or be scrapped if they can't find one.
    final Collection<Territory> possible = new ArrayList<>(2);
    possible.add(m_battleSite);
    if (m_attackingUnits.stream().anyMatch(Matches.unitIsAir())) {
      queryRetreat(false, RetreatType.PLANES, bridge, possible);
    }
  }

  private void attackerRetreatNonAmphibUnits(final IDelegateBridge bridge) {
    final Collection<Territory> possible = getAttackerRetreatTerritories();
    queryRetreat(false, RetreatType.PARTIAL_AMPHIB, bridge, possible);
  }

  private boolean canDefenderRetreatSubs() {
    if (m_attackingUnits.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    if (m_attackingWaitingToDie.stream().anyMatch(Matches.unitIsDestroyer())) {
      return false;
    }
    return
        (getEmptyOrFriendlySeaNeighbors(m_defender, CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsSub()))
            .size() != 0) || canSubsSubmerge();
  }

  private void attackerRetreatSubs(final IDelegateBridge bridge) {
    if (!canAttackerRetreatSubs()) {
      return;
    }
    if (m_attackingUnits.stream().anyMatch(Matches.unitIsSub())) {
      queryRetreat(false, RetreatType.SUBS, bridge, getAttackerRetreatTerritories());
    }
  }

  private void defenderRetreatSubs(final IDelegateBridge bridge) {
    if (!canDefenderRetreatSubs()) {
      return;
    }
    if (!m_isOver && m_defendingUnits.stream().anyMatch(Matches.unitIsSub())) {
      queryRetreat(true, RetreatType.SUBS, bridge, getEmptyOrFriendlySeaNeighbors(m_defender,
          CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsSub())));
    }
  }

  private Collection<Territory> getEmptyOrFriendlySeaNeighbors(final PlayerID player,
      final Collection<Unit> unitsToRetreat) {
    Collection<Territory> possible = m_data.getMap().getNeighbors(m_battleSite);
    if (m_headless) {
      return possible;
    }
    // make sure we can move through the any canals
    final Predicate<Territory> canalMatch = t -> {
      final Route r = new Route();
      r.setStart(m_battleSite);
      r.add(t);
      return MoveValidator.validateCanal(r, unitsToRetreat, m_defender, m_data) == null;
    };
    final Predicate<Territory> match = Matches.territoryIsWater()
        .and(Matches.territoryHasNoEnemyUnits(player, m_data))
        .and(canalMatch);
    possible = CollectionUtils.getMatches(possible, match);
    return possible;
  }

  private void queryRetreat(final boolean defender, final RetreatType retreatType, final IDelegateBridge bridge,
      Collection<Territory> availableTerritories) {
    final boolean planes = retreatType == RetreatType.PLANES;
    final boolean subs = retreatType == RetreatType.SUBS;
    final boolean canSubsSubmerge = canSubsSubmerge();
    final boolean canDefendingSubsSubmergeOrRetreat =
        subs && defender && Properties.getSubmarinesDefendingMaySubmergeOrRetreat(m_data);
    final boolean partialAmphib = retreatType == RetreatType.PARTIAL_AMPHIB;
    final boolean submerge = subs && canSubsSubmerge;
    if (availableTerritories.isEmpty() && !(submerge || canDefendingSubsSubmergeOrRetreat)) {
      return;
    }

    // If attacker then add all owned units at battle site as some might have been removed from battle (infra)
    Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
    if (!defender) {
      units = new HashSet<>(units);
      units.addAll(m_battleSite.getUnits().getMatches(Matches.unitIsOwnedBy(m_attacker)));
      units.removeAll(m_killed);
    }
    if (subs) {
      units = CollectionUtils.getMatches(units, Matches.unitIsSub());
    } else if (planes) {
      units = CollectionUtils.getMatches(units, Matches.unitIsAir());
    } else if (partialAmphib) {
      units = CollectionUtils.getMatches(units, Matches.unitWasNotAmphibious());
    }
    if (units.stream().anyMatch(Matches.unitIsSea())) {
      availableTerritories = CollectionUtils.getMatches(availableTerritories, Matches.territoryIsWater());
    }
    if (canDefendingSubsSubmergeOrRetreat) {
      availableTerritories.add(m_battleSite);
    } else if (submerge) {
      availableTerritories.clear();
      availableTerritories.add(m_battleSite);
    }
    if (planes) {
      availableTerritories.clear();
      availableTerritories.add(m_battleSite);
    }
    if (units.size() == 0) {
      return;
    }
    final PlayerID retreatingPlayer = defender ? m_defender : m_attacker;
    final String text;
    if (subs) {
      text = retreatingPlayer.getName() + " retreat subs?";
    } else if (planes) {
      text = retreatingPlayer.getName() + " retreat planes?";
    } else if (partialAmphib) {
      text = retreatingPlayer.getName() + " retreat non-amphibious units?";
    } else {
      text = retreatingPlayer.getName() + " retreat?";
    }
    final String step;
    if (defender) {
      step = m_defender.getName() + (canSubsSubmerge ? SUBS_SUBMERGE : SUBS_WITHDRAW);
    } else {
      if (subs) {
        step = m_attacker.getName() + (canSubsSubmerge ? SUBS_SUBMERGE : SUBS_WITHDRAW);
      } else {
        step = m_attacker.getName() + ATTACKER_WITHDRAW;
      }
    }
    getDisplay(bridge).gotoBattleStep(m_battleID, step);
    final Territory retreatTo = getRemote(retreatingPlayer, bridge).retreatQuery(m_battleID,
        (submerge || canDefendingSubsSubmergeOrRetreat), m_battleSite, availableTerritories, text);
    if ((retreatTo != null) && !availableTerritories.contains(retreatTo) && !subs) {
      System.err.println("Invalid retreat selection :" + retreatTo + " not in "
          + MyFormatter.defaultNamedToTextList(availableTerritories));
      Thread.dumpStack();
      return;
    }
    if (retreatTo != null) {
      // if attacker retreating non subs then its all over
      if (!defender && !subs && !planes && !partialAmphib) {
        // this is illegal in ww2v2 revised and beyond (the fighters should die). still checking if illegal in classic.
        m_isOver = true;
      }
      if (subs && m_battleSite.equals(retreatTo) && (submerge || canDefendingSubsSubmergeOrRetreat)) {
        if (!m_headless) {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_SUBMERGE, m_attacker);
        }
        submergeUnits(units, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " submerges subs";
        getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
      } else if (planes) {
        if (!m_headless) {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker);
        }
        retreatPlanes(units, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " retreats planes";
        getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
      } else if (partialAmphib) {
        if (!m_headless) {
          if (units.stream().anyMatch(Matches.unitIsSea())) {
            bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_SEA, m_attacker);
          } else if (units.stream().anyMatch(Matches.unitIsLand())) {
            bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_LAND, m_attacker);
          } else {
            bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker);
          }
        }
        // remove amphib units from those retreating
        units = CollectionUtils.getMatches(units, Matches.unitWasNotAmphibious());
        retreatUnitsAndPlanes(units, retreatTo, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " retreats non-amphibious units";
        getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
      } else {
        if (!m_headless) {
          if (units.stream().anyMatch(Matches.unitIsSea())) {
            bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_SEA, m_attacker);
          } else if (units.stream().anyMatch(Matches.unitIsLand())) {
            bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_LAND, m_attacker);
          } else {
            bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker);
          }
        }
        retreatUnits(units, retreatTo, defender, bridge);
        final String messageShort = retreatingPlayer.getName() + " retreats";
        final String messageLong;
        if (subs) {
          messageLong = retreatingPlayer.getName() + " retreats subs to " + retreatTo.getName();
        } else if (planes) {
          messageLong = retreatingPlayer.getName() + " retreats planes to " + retreatTo.getName();
        } else if (partialAmphib) {
          messageLong = retreatingPlayer.getName() + " retreats non-amphibious units to " + retreatTo.getName();
        } else {
          messageLong = retreatingPlayer.getName() + " retreats all units to " + retreatTo.getName();
        }
        getDisplay(bridge).notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
      }
    }
  }

  @Override
  public List<Unit> getRemainingAttackingUnits() {
    final ArrayList<Unit> remaining = new ArrayList<>(m_attackingUnits);
    remaining.addAll(m_attackingUnitsRetreated);
    return remaining;
  }

  @Override
  public List<Unit> getRemainingDefendingUnits() {
    final ArrayList<Unit> remaining = new ArrayList<>(m_defendingUnits);
    remaining.addAll(m_defendingUnitsRetreated);
    return remaining;
  }

  private Change retreatFromDependents(final Collection<Unit> units, final Territory retreatTo,
      final Collection<IBattle> dependentBattles) {
    final CompositeChange change = new CompositeChange();
    for (final IBattle dependent : dependentBattles) {
      final Route route = new Route();
      route.setStart(m_battleSite);
      route.add(dependent.getTerritory());
      final Collection<Unit> retreatedUnits = dependent.getDependentUnits(units);
      dependent.removeAttack(route, retreatedUnits);
      reLoadTransports(units, change);
      change.add(ChangeFactory.moveUnits(dependent.getTerritory(), retreatTo, retreatedUnits));
    }
    return change;
  }

  /**
   * Retreat landed units from allied territory when their transport retreats.
   */
  private Change retreatFromNonCombat(Collection<Unit> units, final Territory retreatTo) {
    final CompositeChange change = new CompositeChange();
    units = CollectionUtils.getMatches(units, Matches.unitIsTransport());
    final Collection<Unit> retreated = getTransportDependents(units);
    if (!retreated.isEmpty()) {
      for (final Unit unit : units) {
        final Territory retreatedFrom = TransportTracker.getTerritoryTransportHasUnloadedTo(unit);
        if (retreatedFrom != null) {
          reLoadTransports(units, change);
          change.add(ChangeFactory.moveUnits(retreatedFrom, retreatTo, retreated));
        }
      }
    }
    return change;
  }

  void reLoadTransports(final Collection<Unit> units, final CompositeChange change) {
    final Collection<Unit> transports = CollectionUtils.getMatches(units, Matches.unitCanTransport());
    // Put units back on their transports
    for (final Unit transport : transports) {
      final Collection<Unit> unloaded = TransportTracker.unloaded(transport);
      for (final Unit load : unloaded) {
        final Change loadChange = TransportTracker.loadTransportChange((TripleAUnit) transport, load);
        change.add(loadChange);
      }
    }
  }

  private void retreatPlanes(final Collection<Unit> retreating, final boolean defender, final IDelegateBridge bridge) {
    final String transcriptText = MyFormatter.unitsToText(retreating) + " retreated";
    final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
    final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
    units.removeAll(retreating);
    unitsRetreated.removeAll(retreating);
    if (units.isEmpty() || m_isOver) {
      endBattle(bridge);
      if (defender) {
        attackerWins(bridge);
      } else {
        defenderWins(bridge);
      }
    } else {
      getDisplay(bridge).notifyRetreat(m_battleID, retreating);
    }
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(retreating));
  }

  private void submergeUnits(final Collection<Unit> submerging, final boolean defender, final IDelegateBridge bridge) {
    final String transcriptText = MyFormatter.unitsToText(submerging) + " Submerged";
    final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
    final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
    final CompositeChange change = new CompositeChange();
    for (final Unit u : submerging) {
      change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.SUBMERGED));
    }
    bridge.addChange(change);
    units.removeAll(submerging);
    unitsRetreated.addAll(submerging);
    if (!units.isEmpty() && !m_isOver) {
      getDisplay(bridge).notifyRetreat(m_battleID, submerging);
    }
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(submerging));
  }

  private void retreatUnits(Collection<Unit> retreating, final Territory to, final boolean defender,
      final IDelegateBridge bridge) {
    retreating.addAll(getDependentUnits(retreating));
    // our own air units don't retreat with land units
    final Predicate<Unit> notMyAir = Matches.unitIsNotAir().or(Matches.unitIsOwnedBy(m_attacker).negate());
    retreating = CollectionUtils.getMatches(retreating, notMyAir);
    final String transcriptText;
    // in WW2V1, defending subs can retreat so show owner
    if (isWW2V2()) {
      transcriptText = MyFormatter.unitsToTextNoOwner(retreating) + " retreated to " + to.getName();
    } else {
      transcriptText = MyFormatter.unitsToText(retreating) + " retreated to " + to.getName();
    }
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(retreating));
    final CompositeChange change = new CompositeChange();
    change.add(ChangeFactory.moveUnits(m_battleSite, to, retreating));
    if (m_isOver) {
      final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(this);
      // If there are no dependent battles, check landings in allied territories
      if (dependentBattles.isEmpty()) {
        change.add(retreatFromNonCombat(retreating, to));
        // Else retreat the units from combat when their transport retreats
      } else {
        change.add(retreatFromDependents(retreating, to, dependentBattles));
      }
    }
    bridge.addChange(change);
    final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
    final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
    units.removeAll(retreating);
    unitsRetreated.addAll(retreating);
    if (units.isEmpty() || m_isOver) {
      endBattle(bridge);
      if (defender) {
        attackerWins(bridge);
      } else {
        defenderWins(bridge);
      }
    } else {
      getDisplay(bridge).notifyRetreat(m_battleID, retreating);
    }
  }

  private void retreatUnitsAndPlanes(final Collection<Unit> retreating, final Territory to, final boolean defender,
      final IDelegateBridge bridge) {
    // Remove air from battle
    final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
    final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
    units.removeAll(CollectionUtils.getMatches(units, Matches.unitIsAir()));
    // add all land units' dependents
    retreating.addAll(getDependentUnits(units));
    // our own air units don't retreat with land units
    final Predicate<Unit> notMyAir = Matches.unitIsNotAir().or(Matches.unitIsOwnedBy(m_attacker).negate());
    final Collection<Unit> nonAirRetreating = CollectionUtils.getMatches(retreating, notMyAir);
    final String transcriptText = MyFormatter.unitsToTextNoOwner(nonAirRetreating) + " retreated to " + to.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(nonAirRetreating));
    final CompositeChange change = new CompositeChange();
    change.add(ChangeFactory.moveUnits(m_battleSite, to, nonAirRetreating));
    if (m_isOver) {
      final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(this);
      // If there are no dependent battles, check landings in allied territories
      if (dependentBattles.isEmpty()) {
        change.add(retreatFromNonCombat(nonAirRetreating, to));
        // Else retreat the units from combat when their transport retreats
      } else {
        change.add(retreatFromDependents(nonAirRetreating, to, dependentBattles));
      }
    }
    bridge.addChange(change);
    units.removeAll(nonAirRetreating);
    unitsRetreated.addAll(nonAirRetreating);
    if (units.isEmpty() || m_isOver) {
      endBattle(bridge);
      if (defender) {
        attackerWins(bridge);
      } else {
        defenderWins(bridge);
      }
    } else {
      getDisplay(bridge).notifyRetreat(m_battleID, retreating);
    }
  }

  private void fire(final String stepName, final Collection<Unit> firingUnits, final Collection<Unit> attackableUnits,
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie, final boolean defender, final ReturnFire returnFire,
      final String text) {
    final PlayerID firing = defender ? m_defender : m_attacker;
    final PlayerID defending = !defender ? m_defender : m_attacker;
    if (firingUnits.isEmpty()) {
      return;
    }

    // Fire each type of suicide on hit unit separately and then remaining units
    final List<Collection<Unit>> firingGroups = createFiringUnitGroups(firingUnits);
    for (final Collection<Unit> units : firingGroups) {
      m_stack.push(new Fire(attackableUnits, returnFire, firing, defending, units, stepName, text, this, defender,
          m_dependentUnits, m_headless, m_battleSite, m_territoryEffects, allEnemyUnitsAliveOrWaitingToDie));
    }
  }

  /**
   * Breaks list of units into groups of non suicide on hit units and each type of suicide on hit units
   * since each type of suicide on hit units need to roll separately to know which ones get hits.
   */
  private static List<Collection<Unit>> createFiringUnitGroups(final Collection<Unit> units) {

    // Sort suicide on hit units by type
    final Map<UnitType, Collection<Unit>> map = new HashMap<>();
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit())) {
      final UnitType type = unit.getType();
      if (map.containsKey(type)) {
        map.get(type).add(unit);
      } else {
        final Collection<Unit> unitList = new ArrayList<>();
        unitList.add(unit);
        map.put(type, unitList);
      }
    }

    // Add all suicide on hit groups and the remaining units
    final List<Collection<Unit>> result = new ArrayList<>();
    result.addAll(map.values());
    final Collection<Unit> remainingUnits = CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit().negate());
    if (!remainingUnits.isEmpty()) {
      result.add(remainingUnits);
    }
    return result;
  }

  /**
   * Check for suicide units and kill them immediately (they get to shoot back, which is the point).
   */
  private void checkSuicideUnits(final IDelegateBridge bridge) {
    if (isDefendingSuicideAndMunitionUnitsDoNotFire()) {
      final List<Unit> deadUnits = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsSuicide());
      getDisplay(bridge).deadUnitNotification(m_battleID, m_attacker, deadUnits, m_dependentUnits);
      remove(deadUnits, bridge, m_battleSite, false);
    } else {
      final List<Unit> deadUnits = new ArrayList<>();
      deadUnits.addAll(CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsSuicide()));
      deadUnits.addAll(CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsSuicide()));
      getDisplay(bridge).deadUnitNotification(m_battleID, m_attacker, deadUnits, m_dependentUnits);
      getDisplay(bridge).deadUnitNotification(m_battleID, m_defender, deadUnits, m_dependentUnits);
      remove(deadUnits, bridge, m_battleSite, null);
    }
  }

  /**
   * Check for unescorted transports and kill them immediately.
   */
  private void checkUndefendedTransports(final IDelegateBridge bridge, final PlayerID player) {
    // if we are the attacker, we can retreat instead of dying
    if (player.equals(m_attacker)
        && (!getAttackerRetreatTerritories().isEmpty() || m_attackingUnits.stream().anyMatch(Matches.unitIsAir()))) {
      return;
    }
    // Get all allied transports in the territory
    final Predicate<Unit> matchAllied = Matches.unitIsTransport()
        .and(Matches.unitIsNotCombatTransport())
        .and(Matches.isUnitAllied(player, m_data))
        .and(Matches.unitIsSea());
    final List<Unit> alliedTransports = CollectionUtils.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);
    // If no transports, just return
    if (alliedTransports.isEmpty()) {
      return;
    }
    // Get all ALLIED, sea & air units in the territory (that are NOT submerged)
    final Predicate<Unit> alliedUnitsMatch = Matches.isUnitAllied(player, m_data)
        .and(Matches.unitIsNotLand())
        .and(Matches.unitIsSubmerged().negate());
    final Collection<Unit> alliedUnits =
        CollectionUtils.getMatches(m_battleSite.getUnits().getUnits(), alliedUnitsMatch);
    // If transports are unescorted, check opposing forces to see if the Trns die automatically
    if (alliedTransports.size() == alliedUnits.size()) {
      // Get all the ENEMY sea and air units (that can attack) in the territory
      final Predicate<Unit> enemyUnitsMatch = Matches.unitIsNotLand()
          .and(Matches.unitIsSubmerged().negate())
          .and(Matches.unitCanAttack(player));
      final Collection<Unit> enemyUnits =
          CollectionUtils.getMatches(m_battleSite.getUnits().getUnits(), enemyUnitsMatch);
      // If there are attackers set their movement to 0 and kill the transports
      if (enemyUnits.size() > 0) {
        final Change change =
            ChangeFactory.markNoMovementChange(CollectionUtils.getMatches(enemyUnits, Matches.unitIsSea()));
        bridge.addChange(change);
        final boolean defender = player.equals(m_defender);
        remove(alliedTransports, bridge, m_battleSite, defender);
      }
    }
  }

  private void checkForUnitsThatCanRollLeft(final IDelegateBridge bridge, final boolean attacker) {
    // if we are the attacker, we can retreat instead of dying
    if (attacker
        && (!getAttackerRetreatTerritories().isEmpty() || m_attackingUnits.stream().anyMatch(Matches.unitIsAir()))) {
      return;
    }
    if (m_attackingUnits.isEmpty() || m_defendingUnits.isEmpty()) {
      return;
    }
    final Predicate<Unit> notSubmergedAndType = Matches.unitIsSubmerged().negate()
        .and(Matches.territoryIsLand().test(m_battleSite)
            ? Matches.unitIsSea().negate()
            : Matches.unitIsLand().negate());
    final Collection<Unit> unitsToKill;
    final boolean hasUnitsThatCanRollLeft;
    if (attacker) {
      hasUnitsThatCanRollLeft = m_attackingUnits.stream().anyMatch(
          notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(attacker)));
      unitsToKill =
          CollectionUtils.getMatches(m_attackingUnits, notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
    } else {
      hasUnitsThatCanRollLeft = m_defendingUnits.stream().anyMatch(
          notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(attacker)));
      unitsToKill =
          CollectionUtils.getMatches(m_defendingUnits, notSubmergedAndType.and(Matches.unitIsNotInfrastructure()));
    }
    final boolean enemy = !attacker;
    final boolean enemyHasUnitsThatCanRollLeft;
    if (enemy) {
      enemyHasUnitsThatCanRollLeft = m_attackingUnits.stream().anyMatch(
          notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(enemy)));
    } else {
      enemyHasUnitsThatCanRollLeft = m_defendingUnits.stream().anyMatch(
          notSubmergedAndType.and(Matches.unitIsSupporterOrHasCombatAbility(enemy)));
    }
    if (!hasUnitsThatCanRollLeft && enemyHasUnitsThatCanRollLeft) {
      remove(unitsToKill, bridge, m_battleSite, !attacker);
    }
  }

  /**
   * Submerge attacking/defending subs if they're alone OR with transports against only air.
   */
  private void submergeSubsVsOnlyAir(final IDelegateBridge bridge) {
    // if All attackers are AIR, submerge any defending subs
    if (!m_attackingUnits.isEmpty() && m_attackingUnits.stream().allMatch(Matches.unitIsAir())
        && m_defendingUnits.stream().anyMatch(Matches.unitIsSub())) {
      // Get all defending subs (including allies) in the territory
      final List<Unit> defendingSubs = CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsSub());
      // submerge defending subs
      submergeUnits(defendingSubs, true, bridge);
      // checking defending air on attacking subs
    } else if (!m_defendingUnits.isEmpty() && m_defendingUnits.stream().allMatch(Matches.unitIsAir())
        && m_attackingUnits.stream().anyMatch(Matches.unitIsSub())) {
      // Get all attacking subs in the territory
      final List<Unit> attackingSubs = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsSub());
      // submerge attacking subs
      submergeUnits(attackingSubs, false, bridge);
    }
  }

  private void defendNonSubs() {
    if (m_attackingUnits.size() == 0) {
      return;
    }
    Collection<Unit> units = new ArrayList<>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
    units.addAll(m_defendingUnits);
    units.addAll(m_defendingWaitingToDie);
    units = CollectionUtils.getMatches(units, Matches.unitIsNotSub());
    // if restricted, remove aircraft from attackers
    if (isAirAttackSubRestricted() && !canAirAttackSubs(m_attackingUnits, units)) {
      units.removeAll(CollectionUtils.getMatches(units, Matches.unitIsAir()));
    }
    if (units.isEmpty()) {
      return;
    }
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingWaitingToDie);
    fire(m_attacker.getName() + SELECT_CASUALTIES, units, m_attackingUnits, allEnemyUnitsAliveOrWaitingToDie, true,
        ReturnFire.ALL, "Defenders fire, ");
  }

  /**
   * If there are no attacking DDs but defending SUBs, fire AIR at non-SUB forces ONLY.
   */
  private void attackAirOnNonSubs() {
    if (m_defendingUnits.size() == 0) {
      return;
    }
    Collection<Unit> units = new ArrayList<>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
    units.addAll(m_attackingUnits);
    units.addAll(m_attackingWaitingToDie);
    // See if allied air can participate in combat
    if (!isAlliedAirIndependent()) {
      units = CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(m_attacker));
    }
    if (!canAirAttackSubs(m_defendingUnits, units)) {
      units = CollectionUtils.getMatches(units, Matches.unitIsAir());
      final Collection<Unit> enemyUnitsNotSubs = CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsNotSub());
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
      allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingUnits);
      allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingWaitingToDie);
      fire(m_defender.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, allEnemyUnitsAliveOrWaitingToDie, false,
          ReturnFire.ALL, "Attacker's aircraft fire,");
    }
  }

  private boolean canAirAttackSubs(final Collection<Unit> firedAt, final Collection<Unit> firing) {
    return !(m_battleSite.isWater() && firedAt.stream().anyMatch(Matches.unitIsSub())
        && firing.stream().noneMatch(Matches.unitIsDestroyer()));
  }

  private void defendAirOnNonSubs() {
    if (m_attackingUnits.size() == 0) {
      return;
    }
    Collection<Unit> units = new ArrayList<>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
    units.addAll(m_defendingUnits);
    units.addAll(m_defendingWaitingToDie);

    if (!canAirAttackSubs(m_attackingUnits, units)) {
      units = CollectionUtils.getMatches(units, Matches.unitIsAir());
      final Collection<Unit> enemyUnitsNotSubs = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsNotSub());
      if (enemyUnitsNotSubs.isEmpty()) {
        return;
      }
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
      allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingUnits);
      allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingWaitingToDie);
      fire(m_attacker.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, allEnemyUnitsAliveOrWaitingToDie, true,
          ReturnFire.ALL, "Defender's aircraft fire,");
    }
  }

  /**
   * If there are no attacking DDs, but defending SUBs, remove attacking AIR as they've already fired, otherwise fire
   * all attackers.
   */
  private void attackNonSubs() {
    if (m_defendingUnits.size() == 0) {
      return;
    }
    Collection<Unit> units = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsNotSub());
    units.addAll(CollectionUtils.getMatches(m_attackingWaitingToDie, Matches.unitIsNotSub()));
    // See if allied air can participate in combat
    if (!isAlliedAirIndependent()) {
      units = CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(m_attacker));
    }
    // if restricted, remove aircraft from attackers
    if (isAirAttackSubRestricted() && !canAirAttackSubs(m_defendingUnits, units)) {
      units.removeAll(CollectionUtils.getMatches(units, Matches.unitIsAir()));
    }
    if (units.isEmpty()) {
      return;
    }
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingWaitingToDie);
    fire(m_defender.getName() + SELECT_CASUALTIES, units, m_defendingUnits, allEnemyUnitsAliveOrWaitingToDie, false,
        ReturnFire.ALL, "Attackers fire,");
  }

  private void attackSubs(final ReturnFire returnFire) {
    final Collection<Unit> firing = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsSub());
    if (firing.isEmpty()) {
      return;
    }
    final Collection<Unit> attacked = CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsNotAir());
    // if there are destroyers in the attacked units, we can return fire.
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingWaitingToDie);
    fire(m_defender.getName() + SELECT_SUB_CASUALTIES, firing, attacked, allEnemyUnitsAliveOrWaitingToDie, false,
        returnFire, "Subs fire,");
  }

  private void defendSubs(final ReturnFire returnFire) {
    if (m_attackingUnits.size() == 0) {
      return;
    }
    Collection<Unit> firing = new ArrayList<>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
    firing.addAll(m_defendingUnits);
    firing.addAll(m_defendingWaitingToDie);
    firing = CollectionUtils.getMatches(firing, Matches.unitIsSub());
    if (firing.isEmpty()) {
      return;
    }
    final Collection<Unit> attacked = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsNotAir());
    if (attacked.isEmpty()) {
      return;
    }
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingWaitingToDie);
    fire(m_attacker.getName() + SELECT_SUB_CASUALTIES, firing, attacked, allEnemyUnitsAliveOrWaitingToDie, true,
        returnFire, "Subs defend, ");
  }

  void removeSuicideOnHitCasualties(final Collection<Unit> firingUnits, final int hits, final boolean defender,
      final IDelegateBridge bridge) {
    if (firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit()) && (hits > 0)) {
      final List<Unit> units = firingUnits.stream().limit(hits).collect(Collectors.toList());
      getDisplay(bridge).deadUnitNotification(m_battleID, defender ? m_defender : m_attacker, units, m_dependentUnits);
      remove(units, bridge, m_battleSite, defender);
    }
  }

  void removeCasualties(final Collection<Unit> killed, final ReturnFire returnFire, final boolean defender,
      final IDelegateBridge bridge) {
    if (killed.isEmpty()) {
      return;
    }
    if (returnFire == ReturnFire.ALL) {
      // move to waiting to die
      if (defender) {
        m_defendingWaitingToDie.addAll(killed);
      } else {
        m_attackingWaitingToDie.addAll(killed);
      }
    } else if (returnFire == ReturnFire.SUBS) {
      // move to waiting to die
      if (defender) {
        m_defendingWaitingToDie.addAll(CollectionUtils.getMatches(killed, Matches.unitIsSub()));
      } else {
        m_attackingWaitingToDie.addAll(CollectionUtils.getMatches(killed, Matches.unitIsSub()));
      }
      remove(CollectionUtils.getMatches(killed, Matches.unitIsNotSub()), bridge, m_battleSite, defender);
    } else if (returnFire == ReturnFire.NONE) {
      remove(killed, bridge, m_battleSite, defender);
    }
    // remove from the active fighting
    if (defender) {
      m_defendingUnits.removeAll(killed);
    } else {
      m_attackingUnits.removeAll(killed);
    }
  }

  private void fireNavalBombardment(final IDelegateBridge bridge) {
    // TODO - check within the method for the bombarding limitations
    final Collection<Unit> bombard = getBombardingUnits();
    final Collection<Unit> attacked = CollectionUtils.getMatches(m_defendingUnits,
        Matches.unitIsNotInfrastructureAndNotCapturedOnEntering(m_attacker, m_battleSite, m_data));
    // bombarding units can't move after bombarding
    if (!m_headless) {
      final Change change = ChangeFactory.markNoMovementChange(bombard);
      bridge.addChange(change);
    }
    /**
     * TODO This code is actually a bug- the property is intended to tell if the return fire is
     * RESTRICTED- but it's used as if it's ALLOWED. The reason is the default values on the
     * property definition. However, fixing this will entail a fix to the XML to reverse
     * all values. We'll leave it as is for now and try to figure out a patch strategy later.
     */
    final boolean canReturnFire = isNavalBombardCasualtiesReturnFire();
    if ((bombard.size() > 0) && (attacked.size() > 0)) {
      if (!m_headless) {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_BOMBARD, m_attacker);
      }
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
      allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingUnits);
      allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingWaitingToDie);
      fire(SELECT_NAVAL_BOMBARDMENT_CASUALTIES, bombard, attacked, allEnemyUnitsAliveOrWaitingToDie, false,
          canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, "Bombard");
    }
  }

  private void fireSuicideUnitsAttack() {
    final Predicate<Unit> attackableUnits =
        Matches.unitIsNotInfrastructureAndNotCapturedOnEntering(m_attacker, m_battleSite, m_data)
            .and(Matches.unitIsSuicide().negate())
            .and(Matches.unitIsBeingTransported().negate());
    final Collection<Unit> suicideAttackers = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsSuicide());
    final Collection<Unit> attackedDefenders = CollectionUtils.getMatches(m_defendingUnits, attackableUnits);
    // comparatively simple rules for isSuicide units. if AirAttackSubRestricted and you have no destroyers, you can't
    // attack subs with anything.
    if (isAirAttackSubRestricted() && !m_attackingUnits.stream().anyMatch(Matches.unitIsDestroyer())
        && attackedDefenders.stream().anyMatch(Matches.unitIsSub())) {
      attackedDefenders.removeAll(CollectionUtils.getMatches(attackedDefenders, Matches.unitIsSub()));
    }
    if (!suicideAttackers.isEmpty() && suicideAttackers.stream().allMatch(Matches.unitIsSub())) {
      attackedDefenders.removeAll(CollectionUtils.getMatches(attackedDefenders, Matches.unitIsAir()));
    }
    if ((suicideAttackers.size() == 0) || (attackedDefenders.size() == 0)) {
      return;
    }
    final boolean canReturnFire = (!isSuicideAndMunitionCasualtiesRestricted());
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_defendingWaitingToDie);
    fire(m_defender.getName() + SELECT_CASUALTIES_SUICIDE, suicideAttackers, attackedDefenders,
        allEnemyUnitsAliveOrWaitingToDie, false, canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, SUICIDE_ATTACK);
  }

  private void fireSuicideUnitsDefend() {
    if (isDefendingSuicideAndMunitionUnitsDoNotFire()) {
      return;
    }
    final Predicate<Unit> attackableUnits = Matches.unitIsNotInfrastructure()
        .and(Matches.unitIsSuicide().negate())
        .and(Matches.unitIsBeingTransported().negate());
    final Collection<Unit> suicideDefenders = CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsSuicide());
    final Collection<Unit> attackedAttackers = CollectionUtils.getMatches(m_attackingUnits, attackableUnits);
    // comparatively simple rules for isSuicide units. if AirAttackSubRestricted and you have no destroyers, you can't
    // attack subs with anything.
    if (isAirAttackSubRestricted() && !m_defendingUnits.stream().anyMatch(Matches.unitIsDestroyer())
        && attackedAttackers.stream().anyMatch(Matches.unitIsSub())) {
      attackedAttackers.removeAll(CollectionUtils.getMatches(attackedAttackers, Matches.unitIsSub()));
    }
    if (!suicideDefenders.isEmpty() && suicideDefenders.stream().allMatch(Matches.unitIsSub())) {
      suicideDefenders.removeAll(CollectionUtils.getMatches(suicideDefenders, Matches.unitIsAir()));
    }
    if ((suicideDefenders.size() == 0) || (attackedAttackers.size() == 0)) {
      return;
    }
    final boolean canReturnFire = (!isSuicideAndMunitionCasualtiesRestricted());
    final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingUnits);
    allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingWaitingToDie);
    fire(m_attacker.getName() + SELECT_CASUALTIES_SUICIDE, suicideDefenders, attackedAttackers,
        allEnemyUnitsAliveOrWaitingToDie, true, canReturnFire ? ReturnFire.ALL : ReturnFire.NONE,
        SUICIDE_DEFEND);
  }

  private boolean isWW2V2() {
    return Properties.getWW2V2(m_data);
  }

  private boolean isWW2V3() {
    return Properties.getWW2V3(m_data);
  }

  private boolean isPartialAmphibiousRetreat() {
    return Properties.getPartialAmphibiousRetreat(m_data);
  }

  private boolean isAlliedAirIndependent() {
    return Properties.getAlliedAirIndependent(m_data);
  }

  private boolean isDefendingSubsSneakAttack() {
    return Properties.getDefendingSubsSneakAttack(m_data);
  }

  private boolean isAttackerRetreatPlanes() {
    return Properties.getAttackerRetreatPlanes(m_data);
  }

  private boolean isNavalBombardCasualtiesReturnFire() {
    return Properties.getNavalBombardCasualtiesReturnFireRestricted(m_data);
  }

  private boolean isSuicideAndMunitionCasualtiesRestricted() {
    return Properties.getSuicideAndMunitionCasualtiesRestricted(m_data);
  }

  private boolean isDefendingSuicideAndMunitionUnitsDoNotFire() {
    return Properties.getDefendingSuicideAndMunitionUnitsDoNotFire(m_data);
  }

  private boolean isAirAttackSubRestricted() {
    return Properties.getAirAttackSubRestricted(m_data);
  }

  private boolean isSubRetreatBeforeBattle() {
    return Properties.getSubRetreatBeforeBattle(m_data);
  }

  private boolean isTransportCasualtiesRestricted() {
    return Properties.getTransportCasualtiesRestricted(m_data);
  }

  private void fireOffensiveAaGuns() {
    m_stack.push(new FireAA(false));
  }

  private void fireDefensiveAaGuns() {
    m_stack.push(new FireAA(true));
  }

  class FireAA implements IExecutable {
    private static final long serialVersionUID = -6406659798754841382L;
    private final boolean m_defending;
    private DiceRoll m_dice;
    private CasualtyDetails m_casualties;
    Collection<Unit> m_casualtiesSoFar = new ArrayList<>();

    private FireAA(final boolean defending) {
      m_defending = defending;
    }

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      if ((m_defending && !canFireDefendingAa()) || (!m_defending && !canFireOffensiveAa())) {
        return;
      }
      for (final String currentTypeAa : (m_defending ? m_defendingAAtypes : m_offensiveAAtypes)) {
        final Collection<Unit> currentAaUnits = CollectionUtils
            .getMatches((m_defending ? m_defendingAA : m_offensiveAA), Matches.unitIsAaOfTypeAa(currentTypeAa));
        final List<Collection<Unit>> firingGroups = createFiringUnitGroups(currentAaUnits);
        for (final Collection<Unit> currentPossibleAa : firingGroups) {
          final Set<UnitType> targetUnitTypesForThisTypeAa =
              UnitAttachment.get(currentPossibleAa.iterator().next().getType()).getTargetsAA(m_data);
          final Set<UnitType> airborneTypesTargettedToo =
              m_defending ? TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data).get(currentTypeAa)
                  : new HashSet<>();
          final Collection<Unit> validAttackingUnitsForThisRoll = CollectionUtils.getMatches(
              (m_defending ? m_attackingUnits : m_defendingUnits), Matches.unitIsOfTypes(targetUnitTypesForThisTypeAa)
                  .or(Matches.unitIsAirborne().and(Matches.unitIsOfTypes(airborneTypesTargettedToo))));
          final IExecutable rollDice = new IExecutable() {
            private static final long serialVersionUID = 6435935558879109347L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              validAttackingUnitsForThisRoll.removeAll(m_casualtiesSoFar);
              if (!validAttackingUnitsForThisRoll.isEmpty()) {
                m_dice =
                    DiceRoll.rollAa(validAttackingUnitsForThisRoll, currentPossibleAa, bridge, m_battleSite,
                        m_defending);
                if (!m_headless) {
                  if (currentTypeAa.equals("AA")) {
                    if (m_dice.getHits() > 0) {
                      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_HIT,
                          (m_defending ? m_defender : m_attacker));
                    } else {
                      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_MISS,
                          (m_defending ? m_defender : m_attacker));
                    }
                  } else {
                    if (m_dice.getHits() > 0) {
                      bridge.getSoundChannelBroadcaster().playSoundForAll(
                          SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAa.toLowerCase() + SoundPath.CLIP_BATTLE_X_HIT,
                          (m_defending ? m_defender : m_attacker));
                    } else {
                      bridge.getSoundChannelBroadcaster().playSoundForAll(
                          SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAa.toLowerCase() + SoundPath.CLIP_BATTLE_X_MISS,
                          (m_defending ? m_defender : m_attacker));
                    }
                  }
                }
              }
            }
          };
          final IExecutable selectCasualties = new IExecutable() {
            private static final long serialVersionUID = 7943295620796835166L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              if (!validAttackingUnitsForThisRoll.isEmpty()) {
                final CasualtyDetails details =
                    selectCasualties(validAttackingUnitsForThisRoll, currentPossibleAa, bridge, currentTypeAa);
                markDamaged(details.getDamaged(), bridge);
                m_casualties = details;
                m_casualtiesSoFar.addAll(details.getKilled());
              }
            }
          };
          final IExecutable notifyCasualties = new IExecutable() {
            private static final long serialVersionUID = -6759782085212899725L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              if (!validAttackingUnitsForThisRoll.isEmpty()) {
                notifyCasualtiesAa(bridge, currentTypeAa);
                removeCasualties(m_casualties.getKilled(), ReturnFire.ALL, !m_defending, bridge);
                removeSuicideOnHitCasualties(currentPossibleAa, m_dice.getHits(), m_defending, bridge);
              }
            }
          };
          // push in reverse order of execution
          stack.push(notifyCasualties);
          stack.push(selectCasualties);
          stack.push(rollDice);
        }
      }
    }

    private CasualtyDetails selectCasualties(final Collection<Unit> validAttackingUnitsForThisRoll,
        final Collection<Unit> defendingAa, final IDelegateBridge bridge, final String currentTypeAa) {
      // send defender the dice roll so he can see what the dice are while he waits for attacker to select casualties
      getDisplay(bridge).notifyDice(m_dice, (m_defending ? m_attacker.getName() : m_defender.getName())
          + SELECT_PREFIX + currentTypeAa + CASUALTIES_SUFFIX);
      return BattleCalculator.getAaCasualties(!m_defending, validAttackingUnitsForThisRoll,
          (m_defending ? m_attackingUnits : m_defendingUnits), defendingAa,
          (m_defending ? m_defendingUnits : m_attackingUnits), m_dice, bridge, (m_defending ? m_defender : m_attacker),
          (m_defending ? m_attacker : m_defender), m_battleID, m_battleSite, m_territoryEffects, m_isAmphibious,
          m_amphibiousLandAttackers);
    }

    private void notifyCasualtiesAa(final IDelegateBridge bridge, final String currentTypeAa) {
      if (m_headless) {
        return;
      }
      getDisplay(bridge).casualtyNotification(m_battleID,
          (m_defending ? m_attacker.getName() : m_defender.getName()) + REMOVE_PREFIX + currentTypeAa
              + CASUALTIES_SUFFIX,
          m_dice, (m_defending ? m_attacker : m_defender), new ArrayList<>(m_casualties.getKilled()),
          new ArrayList<>(m_casualties.getDamaged()), m_dependentUnits);
      getRemote((m_defending ? m_attacker : m_defender), bridge).confirmOwnCasualties(m_battleID,
          "Press space to continue");
      final Thread t = new Thread(() -> {
        try {
          getRemote((m_defending ? m_defender : m_attacker), bridge).confirmEnemyCasualties(m_battleID,
              "Press space to continue", (m_defending ? m_attacker : m_defender));
        } catch (final Exception e) {
          // ignore
        }
      }, "click to continue waiter");
      t.start();
      bridge.leaveDelegateExecution();
      Interruptibles.join(t);
      bridge.enterDelegateExecution();
    }
  }

  private boolean canFireDefendingAa() {
    if (m_defendingAA == null) {
      updateDefendingAaUnits();
    }
    return m_defendingAA.size() > 0;
  }

  private boolean canFireOffensiveAa() {
    if (m_offensiveAA == null) {
      updateOffensiveAaUnits();
    }
    return m_offensiveAA.size() > 0;
  }

  /**
   * @return a collection containing all the combatants in units non
   *         combatants include such things as factories, aaguns, land units
   *         in a water battle.
   */
  private List<Unit> removeNonCombatants(final Collection<Unit> units, final boolean attacking,
      final boolean doNotIncludeAa, final boolean doNotIncludeSeaBombardmentUnits, final boolean removeForNextRound) {
    final List<Unit> unitList = new ArrayList<>(units);
    if (m_battleSite.isWater()) {
      unitList.removeAll(CollectionUtils.getMatches(unitList, Matches.unitIsLand()));
    }
    // still allow infrastructure type units that can provide support have combat abilities
    // remove infrastructure units that can't take part in combat (air/naval bases, etc...)
    unitList.removeAll(CollectionUtils.getMatches(unitList,
        Matches.unitCanBeInBattle(attacking, !m_battleSite.isWater(),
            (removeForNextRound ? (m_round + 1) : m_round), true, doNotIncludeAa, doNotIncludeSeaBombardmentUnits)
            .negate()));
    // remove any disabled units from combat
    unitList.removeAll(CollectionUtils.getMatches(unitList, Matches.unitIsDisabled()));
    // remove capturableOnEntering units (veqryn)
    unitList.removeAll(CollectionUtils.getMatches(unitList,
        Matches.unitCanBeCapturedOnEnteringToInThisTerritory(m_attacker, m_battleSite, m_data)));
    // remove any allied air units that are stuck on damaged carriers (veqryn)
    unitList.removeAll(CollectionUtils.getMatches(unitList, Matches.unitIsBeingTransported()
        .and(Matches.unitIsAir())
        .and(Matches.unitCanLandOnCarrier())));
    // remove any units that were in air combat (veqryn)
    unitList.removeAll(CollectionUtils.getMatches(unitList, Matches.unitWasInAirBattle()));
    return unitList;
  }

  private void removeNonCombatants(final IDelegateBridge bridge, final boolean doNotIncludeAa,
      final boolean doNotIncludeSeaBombardmentUnits, final boolean removeForNextRound) {
    final List<Unit> notRemovedDefending = removeNonCombatants(m_defendingUnits, false, doNotIncludeAa,
        doNotIncludeSeaBombardmentUnits, removeForNextRound);
    final List<Unit> notRemovedAttacking = removeNonCombatants(m_attackingUnits, true, doNotIncludeAa,
        doNotIncludeSeaBombardmentUnits, removeForNextRound);
    final Collection<Unit> toRemoveDefending = CollectionUtils.difference(m_defendingUnits, notRemovedDefending);
    final Collection<Unit> toRemoveAttacking = CollectionUtils.difference(m_attackingUnits, notRemovedAttacking);
    m_defendingUnits = notRemovedDefending;
    m_attackingUnits = notRemovedAttacking;
    if (!m_headless) {
      if (!toRemoveDefending.isEmpty()) {
        getDisplay(bridge).changedUnitsNotification(m_battleID, m_defender, toRemoveDefending, null, null);
      }
      if (!toRemoveAttacking.isEmpty()) {
        getDisplay(bridge).changedUnitsNotification(m_battleID, m_attacker, toRemoveAttacking, null, null);
      }
    }
  }

  private void landParatroops(final IDelegateBridge bridge) {
    if (TechAttachment.isAirTransportable(m_attacker)) {
      final Collection<Unit> airTransports =
          CollectionUtils.getMatches(m_battleSite.getUnits().getUnits(), Matches.unitIsAirTransport());
      if (!airTransports.isEmpty()) {
        final Collection<Unit> dependents = getDependentUnits(airTransports);
        if (!dependents.isEmpty()) {
          final CompositeChange change = new CompositeChange();
          // remove dependency from paratroopers by unloading the air transports
          for (final Unit unit : dependents) {
            change.add(TransportTracker.unloadAirTransportChange((TripleAUnit) unit, m_battleSite, false));
          }
          bridge.addChange(change);
          // remove bombers from m_dependentUnits
          for (final Unit unit : airTransports) {
            m_dependentUnits.remove(unit);
          }
        }
      }
    }
  }

  private void markNoMovementLeft(final IDelegateBridge bridge) {
    if (m_headless) {
      return;
    }
    final Collection<Unit> attackingNonAir = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsAir().negate());
    final Change noMovementChange = ChangeFactory.markNoMovementChange(attackingNonAir);
    if (!noMovementChange.isEmpty()) {
      bridge.addChange(noMovementChange);
    }
  }

  /**
   * Figure out what units a transport is transporting and has to unloaded.
   */
  private Collection<Unit> getTransportDependents(final Collection<Unit> targets) {
    if (m_headless) {
      return Collections.emptyList();
    } else if (targets.stream().noneMatch(Matches.unitCanTransport())) {
      return new ArrayList<>();
    }
    return targets.stream()
        .map(TransportTracker::transportingAndUnloaded)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private void remove(final Collection<Unit> killed, final IDelegateBridge bridge, final Territory battleSite,
      final Boolean defenderDying) {
    if (killed.size() == 0) {
      return;
    }
    final Collection<Unit> dependent = getDependentUnits(killed);
    killed.addAll(dependent);
    final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
    m_killed.addAll(killed);
    final String transcriptText = MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(killed));
    bridge.addChange(killedChange);
    final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(this);
    // If there are NO dependent battles, check for unloads in allied territories
    if (dependentBattles.isEmpty()) {
      removeFromNonCombatLandings(killed, bridge);
      // otherwise remove them and the units involved
    } else {
      removeFromDependents(killed, bridge, dependentBattles);
    }
    // and remove them from the battle display
    if ((defenderDying == null) || defenderDying) {
      m_defendingUnits.removeAll(killed);
    }
    if ((defenderDying == null) || !defenderDying) {
      m_attackingUnits.removeAll(killed);
    }
  }

  private void removeFromDependents(final Collection<Unit> units, final IDelegateBridge bridge,
      final Collection<IBattle> dependents) {
    for (final IBattle dependent : dependents) {
      dependent.unitsLostInPrecedingBattle(this, units, bridge, false);
    }
  }

  // Remove landed units from allied territory when their transport sinks
  private void removeFromNonCombatLandings(final Collection<Unit> units, final IDelegateBridge bridge) {
    for (final Unit transport : CollectionUtils.getMatches(units, Matches.unitIsTransport())) {
      final Collection<Unit> lost = getTransportDependents(Collections.singleton(transport));
      if (lost.isEmpty()) {
        continue;
      }
      final Territory landedTerritory = TransportTracker.getTerritoryTransportHasUnloadedTo(transport);
      if (landedTerritory == null) {
        throw new IllegalStateException("not unloaded?:" + units);
      }
      remove(lost, bridge, landedTerritory, false);
    }
  }

  private void clearWaitingToDieAndDamagedChangesInto(final IDelegateBridge bridge) {
    final Collection<Unit> unitsToRemove = new ArrayList<>();
    unitsToRemove.addAll(m_attackingWaitingToDie);
    unitsToRemove.addAll(m_defendingWaitingToDie);
    remove(unitsToRemove, bridge, m_battleSite, null);
    m_defendingWaitingToDie.clear();
    m_attackingWaitingToDie.clear();
    damagedChangeInto(m_attackingUnits, bridge);
    damagedChangeInto(m_defendingUnits, bridge);
  }

  private void damagedChangeInto(final List<Unit> units, final IDelegateBridge bridge) {
    final List<Unit> damagedUnits = CollectionUtils.getMatches(units,
        Matches.unitWhenHitPointsDamagedChangesInto().and(Matches.unitHasTakenSomeDamage()));
    final CompositeChange changes = new CompositeChange();
    final List<Unit> unitsToRemove = new ArrayList<>();
    final List<Unit> unitsToAdd = new ArrayList<>();
    for (final Unit unit : damagedUnits) {
      final Map<Integer, Tuple<Boolean, UnitType>> map =
          UnitAttachment.get(unit.getType()).getWhenHitPointsDamagedChangesInto();
      if (map.containsKey(unit.getHits())) {
        final boolean translateAttributes = map.get(unit.getHits()).getFirst();
        final UnitType unitType = map.get(unit.getHits()).getSecond();
        final List<Unit> toAdd = unitType.create(1, unit.getOwner());
        if (translateAttributes) {
          final Change translate = TripleAUnit.translateAttributesToOtherUnits(unit, toAdd, m_battleSite);
          changes.add(translate);
        }
        unitsToRemove.add(unit);
        unitsToAdd.addAll(toAdd);
      }
    }
    if (!unitsToRemove.isEmpty()) {
      bridge.addChange(changes);
      remove(unitsToRemove, bridge, m_battleSite, null);
      final String transcriptText = MyFormatter.unitsToText(unitsToAdd) + " added in " + m_battleSite.getName();
      bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(unitsToAdd));
      bridge.addChange(ChangeFactory.addUnits(m_battleSite, unitsToAdd));
      bridge.addChange(ChangeFactory.markNoMovementChange(unitsToAdd));
      units.addAll(unitsToAdd);
      getDisplay(bridge).changedUnitsNotification(m_battleID, unitsToRemove.get(0).getOwner(), unitsToRemove,
          unitsToAdd, null);
    }
  }

  private void defenderWins(final IDelegateBridge bridge) {
    m_whoWon = WhoWon.DEFENDER;
    getDisplay(bridge).battleEnd(m_battleID, m_defender.getName() + " win");
    if (Properties.getAbandonedTerritoriesMayBeTakenOverImmediately(m_data)) {
      if (CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsNotInfrastructure()).size() == 0) {
        final List<Unit> allyOfAttackerUnits = m_battleSite.getUnits().getMatches(Matches.unitIsNotInfrastructure());
        if (!allyOfAttackerUnits.isEmpty()) {
          final PlayerID abandonedToPlayer = AbstractBattle.findPlayerWithMostUnits(allyOfAttackerUnits);
          bridge.getHistoryWriter().addChildToEvent(
              abandonedToPlayer.getName() + " takes over " + m_battleSite.getName() + " as there are no defenders left",
              allyOfAttackerUnits);
          // should we create a new battle records to show the ally capturing the territory (in the case where they
          // didn't already own/allied it)?
          m_battleTracker.takeOver(m_battleSite, abandonedToPlayer, bridge, null, allyOfAttackerUnits);
        }
      } else {
        // should we create a new battle records to show the defender capturing the territory (in the case where they
        // didn't already own/allied it)?
        m_battleTracker.takeOver(m_battleSite, m_defender, bridge, null, m_defendingUnits);
      }
    }
    bridge.getHistoryWriter().addChildToEvent(m_defender.getName() + " win", new ArrayList<>(m_defendingUnits));
    m_battleResultDescription = BattleRecord.BattleResultDescription.LOST;
    showCasualties(bridge);
    if (!m_headless) {
      m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV,
          m_defenderLostTUV, m_battleResultDescription, new BattleResults(this, m_data));
    }
    checkDefendingPlanesCanLand();
    BattleTracker.captureOrDestroyUnits(m_battleSite, m_defender, m_defender, bridge, null);
    if (!m_headless) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, m_attacker);
    }
  }

  private void nobodyWins(final IDelegateBridge bridge) {
    m_whoWon = WhoWon.DRAW;
    getDisplay(bridge).battleEnd(m_battleID, "Stalemate");
    bridge.getHistoryWriter()
        .addChildToEvent(m_defender.getName() + " and " + m_attacker.getName() + " reach a stalemate");
    m_battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
    showCasualties(bridge);
    if (!m_headless) {
      m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV,
          m_defenderLostTUV, m_battleResultDescription, new BattleResults(this, m_data));
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_STALEMATE, m_attacker);
    }
    checkDefendingPlanesCanLand();
  }

  private void attackerWins(final IDelegateBridge bridge) {
    m_whoWon = WhoWon.ATTACKER;
    getDisplay(bridge).battleEnd(m_battleID, m_attacker.getName() + " win");
    if (m_headless) {
      return;
    }
    // do we need to change ownership
    if (m_attackingUnits.stream().anyMatch(Matches.unitIsNotAir())) {
      if (Matches.isTerritoryEnemyAndNotUnownedWater(m_attacker, m_data).test(m_battleSite)) {
        m_battleTracker.addToConquered(m_battleSite);
      }
      m_battleTracker.takeOver(m_battleSite, m_attacker, bridge, null, m_attackingUnits);
      m_battleResultDescription = BattleRecord.BattleResultDescription.CONQUERED;
    } else {
      m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
    }
    // Clear the transported_by for successfully off loaded units
    final Collection<Unit> transports = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsTransport());
    if (!transports.isEmpty()) {
      final CompositeChange change = new CompositeChange();
      final Collection<Unit> dependents = getTransportDependents(transports);
      if (!dependents.isEmpty()) {
        for (final Unit unit : dependents) {
          // clear the loaded by ONLY for Combat unloads. NonCombat unloads are handled elsewhere.
          if (Matches.unitWasUnloadedThisTurn().test(unit)) {
            change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
          }
        }
        bridge.addChange(change);
      }
    }
    bridge.getHistoryWriter().addChildToEvent(m_attacker.getName() + " win", new ArrayList<>(m_attackingUnits));
    showCasualties(bridge);
    if (!m_headless) {
      m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV,
          m_defenderLostTUV, m_battleResultDescription, new BattleResults(this, m_data));
    }
    if (!m_headless) {
      if (Matches.territoryIsWater().test(m_battleSite)) {
        if (!m_attackingUnits.isEmpty() && m_attackingUnits.stream().allMatch(Matches.unitIsAir())) {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL,
              m_attacker);
        } else {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_SEA_SUCCESSFUL,
              m_attacker);
        }
      } else {
        // no sounds for a successful land battle, because land battle means we are going to capture a territory, and we
        // have capture sounds for that
        if (!m_attackingUnits.isEmpty() && m_attackingUnits.stream().allMatch(Matches.unitIsAir())) {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL,
              m_attacker);
        }
      }
    }
  }

  /**
   * The defender has won, but there may be defending fighters that can't stay
   * in the sea zone due to insufficient carriers.
   */
  private void checkDefendingPlanesCanLand() {
    if (m_headless) {
      return;
    }
    // not water, not relevant.
    if (!m_battleSite.isWater()) {
      return;
    }
    // TODO: why do we keep checking throughout this entire class if the units in m_defendingUnits are allied with
    // defender, and if the units in m_attackingUnits are allied with the attacker? Does it really matter?
    final Predicate<Unit> alliedDefendingAir = Matches.unitIsAir().and(Matches.unitWasScrambled().negate());
    final Collection<Unit> defendingAir = CollectionUtils.getMatches(m_defendingUnits, alliedDefendingAir);
    if (defendingAir.isEmpty()) {
      return;
    }
    int carrierCost = AirMovementValidator.carrierCost(defendingAir);
    final int carrierCapacity = AirMovementValidator.carrierCapacity(m_defendingUnits, m_battleSite);
    // add dependent air to carrier cost
    carrierCost += AirMovementValidator
        .carrierCost(CollectionUtils.getMatches(getDependentUnits(m_defendingUnits), alliedDefendingAir));
    // all planes can land, exit
    if (carrierCapacity >= carrierCost) {
      return;
    }
    // find out what we must remove by removing all the air that can land on carriers from defendingAir
    carrierCost = 0;
    carrierCost += AirMovementValidator
        .carrierCost(CollectionUtils.getMatches(getDependentUnits(m_defendingUnits), alliedDefendingAir));
    for (final Unit currentUnit : new ArrayList<>(defendingAir)) {
      if (!Matches.unitCanLandOnCarrier().test(currentUnit)) {
        defendingAir.remove(currentUnit);
        continue;
      }
      carrierCost += UnitAttachment.get(currentUnit.getType()).getCarrierCost();
      if (carrierCapacity >= carrierCost) {
        defendingAir.remove(currentUnit);
      }
    }
    // Moved this choosing to after all battles, as we legally should be able to land in a territory if we win there.
    m_battleTracker.addToDefendingAirThatCanNotLand(defendingAir, m_battleSite);
  }

  static CompositeChange clearTransportedByForAlliedAirOnCarrier(final Collection<Unit> attackingUnits,
      final Territory battleSite, final PlayerID attacker, final GameData data) {
    final CompositeChange change = new CompositeChange();
    // Clear the transported_by for successfully won battles where there was an allied air unit held as cargo by an
    // carrier unit
    final Collection<Unit> carriers = CollectionUtils.getMatches(attackingUnits, Matches.unitIsCarrier());
    if (!carriers.isEmpty() && !Properties.getAlliedAirIndependent(data)) {
      final Predicate<Unit> alliedFighters = Matches.isUnitAllied(attacker, data)
          .and(Matches.unitIsOwnedBy(attacker).negate())
          .and(Matches.unitIsAir())
          .and(Matches.unitCanLandOnCarrier());
      final Collection<Unit> alliedAirInTerr = CollectionUtils.getMatches(
          Sets.union(Sets.newHashSet(attackingUnits), Sets.newHashSet(battleSite.getUnits())),
          alliedFighters);
      for (final Unit fighter : alliedAirInTerr) {
        final TripleAUnit taUnit = (TripleAUnit) fighter;
        if (taUnit.getTransportedBy() != null) {
          final Unit carrierTransportingThisUnit = taUnit.getTransportedBy();
          if (!Matches.unitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER)
              .test(carrierTransportingThisUnit)) {
            change.add(ChangeFactory.unitPropertyChange(fighter, null, TripleAUnit.TRANSPORTED_BY));
          }
        }
      }
    }
    return change;
  }

  private void showCasualties(final IDelegateBridge bridge) {
    if (m_killed.isEmpty()) {
      return;
    }
    // a handy summary of all the units killed
    IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(m_attacker, m_data);
    final int tuvLostAttacker = TuvUtils.getTuv(m_killed, m_attacker, costs, m_data);
    costs = TuvUtils.getCostsForTuv(m_defender, m_data);
    final int tuvLostDefender = TuvUtils.getTuv(m_killed, m_defender, costs, m_data);
    final int tuvChange = tuvLostDefender - tuvLostAttacker;
    bridge.getHistoryWriter().addChildToEvent(
        "Battle casualty summary: Battle score (TUV change) for attacker is " + tuvChange,
        new ArrayList<>(m_killed));
    m_attackerLostTUV += tuvLostAttacker;
    m_defenderLostTUV += tuvLostDefender;
  }

  private void endBattle(final IDelegateBridge bridge) {
    clearWaitingToDieAndDamagedChangesInto(bridge);
    m_isOver = true;
    m_battleTracker.removeBattle(this);

    // Must clear transportedby for allied air on carriers for both attacking units and retreating units
    final CompositeChange clearAlliedAir =
        clearTransportedByForAlliedAirOnCarrier(m_attackingUnits, m_battleSite, m_attacker, m_data);
    if (!clearAlliedAir.isEmpty()) {
      bridge.addChange(clearAlliedAir);
    }
    final CompositeChange clearAlliedAirRetreated =
        clearTransportedByForAlliedAirOnCarrier(m_attackingUnitsRetreated, m_battleSite, m_attacker, m_data);
    if (!clearAlliedAirRetreated.isEmpty()) {
      bridge.addChange(clearAlliedAirRetreated);
    }
  }

  @Override
  public void cancelBattle(final IDelegateBridge bridge) {
    endBattle(bridge);
  }

  @Override
  public String toString() {
    return "Battle in:" + m_battleSite + " battle type:" + m_battleType + " defender:" + m_defender.getName()
        + " attacked by:" + m_attacker.getName() + " from:" + m_attackingFrom + " attacking with: " + m_attackingUnits;
  }

  /**
   * In an amphibious assault, sort on who is unloading from transports first as this will allow the marines with higher
   * scores to get killed last.
   */
  private void sortAmphib(final List<Unit> units) {
    final Comparator<Unit> decreasingMovement = UnitComparator.getLowestToHighestMovementComparator();
    Collections.sort(units,
        Comparator.comparing(Unit::getType, Comparator.comparing(UnitType::getName))
            .thenComparing((u1, u2) -> {
              final UnitAttachment ua = UnitAttachment.get(u1.getType());
              final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
              if ((ua.getIsMarine() != 0) && (ua2.getIsMarine() != 0)) {
                return compareAccordingToAmphibious(u1, u2);
              }
              return 0;
            })
            .thenComparing(decreasingMovement));
  }

  private int compareAccordingToAmphibious(final Unit u1, final Unit u2) {
    if (m_amphibiousLandAttackers.contains(u1) && !m_amphibiousLandAttackers.contains(u2)) {
      return -1;
    } else if (m_amphibiousLandAttackers.contains(u2) && !m_amphibiousLandAttackers.contains(u1)) {
      return 1;
    }
    final int m1 = UnitAttachment.get(u1.getType()).getIsMarine();
    final int m2 = UnitAttachment.get(u2.getType()).getIsMarine();
    return m2 - m1;
  }

  // used for setting stuff when we make a scrambling battle when there was no previous battle there, and we need
  // retreat spaces
  public void setAttackingFromAndMap(final Map<Territory, Collection<Unit>> attackingFromMap) {
    m_attackingFromMap = attackingFromMap;
    m_attackingFrom = new HashSet<>(attackingFromMap.keySet());
  }

  @Override
  public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units,
      final IDelegateBridge bridge, final boolean withdrawn) {
    Collection<Unit> lost = getDependentUnits(units);
    lost.addAll(CollectionUtils.intersection(units, m_attackingUnits));
    // if all the amphibious attacking land units are lost, then we are no longer a naval invasion
    m_amphibiousLandAttackers.removeAll(lost);
    if (m_amphibiousLandAttackers.isEmpty()) {
      m_isAmphibious = false;
      m_bombardingUnits.clear();
    }
    m_attackingUnits.removeAll(lost);
    // now that they are definitely removed from our attacking list, make sure that they were not already removed from
    // the territory by the previous battle's remove method
    lost = CollectionUtils.getMatches(lost, Matches.unitIsInTerritory(m_battleSite));
    if (!withdrawn) {
      remove(lost, bridge, m_battleSite, false);
    }
    if (m_attackingUnits.isEmpty()) {
      final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(m_attacker, m_data);
      final int tuvLostAttacker = (withdrawn ? 0 : TuvUtils.getTuv(lost, m_attacker, costs, m_data));
      m_attackerLostTUV += tuvLostAttacker;
      m_whoWon = WhoWon.DEFENDER;
      if (!m_headless) {
        m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender,
            m_attackerLostTUV, m_defenderLostTUV, BattleRecord.BattleResultDescription.LOST,
            new BattleResults(this, m_data));
      }
      m_battleTracker.removeBattle(this);
    }
  }

  @Override
  public Collection<Territory> getAttackingFrom() {
    return m_attackingFrom;
  }

  @Override
  public Map<Territory, Collection<Unit>> getAttackingFromMap() {
    return m_attackingFromMap;
  }

  @Override
  public Collection<Territory> getAmphibiousAttackTerritories() {
    return m_amphibiousAttackFrom;
  }

}
