package games.strategy.triplea.attatchments;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;


public class TriggerAttachment extends DefaultAttachment{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3327739180569606093L;
	
	private RulesAttachment m_trigger = null;
	private ProductionFrontier m_frontier = null;
	private boolean m_invert = false;
	private String m_tech = null;
	private Map<Territory,IntegerMap<UnitType>> m_placement = null;
	
	
	public TriggerAttachment() {
	}
	
	public static Set<TriggerAttachment> getTriggers(PlayerID player, GameData data, Match<TriggerAttachment> cond){
		Set<TriggerAttachment> trigs = new HashSet<TriggerAttachment>();
        Map<String, IAttachment> map = player.getAttachments();
        Iterator<String> iter = map.keySet().iterator();
        while(iter.hasNext() )
        {
        	IAttachment a = map.get(iter.next());
        	if(a instanceof TriggerAttachment && cond.match((TriggerAttachment)a))
        		trigs.add((TriggerAttachment)a);
        }
        return trigs;
	}
	public void setTrigger(String s) throws GameParseException{
		RulesAttachment trigger = (RulesAttachment)getAttatchedTo().getAttachment(s);
		if(trigger == null)
            throw new GameParseException("Could not find rule. name:" + s);
        m_trigger = trigger;
	}
	
	public RulesAttachment getTrigger() {
		return m_trigger;
	}
	
	public void setFrontier(String s) throws GameParseException{
		ProductionFrontier front = getData().getProductionFrontierList().getProductionFrontier(s);
		if(front == null)
            throw new GameParseException("Could not find frontier. name:" + s);
        m_frontier = front;
	}
	
	public ProductionFrontier getFrontier() {
		return m_frontier;
	}
	
	public boolean getInvert() {
		return m_invert;
	}
	
	public void setInvert(String s) {
		m_invert = getBool(s);
	}
	
	public String getTech() {
		return m_tech;
	}
	
	public void setTech(String s) {
		m_tech = s;
	}
	
	public Map<Territory,IntegerMap<UnitType>> getPlacement() {
		return m_placement;
	}
	// fudging this, it really represents adding placements
	public void setPlacement(String place) throws GameParseException{
		String[] s = place.split(":");
    	int count = -1,i=0;
    	if(s.length<1)
    		throw new GameParseException( "Empty placement list");
    	try {
    		count = getInt(s[0]);
    		i++;
    	} catch(Exception e) {
    		count = 1;
    	}
    	if(s.length<1 || s.length ==1 && count != -1)
    		throw new GameParseException( "Empty placement list");
    	Territory territory = getData().getMap().getTerritory(s[i]);
    	if( territory == null )
			throw new GameParseException( "Territory does not exist " + s[i]);
    	else {
    		i++;
    		IntegerMap<UnitType> map = new IntegerMap<UnitType>();
    		for( ; i < s.length; i++){
    			UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
    			if(type == null)
    				throw new GameParseException( "UnitType does not exist " + s[i]);
    			else
    				map.add(type, count);
    		}
    		if( m_placement == null)
    			m_placement = new HashMap<Territory,IntegerMap<UnitType>>();
    		if(m_placement.containsKey(territory))
    			map.add(m_placement.get(territory));
    		m_placement.put(territory, map);
    	}	
	}
	
	public static void triggerProductionChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,prodMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			if(t.getTrigger().isSatisfied(data,player)!=t.getInvert())
				change.add(ChangeFactory.changeProductionFrontier(player, t.getFrontier()));
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerTechChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,techMatch);
		for(TriggerAttachment t:trigs) {
			if(t.getTrigger().isSatisfied(data,player)!=t.getInvert()) {
				TechAdvance advance = TechAdvance.findAdvance(t.getTech(),data);
				Collection<TechAdvance> alreadyHave = TechTracker.getTechAdvances(player);
				if(alreadyHave.contains(advance))
					continue;
				aBridge.getHistoryWriter().startEvent(player.getName() + " activating " + advance);
				advance.perform(player, aBridge, data);
				TechTracker.addAdvance(player, data, aBridge, advance);
			}
		}
	}
	
	public static void triggerUnitPlacement(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,placeMatch);
		for(TriggerAttachment t:trigs) {
			if(t.getTrigger().isSatisfied(data,player)!=t.getInvert()) {
				for(Territory ter: t.getPlacement().keySet()) {
					placeUnits(ter,t.getPlacement().get(ter),player,data,aBridge);
				}
			}
		}
	}
	
	private static void placeUnits(Territory terr, IntegerMap<UnitType> uMap,PlayerID player,GameData data,IDelegateBridge aBridge){
		// createUnits
		List<Unit> units = new ArrayList<Unit>();;
		for(UnitType u: uMap.keySet()) {
			units.addAll(u.create(uMap.getInt(u), player));
		}
		// place units
		CompositeChange change = new CompositeChange();
        
        Collection<Unit> factoryAndAA = Match.getMatches(units,
                Matches.UnitIsAAOrFactory);
        change.add(DelegateFinder.battleDelegate(data).getOriginalOwnerTracker()
                .addOriginalOwnerChange(factoryAndAA, player));
       
        String transcriptText = MyFormatter.unitsToTextNoOwner(units)
                + " placed in " + terr.getName();
        aBridge.getHistoryWriter().startEvent(transcriptText);
        aBridge.getHistoryWriter().setRenderingData(units);

        Change place = ChangeFactory.addUnits(terr, units);
        change.add(place);
        
        if(Match.someMatch(units, Matches.UnitIsFactory))
        {
        	TerritoryAttachment ta = TerritoryAttachment.get(terr);
        	int prod = 0;
        	if( ta != null)
        		prod = ta.getProduction();
            Change unitProd = ChangeFactory.changeUnitProduction(terr, prod);
            change.add(unitProd);
        }

        aBridge.addChange(change);
        // handle adding to enemy territories
        if( Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(terr))
        	DelegateFinder.battleDelegate(data).getBattleTracker().addBattle(new CRoute(terr), units, false, player, data, aBridge, null);
	}
	private static Match<TriggerAttachment> prodMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getFrontier() != null;
		}
	};

	private static Match<TriggerAttachment> techMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getTech() != null;
		}
	};
	
	private static Match<TriggerAttachment> placeMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getPlacement() != null;
		}
	};
	
	// shameless cheating. making a fake route, so as to handle battles 
	// properly without breaking battleTracker protected status or duplicating 
	// a zillion lines of code.
	private static class CRoute extends Route {
		public CRoute(Territory terr) {
			super(terr);
		}
		public Territory getEnd()
	    {
	        return getStart();
	    }
		public int getLength()
	    {
	        return 1;
	    }
		public Territory at(int i)
	    {
	        return getStart();
	    }

	}
}
