package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.*;

import java.util.*;

/**
 * 
 * Code to fire aa guns while in combat and non combat move.
 * 
 * @author Sean Bridges
 */
public class AAInMoveUtil
{
    
    private final boolean m_nonCombat;
    private final GameData m_data;
    private final IDelegateBridge m_bridge;
    private final PlayerID m_player;
    
    AAInMoveUtil(IDelegateBridge bridge, GameData data)
    {
        m_nonCombat = MoveDelegate.isNonCombat(bridge);
        m_data = data;
        m_bridge = bridge;
        m_player = bridge.getPlayerID();
    }
    
    private boolean isFourEdition()
    {
        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }
    
    private boolean isAlwaysONAAEnabled()
    {
        return m_data.getProperties().get(Constants.ALWAYS_ON_AA_PROPERTY, false);
    }
    
    private ITripleaPlayer getRemotePlayer(PlayerID id)
    {
        return (ITripleaPlayer) m_bridge.getRemote(id);
    }
    
    private ITripleaPlayer getRemotePlayer()
    {
        return getRemotePlayer(m_player);
    }
    
    /**
     * Fire aa guns. Returns units to remove.
     */
    Collection<Unit> fireAA(Route route, Collection<Unit> units, Comparator<Unit> decreasingMovement, UndoableMove currentMove)
    {
        List<Unit> targets = Match.getMatches(units, Matches.UnitIsAir);

        //select units with lowest movement first
        Collections.sort(targets, decreasingMovement);
        Collection<Unit> originalTargets = new ArrayList<Unit>(targets);
        
        Iterator iter = getTerritoriesWhereAAWillFire(route, units).iterator();
        while (iter.hasNext())
        {
            Territory location = (Territory) iter.next();
            fireAA(location, targets, currentMove);
        }

        return Util.difference(originalTargets, targets);

    }

    Collection<Territory> getTerritoriesWhereAAWillFire(Route route, Collection<Unit> units)
    {
        if (m_nonCombat && !isAlwaysONAAEnabled())
            return Collections.emptyList();

        if (Match.noneMatch(units, Matches.UnitIsAir))
            return Collections.emptyList();

        //dont iteratate over the end
        //that will be a battle
        //and handled else where in this tangled mess
        CompositeMatch<Unit> hasAA = new CompositeMatchAnd<Unit>();
        hasAA.add(Matches.UnitIsAA);
        hasAA.add(Matches.enemyUnit(m_player, m_data));

        List<Territory> territoriesWhereAAWillFire = new ArrayList<Territory>();

        for (int i = 0; i < route.getLength() - 1; i++)
        {
            Territory current = route.at(i);

            //aa guns in transports shouldnt be able to fire
            if (current.getUnits().someMatch(hasAA) && !current.isWater())
            {
                territoriesWhereAAWillFire.add(current);
            }
        }

        //check start as well, prevent user from moving to and from aa sites
        // one at a time
        //if there was a battle fought there then dont fire
        //this covers the case where we fight, and always on aa wants to fire
        //after the battle.
        //TODO
        //there is a bug in which if you move an air unit to a battle site
        //in the middle of non combat, it wont fire
        if (route.getStart().getUnits().someMatch(hasAA)
                && !getBattleTracker().wasBattleFought(route.getStart()))
            territoriesWhereAAWillFire.add(route.getStart());
 
        return territoriesWhereAAWillFire;
    }
    
    private BattleTracker getBattleTracker()
    {
        return DelegateFinder.battleDelegate(m_data).getBattleTracker();
    }
    
    /**
     * Fire the aa units in the given territory, hits are removed from units
     */
    private void fireAA(Territory territory, Collection<Unit> units, UndoableMove currentMove)
    {
        
        if(units.isEmpty())
            return;

        //once we fire the aa guns, we cant undo
        //otherwise you could keep undoing and redoing
        //until you got the roll you wanted
        currentMove.setCantUndo("Move cannot be undone after AA has fired.");
        DiceRoll dice = DiceRoll.rollAA(units.size(), m_bridge, territory, m_data);
        int hitCount = dice.getHits();

        if (hitCount == 0)
        {
            getRemotePlayer().reportMessage("No aa hits in " + territory.getName());
        } else
            selectCasualties(dice, units, territory);
    }

    /**
     * hits are removed from units. Note that units are removed in the order
     * that the iterator will move through them.
     */
    private void selectCasualties(DiceRoll dice, Collection<Unit> units, Territory territory)
    {

        String text = "Select " + dice.getHits() + " casualties from aa fire in " + territory.getName();
        // If fourth edition, select casualties randomnly
        Collection<Unit> casualties = null;
        if (isFourEdition())
        {
            casualties = BattleCalculator.fourthEditionAACasualties(units, dice, m_bridge);
        } else
        {
            CasualtyDetails casualtyMsg = BattleCalculator.selectCasualties(m_player, units, m_bridge, text, m_data, dice, false);
            casualties = casualtyMsg.getKilled();
        }

        getRemotePlayer().reportMessage(dice.getHits() + " AA hits in " + territory.getName());
        
        m_bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " lost in " + territory.getName(), casualties);
        units.removeAll(casualties);
    }
    
    
}
