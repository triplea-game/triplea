/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * StrategicBombingRaidBattle.java
 *
 * Created on November 29, 2001, 2:21 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * @author Sean Bridges
 * @version 1.0
 */
public class StrategicBombingRaidBattle implements Battle
{

    private final static String RAID = "Strategic bombing raid";
    private final static String FIRE_AA = "Fire AA";

    private Territory m_battleSite;
    private List<Unit> m_units = new ArrayList<Unit>();
    private PlayerID m_defender;
    private PlayerID m_attacker;
    private GameData m_data;
    private BattleTracker m_tracker;
    private boolean m_isOver = false;

    private final GUID m_battleID = new GUID();
    
    private final ExecutionStack m_stack = new ExecutionStack();
    private List<String> m_steps;
    private int m_bombingRaidCost;
    

    /** Creates new StrategicBombingRaidBattle */
    public StrategicBombingRaidBattle(Territory territory, GameData data, PlayerID attacker, PlayerID defender, BattleTracker tracker)
    {

        m_battleSite = territory;
        m_data = data;
        m_attacker = attacker;
        m_defender = defender;
        m_tracker = tracker;
    }

    /**
     * @param bridge
     * @return
     */
    private ITripleaDisplay getDisplay(IDelegateBridge bridge)
    {
        return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
    }

    public boolean isOver()
    {
        return m_isOver;
    }

    public boolean isEmpty()
    {

        return m_units.isEmpty();
    }

    public void removeAttack(Route route, Collection units)
    {
        m_units.removeAll(units);
    }

    public Change addAttackChange(Route route, Collection<Unit> units)
    {

        if (!Match.allMatch(units, Matches.UnitIsStrategicBomber))
            throw new IllegalArgumentException("Non bombers added to strategic bombing raid:" + units);

        m_units.addAll(units);
        return ChangeFactory.EMPTY_CHANGE;
    }

    
    
