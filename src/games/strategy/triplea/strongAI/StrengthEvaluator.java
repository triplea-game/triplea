package games.strategy.triplea.strongAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class StrengthEvaluator
{
	private float m_alliedNeighborStrength = 0.0F;
	private float m_alliedStrengthInRange = 0.0F;
	private float m_enemyNeighborStrength = 0.0F;
	private float m_enemyStrengthInRange = 0.0F;
	
	public float getAlliedNeighborStrength()
	{
		return (m_alliedNeighborStrength);
	}
	
	public float getAlliedStrengthInRange()
	{
		return (m_alliedStrengthInRange);
	}
	
	public float getEnemyNeighborStrength()
	{
		return (m_enemyNeighborStrength);
	}
	
	public float getEnemyStrengthInRange()
	{
		return (m_enemyStrengthInRange);
	}
	
	public boolean inDanger(final float dangerFactor)
	{
		return (strengthMissing(dangerFactor) <= 0);
	}
	
	public float strengthMissing(final float dangerFactor)
	{
		return (m_enemyStrengthInRange - (m_alliedStrengthInRange * dangerFactor - 3.00F));
	}
	
	// gives our sea Strength within one and two territories of ourTerr
	// defensive strength
	// allied determines whether allied or enemy evaluation
	public void evalStrength(final GameData data, final PlayerID player, final Territory ourTerr, final boolean sea, final boolean contiguous, final boolean tFirst, final boolean includeAllies,
				final boolean allied)
	{
		Collection<Unit> seaUnits = new ArrayList<Unit>();
		Collection<Unit> airUnits = new ArrayList<Unit>();
		Collection<Unit> landUnits = new ArrayList<Unit>();
		int rDist = 0, r = 2;
		float inRangeStrength = 0.0F, thisStrength = 0.0F;
		float inRangeAirStrength = 0.0F, thisAirStrength = 0.0F;
		if (!ourTerr.isWater() && sea)
			r = 3; // if we have a land terr and looking at sea...look 3 out rather than 2
		final List<Territory> nearNeighbors = new ArrayList<Territory>();
		final Set<Territory> nN = data.getMap().getNeighbors(ourTerr, r);
		nearNeighbors.addAll(nN);
		if (ourTerr.isWater() == sea)
			nearNeighbors.add(ourTerr);
		final CompositeMatch<Unit> owned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsSea);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsAir);
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsLand);
		if (includeAllies)
		{
			seaUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea);
			airUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
			landUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsLand);
		}
		if (!allied)
		{
			seaUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
			airUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsAir);
			landUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsLand);
			m_enemyNeighborStrength = 0.0F;
			m_enemyStrengthInRange = 0.0F;
		}
		else
		{
			m_alliedNeighborStrength = 0.0F;
			m_alliedStrengthInRange = 0.0F;
		}
		for (final Territory t : nearNeighbors)
		{
			final boolean isLand = Matches.TerritoryIsLand.match(t);
			if (contiguous)
			{
				if (!isLand && sea)
				{ // don't count anything in a transport
					rDist = data.getMap().getWaterDistance(ourTerr, t);
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					final float seaStrength = SUtils.strength(seaUnits, false, true, tFirst);
					final float airStrength = SUtils.allairstrength(airUnits, false);
					if (rDist == 0 || rDist == 1)
					{
						thisStrength += seaStrength;
						thisAirStrength += airStrength;
					}
					if (rDist >= 0 && rDist <= 3)
					{
						inRangeStrength += seaStrength;
						inRangeAirStrength += airStrength;
					}
				}
				else if (isLand && !sea && !Matches.TerritoryIsNotNeutral.match(t))
				{
					rDist = data.getMap().getLandDistance(ourTerr, t);
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					final float xLandStrength = SUtils.strength(landUnits, false, false, tFirst);
					final float airStrength = SUtils.allairstrength(airUnits, false);
					if (rDist == 0 || rDist == 1)
					{
						thisStrength += xLandStrength;
						thisAirStrength += airStrength;
					}
					if (rDist >= 0 && rDist <= 3)
					{
						inRangeStrength += xLandStrength;
						inRangeAirStrength += airStrength;
					}
				}
				else
					continue;
			}
			else
			{
				rDist = data.getMap().getDistance(ourTerr, t);
				if (!isLand && sea)
				{ // don't count anything in a transport
					seaUnits = t.getUnits().getMatches(seaUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					final float seaStrength = SUtils.strength(seaUnits, false, true, tFirst);
					final float airStrength = SUtils.allairstrength(airUnits, false);
					if (rDist == 0 || rDist == 1)
					{
						thisStrength += seaStrength;
						thisAirStrength += airStrength;
					}
					if (rDist >= 0 && rDist <= 3)
					{
						inRangeStrength += seaStrength;
						inRangeAirStrength += airStrength;
					}
				}
				else if (isLand && !sea && Matches.TerritoryIsNotNeutral.match(t))
				{
					landUnits = t.getUnits().getMatches(landUnit);
					airUnits = t.getUnits().getMatches(airUnit);
					final float xLandStrength = SUtils.strength(landUnits, false, false, tFirst);
					final float airStrength = SUtils.allairstrength(airUnits, false);
					if (rDist == 0 || rDist == 1)
					{
						thisStrength += xLandStrength;
						thisAirStrength += airStrength;
					}
					if (rDist >= 0 && rDist <= 2) // try maxing out at 2 for land
					{
						inRangeStrength += xLandStrength;
						inRangeAirStrength += airStrength;
					}
				}
			}
			thisStrength = 0.0F;
			rDist = 0;
		}
		if (Matches.TerritoryIsLand.match(ourTerr) && thisStrength > 0.0F)
		{// ignore air strength if there are no land units
			if (allied)
			{
				m_alliedNeighborStrength += thisStrength + thisAirStrength;
				m_alliedStrengthInRange += inRangeStrength + inRangeAirStrength;
			}
			else
			{
				m_enemyNeighborStrength += thisStrength + thisAirStrength;
				m_enemyStrengthInRange += inRangeStrength + inRangeAirStrength;
			}
		}
		else
		{
			if (allied)
			{
				m_alliedNeighborStrength += thisStrength + thisAirStrength;
				m_alliedStrengthInRange += inRangeStrength + inRangeAirStrength;
			}
			else
			{
				m_enemyNeighborStrength += thisStrength + thisAirStrength;
				m_enemyStrengthInRange += inRangeStrength + inRangeAirStrength;
			}
		}
	}
	
	/*
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


			for (Territory t: nearNeighbors)
			{
				if (Matches.TerritoryIsLand.match(t) && (Matches.territoryHasEnemyUnits(player, data).invert().match(t) || Matches.TerritoryIsNeutral.match(t)))
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
	*/
	public static StrengthEvaluator evalStrengthAt(final GameData data, final PlayerID player, final Territory ourTerr, final boolean sea, final boolean contiguous, final boolean tFirst,
				final boolean includeAllies)
	{
		final StrengthEvaluator strEval = new StrengthEvaluator();
		strEval.evalStrength(data, player, ourTerr, sea, contiguous, tFirst, includeAllies, true);
		strEval.evalStrength(data, player, ourTerr, sea, contiguous, tFirst, false, false);
		return (strEval);
	}
}
