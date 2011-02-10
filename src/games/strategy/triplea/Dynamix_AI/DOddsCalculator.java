/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.Dynamix_AI;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHitsChange;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.baseAI.AIUtils;
import games.strategy.triplea.baseAI.AbstractAI;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.delegate.TripleADelegateBridge;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.ui.display.DummyDisplay;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Stephen
 */
public class DOddsCalculator
{
    private PlayerID m_attacker;
    private PlayerID m_defender;
    private static GameData s_dataForSimulation;
    private Territory m_location;
    private Collection<Unit> m_attackingUnits = new ArrayList<Unit>();
    private Collection<Unit> m_defendingUnits = new ArrayList<Unit>();
    private Collection<Unit> m_bombardingUnits = new ArrayList<Unit>();
    private boolean m_keepOneAttackingLandUnit = false;
    private volatile boolean m_cancelled = false;

    public DOddsCalculator()
    {

    }
    public static GameData GetDataForSimulation(GameData data)
    {
        data.acquireReadLock();
        if(s_dataForSimulation == null)
        {
            try
            {
                s_dataForSimulation = GameDataUtils.cloneGameData(data, true);
            }
            finally
            {
                data.releaseReadLock();
            }
        }
        return s_dataForSimulation;
    }

    public static void Initialize(GameData data)
    {
        data.acquireReadLock();
        try
        {
            s_dataForSimulation = GameDataUtils.cloneGameData(data, true);
        }
        finally
        {
            data.releaseReadLock();
        }
    }

    @SuppressWarnings("unchecked")
    public AggregateResults calculate(GameData data, PlayerID attacker, PlayerID defender, Territory location, Collection<Unit> attacking, Collection<Unit> defending, Collection<Unit> bombarding, int runCount)
    {
        m_attacker = s_dataForSimulation.getPlayerList().getPlayerID(attacker.getName());
        m_defender = s_dataForSimulation.getPlayerList().getPlayerID(defender.getName());
        m_location = s_dataForSimulation.getMap().getTerritory(location.getName());

        if (m_attacker == null)
            m_attacker = PlayerID.NULL_PLAYERID;
        if (m_defender == null)
            m_defender = PlayerID.NULL_PLAYERID;

        data.acquireReadLock();
        try
        {
            m_attackingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(attacking, data);
            m_defendingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(defending, data);
            m_bombardingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(bombarding, data);
        }
        finally
        {
            data.releaseReadLock();
        }

        new ChangePerformer(s_dataForSimulation).perform(ChangeFactory.removeUnits(m_location, m_location.getUnits().getUnits()));
        new ChangePerformer(s_dataForSimulation).perform(ChangeFactory.addUnits(m_location, m_attackingUnits));
        new ChangePerformer(s_dataForSimulation).perform(ChangeFactory.addUnits(m_location, m_defendingUnits));

        return calculate(runCount);
    }

    public void setKeepOneAttackingLandUnit(boolean aBool)
    {
        m_keepOneAttackingLandUnit = aBool;
    }



    private AggregateResults calculate(int count)
    {

        long start = System.currentTimeMillis();
        AggregateResults rVal = new AggregateResults(count);
        BattleTracker battleTracker = new BattleTracker();


        for(int i =0; i < count && !m_cancelled; i++)
        {
            final CompositeChange allChanges = new CompositeChange();
            DummyDelegateBridge bridge1 = new DummyDelegateBridge(m_attacker, s_dataForSimulation, allChanges, m_keepOneAttackingLandUnit);
            TripleADelegateBridge bridge = new TripleADelegateBridge(bridge1, s_dataForSimulation);
            MustFightBattle battle = new MustFightBattle(m_location, m_attacker, s_dataForSimulation, battleTracker);
            battle.setHeadless(true);
            battle.setUnits(m_defendingUnits, m_attackingUnits, m_bombardingUnits, m_defender);

            battle.fight(bridge);

            rVal.addResult(new BattleResults(battle));

            //restore the game to its original state
            new ChangePerformer(s_dataForSimulation).perform( allChanges.invert());

            battleTracker.clear();

        }
        rVal.setTime(System.currentTimeMillis() - start);

        return rVal;
    }



    public void cancel()
    {
        m_cancelled = true;
    }

}

class DummyDelegateBridge implements IDelegateBridge
{

    private final PlainRandomSource m_randomSource = new PlainRandomSource();
    private final DummyDisplay m_display = new DummyDisplay();
    private final DummyPlayer m_attackingPlayer;
    private final DummyPlayer m_defendingPlayer;
    private final PlayerID m_attacker;
    private final DelegateHistoryWriter m_writer = new DelegateHistoryWriter(new DummyGameModifiedChannel());
    private final CompositeChange m_allChanges;
    private final GameData m_data;
    private final ChangePerformer m_changePerformer;

    public DummyDelegateBridge(PlayerID attacker, GameData data, CompositeChange allChanges, boolean attackerKeepOneLandUnit)
    {
        m_attackingPlayer = new DummyPlayer("battle calc dummy", attackerKeepOneLandUnit);
        m_defendingPlayer = new DummyPlayer("battle calc dummy", false);

        m_data = data;
        m_attacker = attacker;
        m_allChanges = allChanges;
        m_changePerformer = new ChangePerformer(m_data);
    }

   public void leaveDelegateExecution()
   {}

   public Properties getStepProperties()
   {
       throw new UnsupportedOperationException();
   }

   public String getStepName()
   {
       throw new UnsupportedOperationException();
   }

   public IRemote getRemote(PlayerID id)
   {
       if(id.equals(m_attacker))
           return m_attackingPlayer;
       else
           return m_defendingPlayer;
   }

   public IRemote getRemote()
   {
       //the current player is attacker
       return m_attackingPlayer;
   }

