package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.util.CollectionUtils;

/**
 * Battle in which no fighting occurs.
 * Example is a naval invasion into an empty country,
 * but the battle cannot be fought until a naval battle
 * occurs.
 */
public class NonFightingBattle extends DependentBattle {
  private static final long serialVersionUID = -1699534010648145123L;
  private final Set<Territory> m_attackingFrom;
  private final Collection<Territory> m_amphibiousAttackFrom;
  private final Map<Territory, Collection<Unit>> m_attackingFromMap;

  /**
   * Constructor. Suppress checkstyle warning.
   */
  public NonFightingBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker,
      final GameData data) {
    super(battleSite, attacker, battleTracker, data);
    m_attackingFromMap = new HashMap<>();
    m_attackingFrom = new HashSet<>();
    m_amphibiousAttackFrom = new ArrayList<>();
  }

  @Override
  public Change addAttackChange(final Route route, final Collection<Unit> units,
      final HashMap<Unit, HashSet<Unit>> targets) {
    final Map<Unit, Collection<Unit>> addedTransporting = TransportTracker.transporting(units);
    for (final Unit unit : addedTransporting.keySet()) {
      if (m_dependentUnits.get(unit) != null) {
        m_dependentUnits.get(unit).addAll(addedTransporting.get(unit));
      } else {
        m_dependentUnits.put(unit, addedTransporting.get(unit));
      }
    }
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    m_attackingFrom.add(attackingFrom);
    m_attackingUnits.addAll(units);
    m_attackingFromMap.putIfAbsent(attackingFrom, new ArrayList<>());
    final Collection<Unit> attackingFromMapUnits = m_attackingFromMap.get(attackingFrom);
    attackingFromMapUnits.addAll(units);
    // are we amphibious
    if (route.getStart().isWater() && (route.getEnd() != null) && !route.getEnd().isWater()
        && units.stream().anyMatch(Matches.unitIsLand())) {
      getAmphibiousAttackTerritories().add(route.getTerritoryBeforeEnd());
      m_amphibiousLandAttackers.addAll(CollectionUtils.getMatches(units, Matches.unitIsLand()));
      m_isAmphibious = true;
    }
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    if (!m_battleTracker.getDependentOn(this).isEmpty()) {
      throw new IllegalStateException("Must fight battles that this battle depends on first");
    }
    // create event
    bridge.getHistoryWriter().startEvent("Battle in " + m_battleSite, m_battleSite);
    // if any attacking non air units then win
    final boolean someAttacking = hasAttackingUnits();
    if (someAttacking) {
      m_whoWon = WhoWon.ATTACKER;
      m_battleResultDescription = BattleRecord.BattleResultDescription.BLITZED;
      m_battleTracker.takeOver(m_battleSite, m_attacker, bridge, null, null);
      m_battleTracker.addToConquered(m_battleSite);
    } else {
      m_whoWon = WhoWon.DEFENDER;
      m_battleResultDescription = BattleRecord.BattleResultDescription.LOST;
    }
    m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV,
        m_defenderLostTUV, m_battleResultDescription, new BattleResults(this, m_data));
    end();
  }

  private void end() {
    m_battleTracker.removeBattle(this);
    m_isOver = true;
  }

  boolean hasAttackingUnits() {
    final Predicate<Unit> attackingLand = Matches.alliedUnit(m_attacker, m_data).and(Matches.unitIsLand());
    return m_battleSite.getUnits().anyMatch(attackingLand);
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
    for (final Collection<Unit> dependent : m_dependentUnits.values()) {
      dependent.removeAll(units);
    }
  }

  @Override
  public boolean isEmpty() {
    return !hasAttackingUnits();
  }

  @Override
  public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units,
      final IDelegateBridge bridge, final boolean withdrawn) {
    if (withdrawn) {
      return;
    }
    Collection<Unit> lost = getDependentUnits(units);
    lost.addAll(CollectionUtils.intersection(units, m_attackingUnits));
    lost = CollectionUtils.getMatches(lost, Matches.unitIsInTerritory(m_battleSite));
    if (lost.size() != 0) {
      final String transcriptText = MyFormatter.unitsToText(lost) + " lost in " + m_battleSite.getName();
      bridge.getHistoryWriter().addChildToEvent(transcriptText, lost);
      final Change change = ChangeFactory.removeUnits(m_battleSite, lost);
      bridge.addChange(change);
    }
  }

  /**
   * Add dependent Units. Uninformative comment to suppress checkstyle warning.
   */
  public void addDependentUnits(final Map<Unit, Collection<Unit>> dependencies) {
    for (final Unit holder : dependencies.keySet()) {
      final Collection<Unit> transporting = dependencies.get(holder);
      if (m_dependentUnits.get(holder) != null) {
        m_dependentUnits.get(holder).addAll(transporting);
      } else {
        m_dependentUnits.put(holder, new LinkedHashSet<>(transporting));
      }
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
