package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;

import java.util.Collection;

/**
 * Interface to ensure different implementations of the odds calculator all have the same public methods.
 * 
 * @author veqryn
 * 
 */
public interface IOddsCalculator
{
	public void setGameData(final GameData data);
	
	public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location, final Collection<Unit> attacking, final Collection<Unit> defending,
				final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount);
	
	public AggregateResults calculate();
	
	public AggregateResults setCalculateDataAndCalculate(final PlayerID attacker, final PlayerID defender, final Territory location, final Collection<Unit> attacking,
				final Collection<Unit> defending, final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount);
	
	public int getRunCount();
	
	public boolean getIsReady();
	
	public void setKeepOneAttackingLandUnit(final boolean bool);
	
	public void setAmphibious(final boolean bool);
	
	public void setRetreatAfterRound(final int value);
	
	public void setRetreatAfterXUnitsLeft(final int value);
	
	public void setRetreatWhenOnlyAirLeft(final boolean value);
	
	public void setRetreatWhenMetaPowerIsLower(final boolean value);
	
	public void setAttackerOrderOfLosses(final String attackerOrderOfLosses);
	
	public void setDefenderOrderOfLosses(final String defenderOrderOfLosses);
	
	public void cancel();
	
	public void shutdown();
	
	public int getThreadCount();
	
	public void addOddsCalculatorListener(final OddsCalculatorListener listener);
	
	public void removeOddsCalculatorListener(final OddsCalculatorListener listener);
	
}