   public int[] getRandom(int max, int count, String annotation)
   {
       return m_randomSource.getRandom(max, count, annotation);
   }

   public int getRandom(int max, String annotation)
   {
       return m_randomSource.getRandom(max, annotation);
   }

   public PlayerID getPlayerID()
   {
       return m_attacker;
   }

   public IDelegateHistoryWriter getHistoryWriter()
   {
       return m_writer;
   }

   public IChannelSubscribor getDisplayChannelBroadcaster()
   {
       return m_display;
   }

   public void enterDelegateExecution()
   {}

   public void addChange(Change aChange)
   {
       if(!(aChange instanceof UnitHitsChange))
           return;

       m_allChanges.add(aChange);
       m_changePerformer.perform(aChange);

   }

   public void stopGameSequence() {}

};




class DummyGameModifiedChannel implements IGameModifiedChannel
{

    public void addChildToEvent(String text, Object renderingData)
    {
    }

    public void gameDataChanged(Change aChange)
    {
    }

    public void setRenderingData(Object renderingData)
    {
    }

    public void shutDown()
    {
    }

    public void startHistoryEvent(String event)
    {
    }

    public void stepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName, boolean loadedFromSavedGame)
    {
    }

}

class DummyPlayer extends AbstractAI
{

    private final boolean m_keepAtLeastOneLand;

    public DummyPlayer(String name, boolean keepAtLeastOneLand)
    {
        super(name);
        m_keepAtLeastOneLand = keepAtLeastOneLand;
    }

    @Override
    protected void move(boolean nonCombat, IMoveDelegate moveDel, GameData data, PlayerID player)
    {}

    @Override
    protected void place(boolean placeForBid, IAbstractPlaceDelegate placeDelegate, GameData data, PlayerID player)
    {}

    @Override
    protected void purchase(boolean purcahseForBid, int PUsToSpend, IPurchaseDelegate purchaseDelegate, GameData data, PlayerID player)
    {}

    @Override
    protected void tech(ITechDelegate techDelegate, GameData data, PlayerID player)
    {}

    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories)
    {
        throw new UnsupportedOperationException();
    }

    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(Collection<Unit> fightersThatCanBeMoved, Territory from)
    {
        throw new UnsupportedOperationException();
    }

    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message)
    {
        //no retreat, no surrender
        return null;
    }

    public Collection<Unit> scrambleQuery(GUID battleID, Collection<Territory> possibleTerritories, String message)
    {
        //no scramble
        return null;
    }
    boolean useDefaultSelectionThisTime = false;
    @Override
    public void reportError(String error)
    {
        DUtils.Log_Finer("Error message reported: ", error);
        if (error.equals("Wrong number of casualties selected") || error.equals("Cannot remove enough units of those types"))
        {
            useDefaultSelectionThisTime = true;
        }
    }
    //Added new collection autoKilled to handle killing units prior to casualty selection
    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID)
    {
        List<Unit> damaged = new ArrayList<Unit>();
        List<Unit> destroyed = new ArrayList<Unit>();

        if (useDefaultSelectionThisTime)
        {
            useDefaultSelectionThisTime = false;
            for (Unit unit : defaultCasualties)
            {
                boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
                //If it appears in casualty list once, it's damaged, if twice, it's damaged and additionally destroyed
                if (unit.getHits() == 0 && twoHit && !damaged.contains(unit))
                    damaged.add(unit);
                else if(!destroyed.contains(unit))
                    destroyed.add(unit);
                else
                    damaged.add(unit); //Fix for strange 'three default casualty units are equal' problem
            }
        }
        else
        {
            for (Unit unit : defaultCasualties)
            {
                boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
                //If it appears in casualty list once, it's damaged, if twice, it's damaged and additionally destroyed
                if (unit.getHits() == 0 && twoHit && !damaged.contains(unit))
                    damaged.add(unit);
                else if(!destroyed.contains(unit))
                    destroyed.add(unit);
                else
                    damaged.add(unit); //Fix for strange 'three default casualty units are equal' problem
            }


            if (m_keepAtLeastOneLand)
            {
                List<Unit> notKilled = new ArrayList<Unit>(selectFrom);
                notKilled.removeAll(destroyed);
                //No land units left, but we
                //have a non land unit to kill
                //and land unit was killed
                if (!Match.someMatch(notKilled, Matches.UnitIsLand) && Match.someMatch(notKilled, Matches.UnitIsNotLand) && Match.someMatch(destroyed, Matches.UnitIsLand))
                {
                    List<Unit> notKilledAndNotLand = Match.getMatches(notKilled, Matches.UnitIsNotLand);

                    //sort according to cost
                    Collections.sort(notKilledAndNotLand, AIUtils.getCostComparator());

                    //remove the last killed unit, this should be the strongest
                    destroyed.remove(destroyed.size() - 1);
                    //add the cheapest unit
                    destroyed.add(notKilledAndNotLand.get(0));
                }

            }
        }

        CasualtyDetails m2 = new CasualtyDetails(destroyed, damaged, false);
        return m2;
    }

    public Territory selectTerritoryForAirToLand(Collection<Territory> candidates)
    {
        throw new UnsupportedOperationException();
    }
    public Unit whatShouldBomberBomb(Territory territory, Collection<Unit> units)
    {
        throw new UnsupportedOperationException();
    }
    public boolean shouldBomberBomb(Territory territory)
    {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, java.lang.String)
     */
    public int[] selectFixedDice(int numRolls, int hitAt, boolean hitOnlyIfEquals, String message)
    {
        int[] dice = new int[numRolls];
        for (int i=0; i<numRolls; i++)
        {
            dice[i] = (int)Math.ceil(Math.random() * 6);
        }
        return dice;
    }

}