    public void fight(IDelegateBridge bridge)
    {
        //we were interrupted
        if(m_stack.isExecuting())
        {
            showBattle(bridge);
            m_stack.execute(bridge, m_data);
            return;
        }
            
       
        bridge.getHistoryWriter().startEvent("Strategic bombing raid in " + m_battleSite);
        bridge.getHistoryWriter().setRenderingData(m_battleSite);
        BattleCalculator.sortPreBattle(m_units, m_data);


        CompositeMatch<Unit> hasAAMatch = new CompositeMatchAnd<Unit>();
        hasAAMatch.add(Matches.UnitIsAA);
        hasAAMatch.add(Matches.enemyUnit(m_attacker, m_data));

        boolean hasAA = m_battleSite.getUnits().someMatch(hasAAMatch);

        m_steps = new ArrayList<String>();
        if (hasAA)
            m_steps.add(FIRE_AA);
        m_steps.add(RAID);

        showBattle(bridge);

        List<IExecutable> steps = new ArrayList<IExecutable>();
        
        
        if (hasAA)
            steps.add(new FireAA());

        steps.add(new ConductAA());
        
        steps.add(new IExecutable()
        {
        
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                getDisplay(bridge).gotoBattleStep(m_battleID, RAID);
                
                m_tracker.removeBattle(StrategicBombingRaidBattle.this);

                if(isSBRAffectsUnitProduction())
                	bridge.getHistoryWriter().addChildToEvent("AA raid costs " + m_bombingRaidCost + " " + " production in " + m_battleSite.getName());
                else
                	bridge.getHistoryWriter().addChildToEvent("AA raid costs " + m_bombingRaidCost + " " + MyFormatter.pluralize("ipc", m_bombingRaidCost));

                if(isPacificEdition() || isSBRVictoryPoints())
                {
                    if(m_defender.getName().equals(Constants.JAPANESE)) 
                    {
                        Change changeVP;
                        PlayerAttachment pa = (PlayerAttachment) PlayerAttachment.get(m_defender);
                        if(pa != null)
                        {
                            changeVP = ChangeFactory.attachmentPropertyChange(pa, (new Integer(-(m_bombingRaidCost / 10) + Integer.parseInt(pa.getVps()))).toString(), "vps");
                            bridge.addChange(changeVP);
                            bridge.getHistoryWriter().addChildToEvent("AA raid costs " + (m_bombingRaidCost / 10) + " " + MyFormatter.pluralize("vp", (m_bombingRaidCost / 10)));
                        } 
                    } 
                }
        
            }
        
        });
       
        
        steps.add(new IExecutable()
        {
        
            public void execute(ExecutionStack stack, IDelegateBridge bridge,
                    GameData data)
            {
                if(isSBRAffectsUnitProduction())
                    getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + m_bombingRaidCost + " production.");
                else
                    getDisplay(bridge).battleEnd(m_battleID, "Bombing raid cost " + m_bombingRaidCost + " " +  MyFormatter.pluralize("ipc", m_bombingRaidCost));
                m_isOver = true;        
            }
        
        });


        Collections.reverse(steps);
        for (IExecutable executable : steps)
        {
            m_stack.push(executable);
        }
        m_stack.execute(bridge, m_data);
        
        
    }

    private void showBattle(IDelegateBridge bridge)
    {
        String title = "Bombing raid in " + m_battleSite.getName();
        getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_units, getDefendingUnits(), Collections.<Unit, Collection<Unit>>emptyMap(), m_attacker, m_defender);
        getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
    }

    private List<Unit> getDefendingUnits()
    {
        return Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAAOrFactory);
    }

    class FireAA implements IExecutable
    {
        DiceRoll m_dice;
        Collection<Unit> m_casualties;
        
        public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
        {
            boolean isEditMode = EditDelegate.getEditMode(data);

            IExecutable roll = new IExecutable()
            {
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    m_dice = DiceRoll.rollAA(m_units.size(), bridge, m_battleSite, m_data);
                }
            };

            IExecutable calculateCasualties = new IExecutable()
            {
            
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    m_casualties = calculateCasualties(bridge, m_dice);
            
                }
            
            };
            
            IExecutable notifyCasualties = new IExecutable()
            {
            
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    notifyAAHits(bridge, m_dice, m_casualties);
            
                }
            
            };

            
            IExecutable removeHits = new IExecutable()
            {

                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    removeAAHits(bridge, m_dice, m_casualties);
                }
                
            };
            
            //push in reverse order of execution
            stack.push(removeHits);
            stack.push(notifyCasualties);
            stack.push(calculateCasualties);
            if (!isEditMode)
                stack.push(roll);
            
        }

    }
    
    /**
     * @return
     */
    private boolean isChooseAA()
	{
		return m_data.getProperties().get(Constants.CHOOSE_AA, false);
	}	

    /**
     * @return
     */
    private boolean isSBRAffectsUnitProduction()
	{
    	return games.strategy.triplea.Properties.getSBRAffectsUnitProduction(m_data);
	}	
    
    /**
     * @return
     */
    private boolean isFourthEdition()
    {
    	return games.strategy.triplea.Properties.getFourthEdition(m_data);
    }

    private boolean isRandomAACasualties()
    {
    	return games.strategy.triplea.Properties.getRandomAACasualties(m_data);
    }

    private boolean isLimitSBRDamageToProduction()
    {
    	return games.strategy.triplea.Properties.getLimitSBRDamageToProduction(m_data);
    }
    
	private boolean isLimitSBRDamagePerTurn(GameData data)
	{
    	return games.strategy.triplea.Properties.getLimitSBRDamagePerTurn(data);
	}
	
	private boolean isIPCCap(GameData data)
	{
    	return games.strategy.triplea.Properties.getIPCCap(data);
	}
	
    private boolean isSBRVictoryPoints()
    {
        return games.strategy.triplea.Properties.getSBRVictoryPoint(m_data);
    }
    
    private boolean isPacificEdition()
    {
        return games.strategy.triplea.Properties.getPacificEdition(m_data);
    }

    private Collection<Unit> calculateCasualties(IDelegateBridge bridge, DiceRoll dice)
    {
        Collection<Unit> casualties = null;
        boolean isEditMode = EditDelegate.getEditMode(m_data);
        if (isEditMode)
        {
            String text = "AA guns fire";
            CasualtyDetails casualtySelection = BattleCalculator.selectCasualties(RAID, m_attacker, 
                    m_units, bridge, text, m_data, /*dice*/ null,/*defending*/ false, m_battleID, /*headless*/ false, 0);
            return casualtySelection.getKilled();
        }     	
    	else if ((isFourthEdition() || isRandomAACasualties()) && !isChooseAA())
        {
            casualties = BattleCalculator.fourthEditionAACasualties(m_units, dice, bridge);
        }
        else
        {
            casualties = new ArrayList<Unit>(dice.getHits());
            for (int i = 0; i < dice.getHits() && i < m_units.size(); i++)
            {
                casualties.add(m_units.get(i));
            }
        }

        if (casualties.size() != dice.getHits())
            throw new IllegalStateException("Wrong number of casualties");
        
        return casualties;
    }

    private void notifyAAHits(final IDelegateBridge bridge, DiceRoll dice, Collection<Unit> casualties)
    {
        getDisplay(bridge).casualtyNotification(m_battleID, FIRE_AA, dice, m_attacker, casualties, Collections.<Unit>emptyList(), Collections.<Unit, Collection<Unit>>emptyMap());
        
        Runnable r = new Runnable()
        {
            public void run()
            {
                ITripleaPlayer defender = (ITripleaPlayer) bridge.getRemote(m_defender);
                defender.confirmEnemyCasualties(m_battleID, "Press space to continue", m_attacker);        
            }
        };
        Thread t = new Thread(r, "click to continue waiter");
        t.start();
        
        ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemote(m_attacker);
        attacker.confirmOwnCasualties(m_battleID, "Press space to continue");
        
        try
        {
            bridge.leaveDelegateExecution();
            t.join();
        } catch (InterruptedException e)
        {
          //ignore
        }
        finally
        {
            bridge.enterDelegateExecution();
        }
                
    }
    
    private void removeAAHits(IDelegateBridge bridge, DiceRoll dice, Collection<Unit> casualties)
    {
        if(!casualties.isEmpty())
            bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " killed by AA guns", casualties);

        m_units.removeAll(casualties);
        Change remove = ChangeFactory.removeUnits(m_battleSite, casualties);
        bridge.addChange(remove);
    }

    class ConductAA implements IExecutable
    {
        private int[] m_dice;
        
        public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
        {
            IExecutable rollDice = new IExecutable()
            {
            
                public void execute(ExecutionStack stack, IDelegateBridge bridge,
                        GameData data)
                {
                    rollDice(bridge);
                }
            
            };
            
            IExecutable findCost = new IExecutable()
            {
                public void execute(ExecutionStack stack, IDelegateBridge bridge, GameData data)
                {
                    findCost(bridge);
                }
                
            };
            
            //push in reverse order of execution
            m_stack.push(findCost);
            m_stack.push(rollDice);
            
            
        }
        
        private void rollDice(IDelegateBridge bridge)
        {
            int rollCount = BattleCalculator.getRolls(m_units, m_attacker, false);
            if (rollCount == 0)
            {
                m_dice = null;
                return;
            }
    
            boolean isEditMode = EditDelegate.getEditMode(m_data);
            if (isEditMode)
            {
                String annotation = m_attacker.getName() + " fixing dice to allocate ipc cost in strategic bombing raid against " + m_defender.getName() + " in "
                        + m_battleSite.getName();
                ITripleaPlayer attacker = (ITripleaPlayer) bridge.getRemote(m_attacker);
                m_dice = attacker.selectFixedDice(rollCount, 0, true, annotation);
            }
            else
            {
                String annotation = m_attacker.getName() + " rolling to allocate ipc cost in strategic bombing raid against " + m_defender.getName() + " in "
                        + m_battleSite.getName();
                m_dice = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);
            }

        }
        
        private void findCost(IDelegateBridge bridge)
        {
            //if no planes left after aa fires, this is possible
            if(m_units.isEmpty())
            {
                return;
            }
            
            TerritoryAttachment ta = TerritoryAttachment.get(m_battleSite);
            int cost = 0;
            boolean lhtrHeavyBombers = m_data.getProperties().get(Constants.LHTR_HEAVY_BOMBERS, false);
            
            int production = ta.getProduction();

            Iterator<Unit> iter = m_units.iterator();
            int index = 0;
            Boolean limitDamage = isFourthEdition() || isLimitSBRDamageToProduction();

            while (iter.hasNext())
            {
                int rolls;
                
                rolls = BattleCalculator.getRolls(iter.next(), m_attacker, false);
                
                int costThisUnit = 0;
                
                if(lhtrHeavyBombers && rolls > 1)
                {
                    int max = 0;
                    for(int i =0; i < rolls; i++)
                    {
                        //+1 since 0 based
                        max = Math.max(max, m_dice[index]  + 1);
                        index++;
                    }
                    //add 1
                    //costThisUnit = max + 1;
                    //Fix max to 6 rather than 7
                    costThisUnit = max;
                    
                }
                else
                {
                    for (int i = 0; i < rolls; i++)
                    {
                        costThisUnit += m_dice[index] + 1;
                        index++;
                    }                    
                }
                

                if (limitDamage)
                    cost += Math.min(costThisUnit, production);
                else
                    cost += costThisUnit;
            }

            if(isSBRAffectsUnitProduction())
            {
            	//get current production
                int unitProduction = ta.getUnitProduction();                
            	//Detemine the max that can be taken as losses
                //int alreadyLost = DelegateFinder.moveDelegate(m_data).ipcsAlreadyLost(m_battleSite);
                int alreadyLost = production - unitProduction;
                
                int limit = 2 * production  - alreadyLost;
                cost = Math.min(cost, limit);
        		
        		getDisplay(bridge).bombingResults(m_battleID, m_dice, cost);

            	// Record production lost
            	DelegateFinder.moveDelegate(m_data).ipcsLost(m_battleSite, cost);
            	//TODO COMCO add the damaged parm here.
            	/*
            	 * Match<Unit> damagedBattleship = new CompositeMatchAnd<Unit>(Matches.UnitIsTwoHit, Matches.UnitIsDamaged);
            	 * 
            	 *
            	 * 
while(iter.hasNext())
       {
           Unit unit = (Unit) iter.next();
           hits.put(unit,0);
       }
       aBridge.addChange(ChangeFactory.unitsHit(hits));
       aBridge.getHistoryWriter().startEvent(damaged.size() + " " +  MyFormatter.pluralize("unit", damaged.size()) + " repaired.");



void markDamaged(Collection<Unit> damaged, IDelegateBridge bridge)
    {

        if (damaged.size() == 0)
            return;
        Change damagedChange = null;
        IntegerMap<Unit> damagedMap = new IntegerMap<Unit>();
        damagedMap.putAll(damaged, 1);
        damagedChange = ChangeFactory.unitsHit(damagedMap);
        bridge.getHistoryWriter().addChildToEvent(
                "Units damaged: " + MyFormatter.unitsToText(damaged),
                damaged);
        bridge.addChange(damagedChange);

    }
            	 */
            	Change change = ChangeFactory.attachmentPropertyChange(ta, (new Integer(unitProduction - cost)).toString(), "unitProduction");
            	bridge.addChange(change);
            }
            else
            {
            	// Limit ipcs lost if we would like to cap ipcs lost at territory value
            	if (isIPCCap(m_data) || isLimitSBRDamagePerTurn(m_data))
            	{
            		int alreadyLost = DelegateFinder.moveDelegate(m_data).ipcsAlreadyLost(m_battleSite);
            		int limit = Math.max(0, production - alreadyLost);
            		cost = Math.min(cost, limit);
            	}

            	getDisplay(bridge).bombingResults(m_battleID, m_dice, cost);

            	//get resources
            	Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
            	int have = m_defender.getResources().getQuantity(ipcs);
            	int toRemove = Math.min(cost, have);

            	// Record ipcs lost
            	DelegateFinder.moveDelegate(m_data).ipcsLost(m_battleSite, toRemove);

            	Change change = ChangeFactory.changeResourcesChange(m_defender, ipcs, -toRemove);
            	bridge.addChange(change);
            }
            
            m_bombingRaidCost = cost;
               
        }   
    }

    public boolean isBombingRun()
    {

        return true;
    }

    public void unitsLostInPrecedingBattle(Battle battle, Collection units, IDelegateBridge bridge)
    {

        //should never happen
        throw new IllegalStateException("say what, why you telling me that");
    }

    public int hashCode()
    {

        return m_battleSite.hashCode();
    }

    public boolean equals(Object o)
    {

        //2 battles are equal if they are both the same type (boming or not)
        //and occur on the same territory
        //equals in the sense that they should never occupy the same Set
        //if these conditions are met
        if (o == null || !(o instanceof Battle))
            return false;

        Battle other = (Battle) o;
        return other.getTerritory().equals(this.m_battleSite) && other.isBombingRun() == this.isBombingRun();
    }

    public Territory getTerritory()
    {

        return m_battleSite;
    }

    
    public Collection<Unit> getDependentUnits(Collection<Unit> units)
    {
        return Collections.emptyList();
    }

    /**
     * Add bombarding unit. Doesn't make sense here so just do nothing.
     */
    public void addBombardingUnit(Unit unit)
    {
        // nothing
    }

    /**
     * Return whether battle is amphibious.
     */
    public boolean isAmphibious()
    {
        return false;
    }

    public Collection<Unit> getAmphibiousLandAttackers()
    {
        return new ArrayList<Unit>();
    }

    public Collection<Unit> getBombardingUnits()
    {
        return new ArrayList<Unit>();
    }
    
    public int getBattleRound()
    {
        return 0;
    }
    
}
