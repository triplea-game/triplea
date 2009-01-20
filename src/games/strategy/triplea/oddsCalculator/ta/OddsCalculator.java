package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.Change;
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
import games.strategy.triplea.ui.display.DummyDisplay;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class OddsCalculator
{
    
   
    private PlayerID m_attacker;
    private PlayerID m_defender;
    private GameData m_data;
    private Territory m_location;
    private Collection<Unit> m_attackingUnits = new ArrayList<Unit>();
    private Collection<Unit> m_defendingUnits = new ArrayList<Unit>();
    private Collection<Unit> m_bombardingUnits = new ArrayList<Unit>();
    private boolean m_keepOneAttackingLandUnit = false;
    
    public OddsCalculator()
    {
        
    }

    

    @SuppressWarnings("unchecked")
    public AggregateResults calculate(GameData data, PlayerID attacker, PlayerID defender,  Territory location, Collection<Unit> attacking, Collection<Unit>  defending, Collection<Unit> bombarding, int runCount)
    {
       m_data = GameDataUtils.cloneGameData(data, false);
       m_attacker =  (PlayerID) GameDataUtils.translateIntoOtherGameData(attacker, data);
       m_defender =  (PlayerID) GameDataUtils.translateIntoOtherGameData(defender, data);
       m_location = (Territory) GameDataUtils.translateIntoOtherGameData(location, data);
       m_attackingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(attacking, data);
       m_defendingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(defending, data);
       m_bombardingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(bombarding, data);
       
             
       
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
        
        for(int i =0; i < count; i++)
        {
            final CompositeChange allChanges = new CompositeChange();
            DummyDelegateBridge bridge1 = new DummyDelegateBridge(m_attacker, m_data, allChanges, m_keepOneAttackingLandUnit);
            TripleADelegateBridge bridge = new TripleADelegateBridge(bridge1, m_data);
            MustFightBattle battle = new MustFightBattle(m_location, m_attacker, m_data, battleTracker);
            battle.setHeadless(true);
            battle.setUnits(m_defendingUnits, m_attackingUnits, m_bombardingUnits, m_defender);
            
            battle.fight(bridge);
            
            rVal.addResult(new BattleResults(battle));
            
            //restore the game to its original state
            new ChangePerformer(m_data).perform( allChanges.invert());
            
            battleTracker.clear();
            
        }
        rVal.setTime(System.currentTimeMillis() - start);
        
        return rVal;
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
    protected void purchase(boolean purcahseForBid, int ipcsToSpend, IPurchaseDelegate purchaseDelegate, GameData data, PlayerID player)
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

    //Added new collection autoKilled to handle killing units prior to casualty selection
    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID)
    {
        List<Unit> rDamaged = new ArrayList<Unit>();
        List<Unit> rKilled = new ArrayList<Unit>();
        
        for(Unit unit : defaultCasualties)
        {
            boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
            //if it appears twice it then it both damaged and killed
            if(unit.getHits() == 0 && twoHit && !rDamaged.contains(unit))
                rDamaged.add(unit);
            else 
                rKilled.add(unit);
        }
        
        
        if(m_keepAtLeastOneLand)
        {
            List<Unit> notKilled = new ArrayList<Unit>(selectFrom);
            notKilled.removeAll(rKilled);
            //no land units left, but we
            //have a non land unit to kill
            //and land unit was killed
            if(!Match.someMatch(notKilled, Matches.UnitIsLand) && Match.someMatch(notKilled, Matches.UnitIsNotLand) && Match.someMatch(rKilled, Matches.UnitIsLand))
            {
                List<Unit> notKilledAndNotLand =Match.getMatches(notKilled, Matches.UnitIsNotLand); 
                 
                
                //sort according to cost
                Collections.sort(notKilledAndNotLand, AIUtils.getCostComparator());                
                
                //remove the last killed unit, this should be the strongest
                rKilled.remove(rKilled.size() -1);
                //add the cheapest unit 
                rKilled.add(notKilledAndNotLand.get(0));
            }
                
        }
        
        CasualtyDetails m2 = new CasualtyDetails(rKilled, rDamaged, false);
        return m2;
    }
    
    public Territory selectTerritoryForAirToLand(Collection<Territory> candidates)
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
