package games.strategy.triplea.strongAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.CanalAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.baseAI.AbstractAI;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.logging.Logger;


public class StrengthEvaluator 
{
	private float m_alliedNeighborStrength = 0.0F;
	private float m_alliedStrengthInRange = 0.0F;
	private float m_enemyNeighborStrength = 0.0F;
	private float m_enemyStrengthInRange = 0.0F;
	
	public float getAlliedNeighborStrength()
	{
		return(m_alliedNeighborStrength);
	}
	
	public float getAlliedStrengthInRange()
	{
		return(m_alliedStrengthInRange);
	}
	
	public float getEnemyNeighborStrength()
	{
		return(m_enemyNeighborStrength);
	}
	
	public float getEnemyStrengthInRange()
	{
		return(m_enemyStrengthInRange);
	}
	
	public boolean inDanger(float dangerFactor)
	{
		return(strengthMissing(dangerFactor) <= 0);
	}

	public float strengthMissing(float dangerFactor)
	{
		return(m_enemyStrengthInRange - (m_alliedStrengthInRange*dangerFactor - 3.00F));
	}

	//gives our sea Strength within one and two territories of ourTerr
	//defensive strength
    public void evalAlliedStrengthAt(GameData data, PlayerID player, Territory ourTerr,
    		boolean sea, boolean contiguous, boolean tFirst, boolean includeAllies)
    {
		Collection<Unit> seaUnits = new ArrayList<Unit>();
		Collection<Unit> airUnits = new ArrayList<Unit>();
		Collection<Unit> landUnits = new ArrayList<Unit>();
		int rDist=0, r=2;
		float thisStrength = 0.0F;

		if (!ourTerr.isWater() && sea)
		    r=3; //if we have a land terr and looking at sea...look 3 out rather than 2
		List<Territory> nearNeighbors = new ArrayList<Territory>();
		Set <Territory> nN = data.getMap().getNeighbors(ourTerr, r);
		nearNeighbors.addAll(nN);
		if (ourTerr.isWater() == sea)
		    nearNeighbors.add(ourTerr);
		CompositeMatch<Unit> owned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));

		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsSea);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsAir);
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsLand);
		if (includeAllies)
		{
			seaUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea);
			airUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
			landUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsLand);
		}

		m_alliedNeighborStrength = 0.0F;
		m_alliedStrengthInRange = 0.0F;

		for (Territory t: nearNeighbors)
		{
			if (contiguous)
			{
				if (t.isWater() && sea)
				{ //don't count anything in a transport
					rDist = data.getMap().getWaterDistance(ourTerr, t);
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(seaUnits, false, true, tFirst) + SUtils.allairstrength(airUnits, false);
				}
				else if (!t.isWater() && !sea)
				{
					rDist = data.getMap().getLandDistance(ourTerr, t);
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(landUnits, false, false, tFirst) + SUtils.allairstrength(airUnits, false);
				}
				else
					continue;
			}
			else
			{
				rDist = data.getMap().getDistance(ourTerr, t);
				if (t.isWater() && sea)
				{ //don't count anything in a transport
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(seaUnits, false, true, tFirst) + SUtils.allairstrength(airUnits, false);
				}
				else if (!t.isWater())
				{
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(landUnits, false, false, tFirst) + SUtils.allairstrength(airUnits, false);
				}
			}
			if (rDist == 0 || rDist == 1)
				m_alliedNeighborStrength += thisStrength;
			if (rDist >= 0 && rDist <=3)
				m_alliedStrengthInRange += thisStrength;
			thisStrength = 0.0F;
			rDist = 0;
		}
	}

    //gives enemy Strength within one and three zones of territory if sea, one and two if land
	//attack strength
	//sea is true if this is a sea check
	public void evalEnemyStrengthAt(GameData data, PlayerID player, Territory ourTerr,
				boolean sea, boolean contiguous, boolean tFirst)
	{
		Collection<Unit> seaUnits = new ArrayList<Unit>();
		int rDist=0;
		Collection<Unit> airUnits = new ArrayList<Unit>();
		Collection<Unit> landUnits = new ArrayList<Unit>();
		float thisStrength = 0.0F;
		int sDist = 3;
		if (!sea)
			sDist = 2;

		Collection <Territory> nearNeighbors = data.getMap().getNeighbors(ourTerr, sDist);

		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data),	Matches.UnitIsAir);
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsLand);
		m_enemyNeighborStrength = 0.0F;
		m_enemyStrengthInRange = 0.0F;

		for (Territory t: nearNeighbors)
		{
			if (!Matches.territoryHasEnemyUnits(player, data).match(t) && !t.isWater())
				continue;
			if (contiguous)
			{
				if (t.isWater() && sea)
				{
					rDist = data.getMap().getWaterDistance(ourTerr, t);
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(seaUnits, true, true, tFirst) + SUtils.allairstrength(airUnits, true);
				}
				else if (!t.isWater() && !sea)
				{
					rDist = data.getMap().getLandDistance(ourTerr, t);
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(landUnits, true, false, tFirst) + SUtils.allairstrength(airUnits, true);
				}
				else
					continue;
			}
			else
			{
				rDist = data.getMap().getDistance(ourTerr, t);
				if (t.isWater() && sea)
				{
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(seaUnits, true, true, tFirst) + SUtils.allairstrength(airUnits, true);
				}
				else if (!t.isWater())
				{
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					thisStrength = SUtils.strength(landUnits, true, false, tFirst) + SUtils.allairstrength(airUnits, true);
				}
			}

			if (rDist == 0 || rDist == 1)
				m_enemyNeighborStrength += thisStrength;
			if (rDist >= 0 && rDist <=2)
				m_enemyStrengthInRange += thisStrength;
			thisStrength = 0.0F;
			rDist = 0;
		}
	}
	
    public static StrengthEvaluator evalStrengthAt(GameData data, PlayerID player, Territory ourTerr,
    		boolean sea, boolean contiguous, boolean tFirst, boolean includeAllies)
    {
    	StrengthEvaluator strEval = new StrengthEvaluator();

    	strEval.evalAlliedStrengthAt(data, player, ourTerr,	sea, contiguous, tFirst, includeAllies);
    	strEval.evalEnemyStrengthAt(data, player, ourTerr,	sea, contiguous, tFirst);
    	
		return(strEval);
    }
}
