package games.strategy.triplea.ai.AdvancedAI;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class AITurn
{
	int turn;
	private Collection<Territory> landingTerrs = new ArrayList<Territory>();
	private HashMap<Territory, Integer> targetTerr;
	private HashMap<UnitType, Integer> purchaseScheme;
	private HashMap<UnitType, HashMap<Territory, Integer>> placeScheme;
	
	public AITurn(final int aTurn)
	{
		turn = aTurn;
	}
	
	public void setLandingTerrs(final Collection<Territory> newTerrs)
	{
		landingTerrs = newTerrs;
	}
	
	public void setTargetTerr(final HashMap<Territory, Integer> newHash)
	{
		targetTerr = newHash;
	}
	
	public void setPurchaseScheme(final HashMap<UnitType, Integer> newHash)
	{
		purchaseScheme = newHash;
	}
	
	public void setPlaceScheme(final HashMap<UnitType, HashMap<Territory, Integer>> newHash)
	{
		placeScheme = newHash;
	}
	
	public Collection<Territory> getLandingTerrs()
	{
		return landingTerrs;
	}
	
	public HashMap<Territory, Integer> getTargetTerr()
	{
		return targetTerr;
	}
	
	public HashMap<UnitType, Integer> getPurchaseScheme()
	{
		return purchaseScheme;
	}
	
	public HashMap<UnitType, HashMap<Territory, Integer>> getPlaceScheme()
	{
		return placeScheme;
	}
}
