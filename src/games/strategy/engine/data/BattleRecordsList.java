package games.strategy.engine.data;

import games.strategy.triplea.delegate.dataObjects.BattleRecords;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A holder for BattleRecords
 * 
 * @author Veqryn
 * 
 */
public class BattleRecordsList extends GameDataComponent implements Serializable
{
	private static final long serialVersionUID = 7515693859612849475L;
	
	private Map<Integer, BattleRecords> m_battleRecords = new HashMap<Integer, BattleRecords>();
	
	public BattleRecordsList(final GameData data)
	{
		super(data);
	}
	
	public static void addRecords(final Map<Integer, BattleRecords> recordList, final Integer currentRound, final BattleRecords other)
	{
		final BattleRecords current = recordList.get(currentRound);
		if (current == null)
		{
			recordList.put(currentRound, other);
			return;
		}
		current.addRecord(other);
		recordList.put(currentRound, current);
	}
	
	public void setRecords(final Map<Integer, BattleRecords> recordList)
	{
		m_battleRecords = recordList;
	}
	
	public BattleRecords getCurrentRound()
	{
		return copyList(m_battleRecords).get(getData().getSequence().getRound());
	}
	
	public Map<Integer, BattleRecords> getBattleRecordsMap()
	{
		return copyList(m_battleRecords);
	}
	
	private static Map<Integer, BattleRecords> copyList(final Map<Integer, BattleRecords> records)
	{
		final Map<Integer, BattleRecords> copy = new HashMap<Integer, BattleRecords>();
		for (final Entry<Integer, BattleRecords> entry : records.entrySet())
		{
			copy.put(Integer.valueOf(entry.getKey()), new BattleRecords(entry.getValue()));
		}
		return copy;
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("BattleRecordsList:");
		for (final Entry<Integer, BattleRecords> entry : m_battleRecords.entrySet())
		{
			sb.append("\n");
			sb.append(entry.getKey().toString());
			sb.append(" -> ");
			sb.append(entry.getValue().toString());
			sb.append("");
		}
		return sb.toString();
	}
	
	// Interpretation stuff below
	public static int getTUVdamageCausedByPlayer(final PlayerID attacker, final BattleRecordsList brl, final int beginningRound, final int endRound,
				final boolean currentRoundOnly, final boolean includeNullPlayer)
	{
		int damageCausedByAttacker = 0;
		final Collection<BattleRecords> brs = new ArrayList<BattleRecords>();
		if (currentRoundOnly)
		{
			if (brl != null && brl.getCurrentRound() != null)
				brs.add(brl.getCurrentRound());
		}
		else
		{
			final Map<Integer, BattleRecords> currentList = brl.getBattleRecordsMap();
			for (int i = beginningRound; i > endRound; i++)
			{
				final BattleRecords currentRecords = currentList.get(i);
				if (currentRecords != null)
					brs.add(currentRecords);
			}
		}
		for (final BattleRecords br : brs)
		{
			damageCausedByAttacker += BattleRecords.getLostTUVforBattleRecords(BattleRecords.getRecordsForPlayerID(attacker, br), false, includeNullPlayer);
		}
		return damageCausedByAttacker;
	}
	
	/**
	 * Determines if there were any battles that match the following criteria:
	 * 
	 * @param attacker
	 *            if null then any player
	 * @param defender
	 *            if null then any player
	 * @param battleType
	 * @param anyOfTheseTerritories
	 * @param brl
	 * @param beginningRound
	 * @param endRound
	 * @param currentRoundOnly
	 * @return
	 */
	public static boolean getWereThereBattlesInTerritoriesMatching(final PlayerID attacker, final PlayerID defender, final String battleType, final Collection<Territory> anyOfTheseTerritories,
				final BattleRecordsList brl, final int beginningRound, final int endRound, final boolean currentRoundOnly)
	{
		final Collection<BattleRecords> brs = new ArrayList<BattleRecords>();
		if (currentRoundOnly)
		{
			if (brl != null && brl.getCurrentRound() != null)
				brs.add(brl.getCurrentRound());
		}
		else
		{
			final Map<Integer, BattleRecords> currentList = brl.getBattleRecordsMap();
			for (int i = beginningRound; i > endRound; i++)
			{
				final BattleRecords currentRecords = currentList.get(i);
				if (currentRecords != null)
					brs.add(currentRecords);
			}
		}
		// null for attacker means any attacker
		for (final BattleRecords br : brs)
		{
			if (BattleRecords.getWereThereBattlesInTerritoriesMatching((attacker == null ? BattleRecords.getAllRecords(br) : BattleRecords.getRecordsForPlayerID(attacker, br)), attacker, defender,
						battleType, anyOfTheseTerritories))
				return true;
		}
		return false;
	}
}
