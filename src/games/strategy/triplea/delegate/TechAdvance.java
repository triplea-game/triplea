/*
 * TechAdvance.java
 *
 * Created on November 25, 2001, 4:22 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
abstract class TechAdvance 
{
	private static List s_advances;

	public static final TechAdvance JET_POWER = new JetPowerAdvance();
	public static final TechAdvance SUPER_SUBS = new SuperSubsAdvance();
	public static final TechAdvance LONG_RANGE_AIRCRAFT = new LongRangeAircraftAdvance();
	public static final TechAdvance ROCKETS = new RocketsAdvance();
	public static final TechAdvance INDUSTRIAL_TECHNOLOGY = new IndustrialTechnologyAdvance();
	public static final TechAdvance HEAVY_BOMBER = new HeavyBomberAdvance();
	
	public static List getTechAdvances()
	{
		return s_advances;
	}
	
	//initialize the advances, note s_advances is made unmodifiable
	static {
		s_advances = new ArrayList();
		
		s_advances.add(JET_POWER);
		s_advances.add(SUPER_SUBS);
		s_advances.add(LONG_RANGE_AIRCRAFT);
		s_advances.add(ROCKETS);
		s_advances.add(INDUSTRIAL_TECHNOLOGY);
		s_advances.add(HEAVY_BOMBER);
		
		s_advances = Collections.unmodifiableList(s_advances);
	}
	
	public abstract String getName();
	public abstract void perform(PlayerID id, DelegateBridge bridge, GameData data);
}

class SuperSubsAdvance extends TechAdvance
{
	public String getName()
	{
		return "Super subs";
	}
	
	public void perform(PlayerID id, DelegateBridge bridge, GameData data)
	{}
}

class HeavyBomberAdvance extends TechAdvance
{
	public String getName()
	{
		return "Heavy Bomber";
	}
	
	public void perform(PlayerID id, DelegateBridge bridge, GameData data)
	{}
	
}

class IndustrialTechnologyAdvance extends TechAdvance
{
	public String getName()
	{
		return "Industrial Technology";
	}
	
	public void perform(PlayerID id, DelegateBridge bridge, GameData data)
	{
		ProductionFrontier advancedTech = data.getProductionFrontierList().getProductionFrontier("productionIndustrialTechnology");
		Change prodChange = ChangeFactory.changeProductionFrontier(id, advancedTech);
		bridge.addChange(prodChange);
	}
}

class JetPowerAdvance extends TechAdvance
{
	public String getName()
	{
		return "Jet Power";
	}
	
	public void perform(PlayerID id, DelegateBridge bridge, GameData data)
	{	}
	
}

class RocketsAdvance extends TechAdvance
{
	public String getName()
	{
		return "Rockets Advance";
	}
	
	public void perform(PlayerID id, DelegateBridge bridge, GameData data)
	{	}
	
}

class LongRangeAircraftAdvance extends TechAdvance
{
	public String getName()
	{
		return "Long Range Aircraft";
	}
	
	public void perform(PlayerID id, DelegateBridge bridge, GameData data)
	{}
}