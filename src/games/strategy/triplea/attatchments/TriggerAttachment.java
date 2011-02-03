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
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.TripleAUnit;
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
	
	private List<RulesAttachment> m_trigger = null;
	private ProductionFrontier m_frontier = null;
	private boolean m_invert = false;
	private String m_tech = null;
	private Map<Territory,IntegerMap<UnitType>> m_placement = null;
	private IntegerMap<UnitType> m_purchase = null;
	private String m_resource = null;
	private int m_resourceCount = 0;
	private int m_uses = -1;
	private PlayerID m_player= null;
	private UnitSupportAttachment m_support = null;
	private List<String> m_unitProperty = null;
	private UnitType m_unitType = null;
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
	public void setTrigger(String triggers) throws GameParseException{
		String[] s = triggers.split(":");
		for(int i = 0;i<s.length;i++) {
			RulesAttachment trigger = null;
			for(PlayerID p:getData().getPlayerList().getPlayers()){
				trigger = (RulesAttachment) p.getAttachment(s[i]);
				if( trigger != null)
					break;
			}
			if(trigger == null)
				throw new GameParseException("Could not find rule. name:" + s);
			if(m_trigger == null)
				m_trigger = new ArrayList<RulesAttachment>();
			m_trigger.add(trigger);
		}
	}
	
	public List<RulesAttachment> getTrigger() {
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
	
	public int getResourceCount() {
		return m_resourceCount;
	}
	public void setResourceCount(String s) {
		m_resourceCount = getInt(s);
	}

	public void setUses(String s) {
		m_uses = getInt(s);
	}
	public void setUses(int u) {
		m_uses = u;
	}
	
	public int getUses() {
		return m_uses;
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
	
	public UnitType getUnitType() {
		return m_unitType;
	}
	public void setUnitType(String name) throws GameParseException
    {
            UnitType type = getData().getUnitTypeList().getUnitType(name);
            if(type == null)
                throw new GameParseException("Could not find unitType. name:" + name);
            m_unitType = type;
    	
    }
	public UnitSupportAttachment getSupport() {
		return m_support;
	}
	
	public void setSupport(String s) throws GameParseException{
		
		for(UnitSupportAttachment support:UnitSupportAttachment.get(getData())) {
			if( support.getName().equals(s)) {
				m_support = support;
				break;
			}
		}
		if(m_support == null)
			throw new GameParseException("Could not find unitSupportAttachment. name:" + s);
	}
	public void setPlayer(String name) throws GameParseException
    {
            
            PlayerID player = getData().getPlayerList().getPlayerID(name);
            if(player == null)
                throw new GameParseException("Could not find player. name:" + name);
            m_player = player;
    }
	public PlayerID getPlayer() {
    	return (PlayerID) (m_player==null?getAttatchedTo():m_player);
    }
	public String getResource() {
		return m_resource;
	}
	
	public void setResource(String s) throws GameParseException{
		Resource r = getData().getResourceList().getResource(s);
		if( r == null )
			throw new GameParseException( "Invalid resource: " +s);
		else
			m_resource = s;
	}
	
	public List<String> getUnitProperty() {
		return m_unitProperty;
	}
	
	// add not set
	public void setUnitProperty(String prop) throws GameParseException{
		String[] s = prop.split(":");
		if(s.length!=2) 
    		throw new GameParseException( "Invalid unitProperty declaration: " +prop);
		if(m_unitProperty== null)
			m_unitProperty = new ArrayList<String>();
		prop = s[1]+":"+s[0];
		m_unitProperty.add(prop);
		
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
	
	public void setPurchase(String place) throws GameParseException{
		String[] s = place.split(":");
    	int count = -1,i=0;
    	if(s.length<1)
    		throw new GameParseException( "Empty purchase list");
    	try {
    		count = getInt(s[0]);
    		i++;
    	} catch(Exception e) {
    		count = 1;
    	}
    	if(s.length<1 || s.length ==1 && count != -1)
    		throw new GameParseException( "Empty purchase list");
    	else {
    		if(m_purchase == null ) 
    			m_purchase = new IntegerMap<UnitType>();
    		for( ; i < s.length; i++){
    			UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
    			if(type == null)
    				throw new GameParseException( "UnitType does not exist " + s[i]);
    			else
    				m_purchase.add(type, count);
    		}	
    	}


	}
	public IntegerMap<UnitType> getPurchase() {
		return m_purchase;
	}
	public static void triggerProductionChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,prodMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = false;
			for(RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data);
				if(!met)
					break;
			}
			if(met!=t.getInvert())
				change.add(ChangeFactory.changeProductionFrontier(t.getPlayer(), t.getFrontier()));
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerTechChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,techMatch);
		for(TriggerAttachment t:trigs) {
			boolean met = false;
			for(RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data);
				if(!met)
					break;
			}
			if(met!=t.getInvert()) {
				TechAdvance advance = TechAdvance.findAdvance(t.getTech(),data);
				Collection<TechAdvance> alreadyHave = TechTracker.getTechAdvances(t.getPlayer());
				if(alreadyHave.contains(advance))
					continue;
				aBridge.getHistoryWriter().startEvent(t.getPlayer().getName() + " activating " + advance);
				advance.perform(t.getPlayer(), aBridge, data);
				TechTracker.addAdvance(t.getPlayer(), data, aBridge, advance);
			}
		}
	}
	
	public static void triggerUnitPlacement(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,placeMatch);
		for(TriggerAttachment t:trigs) {
			boolean met = false;
			for(RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data);
				if(!met)
					break;
			}
			if(met!=t.getInvert()) {
				for(Territory ter: t.getPlacement().keySet()) {
					placeUnits(ter,t.getPlacement().get(ter),t.getPlayer(),data,aBridge);
				}
			}
		}
	}
	
	public static void triggerPurchase(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,purchaseMatch);
		for(TriggerAttachment t:trigs) {
			List<Unit> units = new ArrayList<Unit>();
			boolean met = false;
			for(RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data);
				if(!met)
					break;
			}
			if(met!=t.getInvert()) {
				for(UnitType u: t.getPurchase().keySet()) {
					units.addAll(u.create(t.getPurchase().getInt(u), t.getPlayer()));
				}
			}	
			if(!units.isEmpty()) {
				String transcriptText = MyFormatter.unitsToTextNoOwner(units)
				+ " gained by " + t.getPlayer();
				aBridge.getHistoryWriter().startEvent(transcriptText);
				aBridge.getHistoryWriter().setRenderingData(units);
				Change place = ChangeFactory.addUnits(t.getPlayer(), units);
				aBridge.addChange(place);
			}
		}
	}
	
	public static void triggerResourceChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,resourceMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = false;
			int uses = t.getUses();
			if(uses == 0)
				continue;
			for(RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data);
				if(!met)
					break;
			}
			if(met!=t.getInvert()){
				int toAdd = t.getResourceCount();
				int total = t.getPlayer().getResources().getQuantity(t.getResource()) + toAdd;
    		    if(total < 0) {
    		    	toAdd -= total;
    		    	total = 0;
    		    }
    		    change.add(ChangeFactory.changeResourcesChange(t.getPlayer(), data.getResourceList().getResource(t.getResource()), toAdd));
        	    if( uses > 0) {
        	    	uses--;
        	    	change.add(ChangeFactory.attachmentPropertyChange(t, new Integer(uses).toString(), "uses"));
        	    }
    			String PUMessage = t.getPlayer().getName() + " met a national objective for an additional " + t.getResourceCount() + " " + t.getResource()+
    			"; end with " + total + " " +t.getResource();
    			aBridge.getHistoryWriter().startEvent(PUMessage);
			}
				
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	
	// note this change is silent
	public static void triggerSupportChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,supportMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = false;
			for(RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data);
				if(!met)
					break;
			}
			if(met!=t.getInvert()){
				if(t.getSupport().getPlayers().contains(t.getPlayer()))
					continue;
				List<PlayerID> p = new ArrayList<PlayerID>(t.getSupport().getPlayers());
				p.add(t.getPlayer());
				change.add(ChangeFactory.attachmentPropertyChange(t.getSupport(), p, "players"));
			}	
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerUnitPropertyChange(PlayerID player, IDelegateBridge aBridge, GameData data) {
		Set<TriggerAttachment> trigs = getTriggers(player,data,unitPropertyMatch);
		CompositeChange change = new CompositeChange();
		for(TriggerAttachment t:trigs) {
			boolean met = false;
			for(RulesAttachment c:t.getTrigger()) {
				met = c.isSatisfied(data);
				if(!met)
					break;
			}
			if(met!=t.getInvert()){
				for(String property:t.getUnitProperty()) {
					String[] s = property.split(":");
					if(UnitAttachment.get(t.getUnitType()).getRawProperty(s[0]).equals(s[1]))
						continue;
					change.add(ChangeFactory.attachmentPropertyChange(UnitAttachment.get(t.getUnitType()), property, "rawProperty"));
					aBridge.getHistoryWriter().startEvent("Setting " + s[0]+ " to " + s[1] + " for " + t.getUnitType().getName());
				}
			}	
		}
		if( !change.isEmpty())
			aBridge.addChange(change);
	}
	private static void placeUnits(Territory terr, IntegerMap<UnitType> uMap,PlayerID player,GameData data,IDelegateBridge aBridge){
		// createUnits
		List<Unit> units = new ArrayList<Unit>();;
		for(UnitType u: uMap.keySet()) {
			units.addAll(u.create(uMap.getInt(u), player));
		}
		CompositeChange change = new CompositeChange();
		// mark no movement
		for(Unit unit:units){
			UnitAttachment ua = UnitAttachment.get(unit.getType());
	        change.add(ChangeFactory.unitPropertyChange(unit, ua.getMovement(unit.getOwner()), TripleAUnit.ALREADY_MOVED));
		}
		// place units
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
	
	private static Match<TriggerAttachment> purchaseMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getPurchase() != null;
		}
	};
	
	private static Match<TriggerAttachment> resourceMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getResource() != null && t.getResourceCount() != 0;
		}
	};
	
	private static Match<TriggerAttachment> supportMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getSupport() != null;
		}
	};
	
	private static Match<TriggerAttachment> unitPropertyMatch = new Match<TriggerAttachment>()
	{
		public boolean match(TriggerAttachment t)
		{
			return t.getUnitType() != null && t.getUnitProperty() !=null;
		}
	};
	public void validate(GameData data) throws GameParseException
	{
		if( m_trigger==null)
		throw new GameParseException("Invalid Unit attatchment" + this);
	}
	  
	// shameless cheating. making a fake route, so as to handle battles 
	// properly without breaking battleTracker protected status or duplicating 
	// a zillion lines of code.
	private static class CRoute extends Route {
		private static final long serialVersionUID = -4571007882522107666L;
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
