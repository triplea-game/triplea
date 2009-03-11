/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * PurchaseDelegate.java
 *
 * Created on November 2, 2001, 12:28 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitTypeComparator;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.triplea.ui.ProductionRepairPanel;
import games.strategy.triplea.ui.ProductionRepairPanel.Rule;
import games.strategy.triplea.ui.UnitChooser;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 *
 * Logic for purchasing units.
 *
 * Subclasses can override canAfford(...) to test if a purchase can be made
 *
 * Subclasses can over ride addToPlayer(...) and removeFromPlayer(...) to change how
 * the adding or removing of resources is done.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PurchaseDelegate implements IDelegate, IPurchaseDelegate
{
    private String m_name;
    private String m_displayName;
    private IDelegateBridge m_bridge;
    private PlayerID m_player;
    private GameData m_data;
    private static HashMap<Territory, Integer> m_repairCount = new HashMap<Territory, Integer>();

    public void initialize(String name, String displayName)
    {
        m_name = name;
        m_displayName = displayName;
    }


    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        m_bridge = aBridge;
        m_player = aBridge.getPlayerID();
        m_data = gameData;
    }

    public String getName()
    {
        return m_name;
    }

    public String getDisplayName()
    {
        return m_displayName;
    }

    protected GameData getData()
    {
        return m_data;
    }


    /**
     * subclasses can over ride this method to use different restrictions as to what a player can buy
     */
    protected boolean canAfford(IntegerMap<Resource> costs, PlayerID player)
    {
        return player.getResources().has(costs);
    }

    /**
     * Returns an error code, or null if all is good.
     */
    public String purchase(IntegerMap<ProductionRule> productionRules)
    {	  
        IntegerMap<Resource> costs = getCosts(productionRules);
        IntegerMap<NamedAttachable> results = getResults(productionRules);

        if(!(canAfford(costs, m_player)))
            return "Not enough resources";

        // remove first, since add logs ipcs remaining

        Iterator<NamedAttachable> iter = results.keySet().iterator();
        Collection<Unit> totalUnits = new ArrayList<Unit>();
        CompositeChange changes = new CompositeChange();

        // add changes for added resources
        //  and find all added units
        while(iter.hasNext() )
        {
            Object next = iter.next();
            if(next instanceof Resource)
            {
                Resource resource = (Resource) next;
                int quantity = results.getInt(resource);
                Change change = ChangeFactory.changeResourcesChange(m_player, resource, quantity);
                changes.add(change);
            } else
            {
                UnitType type = (UnitType) next;
                int quantity = results.getInt(type);
                Collection<Unit> units = type.create(quantity, m_player);
                totalUnits.addAll(units);

            }
        }

        // add changes for added units
        if(!totalUnits.isEmpty())
        {
            Change change = ChangeFactory.addUnits(m_player, totalUnits);
            changes.add(change);
        }

        // add changes for spent resources
        String remaining = removeFromPlayer(m_player, costs, changes, totalUnits);

        addHistoryEvent(totalUnits,  remaining);  

        // commit changes
        m_bridge.addChange(changes);



        return null;
    }
    /**
     * Returns an error code, or null if all is good.
     */
    public String purchaseRepair(HashMap<Territory, IntegerMap<RepairRule>> repairRules)
    {	  
        IntegerMap<Resource> costs = getRepairCosts(repairRules);

        IntegerMap<NamedAttachable> results = getRepairResults(repairRules);

        if(!(canAfford(costs, m_player)))
            return "Not enough resources";

        // remove first, since add logs ipcs remaining
        CompositeChange changes = new CompositeChange();
        Collection<Unit> totalUnits = new ArrayList<Unit>();

        Iterator<NamedAttachable> iter = results.keySet().iterator();

        // add changes for added resources
        //  and find all added units
        while(iter.hasNext() )
        {
            Object next = iter.next();
            if(next instanceof Resource)
            {
                Resource resource = (Resource) next;
                int quantity = results.getInt(resource);
                Change change = ChangeFactory.changeResourcesChange(m_player, resource, quantity);
                changes.add(change);
            } else
            {
                UnitType type = (UnitType) next;
                int quantity = results.getInt(type);
                Collection<Unit> units = type.create(quantity, m_player);
                totalUnits.addAll(units);

            }
        }

        //Get the map of the factories that were repaired and how much for each
        m_repairCount = getTerritoryRepairs(repairRules);

        if(!m_repairCount.isEmpty())
        {
            Collection<Territory> factoryTerrs = Match.getMatches(m_data.getMap().getTerritories(), Matches.territoryHasOwnedFactory(m_data, m_player));

            for(Territory terr : factoryTerrs)
            {
                if (m_repairCount.containsKey(terr))
                {
                    int repairCount = m_repairCount.get(terr);
                    TerritoryAttachment ta = TerritoryAttachment.get(terr);
                    int current = ta.getUnitProduction();

                    IntegerMap<Unit> hits = new IntegerMap<Unit>();
                    Collection<Unit> factories = Match.getMatches(terr.getUnits().getUnits(), Matches.UnitIsFactory);

                    //reset factory damage marker if full production restored
                    if(current + repairCount == ta.getProduction())
                    {
                        hits.put(factories.iterator().next(), 0);
                        changes.add(ChangeFactory.unitsHit(hits));
                    }            	

                    changes.add(ChangeFactory.attachmentPropertyChange(ta, (new Integer(current + repairCount)).toString(), "unitProduction"));
                }
            }
        }
        else
        {
            return "m_repairCount is empty";
        }

        // add changes for spent resources
        String remaining = removeFromPlayer(m_player, costs, changes, totalUnits);

        addHistoryEvent(totalUnits,  remaining);  

        // commit changes
        m_bridge.addChange(changes);



        return null;
    }

    private HashMap<Territory, Integer> getTerritoryRepairs(HashMap<Territory, IntegerMap<RepairRule>> repairRules)
    {
        HashMap<Territory, Integer> repairMap = new HashMap<Territory, Integer>();
        
        Set entries = repairRules.keySet();
        Iterator iter = entries.iterator();
        while (iter.hasNext())
        {
            Territory terr = (Territory) iter.next();
            IntegerMap<RepairRule> rules = repairRules.get(terr);

            TreeSet<RepairRule> repRules = new TreeSet<RepairRule>(repairRuleComparator);
            repRules.addAll(rules.keySet());
            Iterator<RepairRule> ruleIter = repRules.iterator();
            while (ruleIter.hasNext())
            {
                RepairRule repairRule = ruleIter.next();
                int quantity = rules.getInt(repairRule);

                repairMap.put(terr, quantity);
            }        
        }
        
        return repairMap;        
    }
    
    Comparator<RepairRule> repairRuleComparator = new Comparator<RepairRule>()
    {
        UnitTypeComparator utc = new UnitTypeComparator();

        public int compare(RepairRule o1, RepairRule o2)
        {
            UnitType u1 = (UnitType)  o1.getResults().keySet().iterator().next();
            UnitType u2 = (UnitType)  o2.getResults().keySet().iterator().next();
            return utc.compare(u1, u2);
        }
    };
    
    private void addHistoryEvent(Collection<Unit> totalUnits, String remainingText)
    {
        // add history event
        String transcriptText;
        if(!totalUnits.isEmpty())
            transcriptText = m_player.getName() + " buy " + MyFormatter.unitsToTextNoOwner(totalUnits)+"; "+ remainingText;
        else
            transcriptText = m_player.getName() + " buy nothing; "+ remainingText;
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        m_bridge.getHistoryWriter().setRenderingData(totalUnits);
    }

    private IntegerMap<Resource> getCosts(IntegerMap<ProductionRule> productionRules)
    {
        IntegerMap<Resource> costs = new IntegerMap<Resource>();

        Iterator<ProductionRule> rules = productionRules.keySet().iterator();
        while(rules.hasNext() )
        {
            ProductionRule rule = rules.next();
            costs.addMultiple(rule.getCosts(), productionRules.getInt(rule));
        }
        return costs;
    }


    private IntegerMap<Resource> getRepairCosts(HashMap<Territory, IntegerMap<RepairRule>> repairRules)
    {        
        Collection<Territory> terrs = repairRules.keySet();
        Iterator<Territory> iter = terrs.iterator();    
        IntegerMap<Resource> costs = new IntegerMap<Resource>();        

        while(iter.hasNext())
        {
            Territory terr = iter.next();

            Iterator<RepairRule> rules = repairRules.get(terr).keySet().iterator();

            while(rules.hasNext() )
            {
                RepairRule rule = rules.next();
                costs.addMultiple(rule.getCosts(), repairRules.get(terr).getInt(rule));
            }
        }
       
        return costs;
    }

    private IntegerMap<NamedAttachable> getResults(IntegerMap<ProductionRule> productionRules)
    {
        IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();

        Iterator<ProductionRule> rules = productionRules.keySet().iterator();
        while(rules.hasNext() )
        {
            ProductionRule rule = rules.next();
            costs.addMultiple(rule.getResults(), productionRules.getInt(rule));
        }
        return costs;
    }

    private IntegerMap<NamedAttachable> getRepairResults(HashMap<Territory, IntegerMap<RepairRule>> repairRules)
    {
        Collection<Territory> terrs = repairRules.keySet();
        Iterator<Territory> iter = terrs.iterator();    
        IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();        

        while(iter.hasNext())
        {
            Territory terr = iter.next();

            Iterator<RepairRule> rules = repairRules.get(terr).keySet().iterator();

            while(rules.hasNext() )
            {
                RepairRule rule = rules.next();
                costs.addMultiple(rule.getResults(), repairRules.get(terr).getInt(rule));
            }
        }
        /*IntegerMap<NamedAttachable> costs = new IntegerMap<NamedAttachable>();

        Iterator<RepairRule> rules = repairRules.keySet().iterator();
        while(rules.hasNext() )
        {
            RepairRule rule = rules.next();
            costs.addMultiple(rule.getResults(), repairRules.getInt(rule));
        }*/
        return costs;
    }




    /**
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {
        return null;
    }

    /**
     * Loads the delegates state
     */
    public void loadState(Serializable state)
    {}


    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
        //this space intentionally left blank
    }


    /* 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return IPurchaseDelegate.class;
    }


    protected String removeFromPlayer(PlayerID player, IntegerMap<Resource> costs, CompositeChange changes, Collection<Unit> totalUnits)
    {
        Iterator<Resource> costsIter = costs.keySet().iterator();
        int AvailIPCs = player.getResources().getQuantity(Constants.IPCS);

        while(costsIter.hasNext() )
        {
            Resource resource = costsIter.next();
            float quantity = costs.getInt(resource);
            int cost = (int) quantity;

            if(isIncreasedFactoryProduction(player))
            {
                Set<UnitCategory> categorized = UnitSeperator.categorize(totalUnits);
                if (categorized.size() == 1)
                {
                    UnitCategory unitCategory =  categorized.iterator().next();
                    if(unitCategory.getType().getName().endsWith("_hit"))
                        cost = (int) (Math.round(quantity/2));
                }
            }

            Change change = ChangeFactory.changeResourcesChange(m_player, resource, -cost);
            changes.add(change);

            return m_player.getResources().getQuantity(resource) -  cost + " ipcs remaining"; 
        }
        return "";
    }


    private boolean isIncreasedFactoryProduction(PlayerID player)    
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
        if(ta == null)
            return false;
        return ta.hasIncreasedFactoryProduction();
    }

}

