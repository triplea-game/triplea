package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.BattleRecord.BattleResultDescription;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * A sort of scripted battle made for blitzed/conquered territories without a fight.
 * TODO: expand to cover all possible scripting battle needs.
 */
public class FinishedBattle extends AbstractBattle {
  private static final long serialVersionUID = -5852495231826940879L;
  private final Set<Territory> m_attackingFrom = new HashSet<>();
  private final Collection<Territory> m_amphibiousAttackFrom = new ArrayList<>();
  // maps Territory-> units (stores a collection of who is attacking from where, needed for undoing moves)
  private final Map<Territory, Collection<Unit>> m_attackingFromMap = new HashMap<>();

  FinishedBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker,
      final boolean isBombingRun, final BattleType battleType, final GameData data,
      final BattleResultDescription battleResultDescription, final WhoWon whoWon) {
    super(battleSite, attacker, battleTracker, isBombingRun, battleType, data);
    m_battleResultDescription = battleResultDescription;
    m_whoWon = whoWon;
  }

  public void setDefendingUnits(final List<Unit> defendingUnits) {
    m_defendingUnits = defendingUnits;
  }

  @Override
  public boolean isEmpty() {
    return m_attackingUnits.isEmpty();
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    if (!m_headless) {
      m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV,
          m_defenderLostTUV, m_battleResultDescription, new BattleResults(this, m_data));
    }
    m_battleTracker.removeBattle(this);
    m_isOver = true;
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
      m_amphibiousAttackFrom.add(route.getTerritoryBeforeEnd());
      m_amphibiousLandAttackers.addAll(CollectionUtils.getMatches(units, Matches.unitIsLand()));
      m_isAmphibious = true;
    }
    return ChangeFactory.EMPTY_CHANGE;
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
        m_amphibiousAttackFrom.remove(attackingFrom);
        // do we have any amphibious attacks left?
        m_isAmphibious = !m_amphibiousAttackFrom.isEmpty();
      }
    }
    for (final Collection<Unit> dependent : m_dependentUnits.values()) {
      dependent.removeAll(units);
    }
  }

  @Override
  public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units,
      final IDelegateBridge bridge, final boolean withdrawn) {
    final Collection<Unit> lost = getDependentUnits(units);
    lost.addAll(CollectionUtils.intersection(units, m_attackingUnits));
    if (lost.size() != 0) {
      m_attackingUnits.removeAll(lost);
      /*
       * TODO: these units are no longer in this territory, most probably. Plus they may have already been removed by
       * another "real" battle
       * class.
       * final String transcriptText = MyFormatter.unitsToText(lost) + " lost in " + m_battleSite.getName();
       * bridge.getHistoryWriter().startEvent(transcriptText);
       * final Change change = ChangeFactory.removeUnits(m_battleSite, lost);
       * bridge.addChange(change);
       */
      if (m_attackingUnits.isEmpty()) {
        final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(m_attacker, m_data);
        final int tuvLostAttacker = (withdrawn ? 0 : TuvUtils.getTuv(lost, m_attacker, costs, m_data));
        m_attackerLostTUV += tuvLostAttacker;
        // scripted?
        m_whoWon = WhoWon.DEFENDER;
        if (!m_headless) {
          m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender,
              m_attackerLostTUV, m_defenderLostTUV, BattleRecord.BattleResultDescription.LOST,
              new BattleResults(this, m_data));
        }
        m_battleTracker.removeBattle(this);
      }
    }
  }

  Map<Territory, Collection<Unit>> getAttackingFromMap() {
    return m_attackingFromMap;
  }
}
