package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.BattleRecord.BattleResultDescription;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * The Purpose of this class is to record various information about combat,
 * in order to use it for conditions and other things later.
 * 
 * @author Veqryn [Mark Christopher Duncan]
 * 
 */
public class BattleRecords extends GameDataComponent implements Serializable
{
	private static final long serialVersionUID = 1473664374777905497L;
	
	private final HashMap<PlayerID, HashMap<GUID, BattleRecord>> m_records = new HashMap<PlayerID, HashMap<GUID, BattleRecord>>();
	
	public BattleRecords(final GameData data)
	{
		super(data);
	}
	
	public BattleRecords(final BattleRecords records)
	{
		super(records.getData());
		for (final Entry<PlayerID, HashMap<GUID, BattleRecord>> entry : records.m_records.entrySet())
		{
			final PlayerID p = entry.getKey();
			final HashMap<GUID, BattleRecord> record = entry.getValue();
			final HashMap<GUID, BattleRecord> map = new HashMap<GUID, BattleRecord>();
			for (final Entry<GUID, BattleRecord> entry2 : record.entrySet())
			{
				map.put(entry2.getKey(), new BattleRecord(entry2.getValue()));
			}
			m_records.put(p, map);
		}
	}
	
	public static Collection<BattleRecord> getAllRecords(final BattleRecords brs)
	{
		final Collection<BattleRecord> records = new ArrayList<BattleRecord>();
		for (final HashMap<GUID, BattleRecord> playerMap : brs.m_records.values())
		{
			for (final BattleRecord r : playerMap.values())
			{
				records.add(r);
			}
		}
		return records;
	}
	
	public static Collection<BattleRecord> getRecordsForPlayerID(final PlayerID player, final BattleRecords brs)
	{
		final Collection<BattleRecord> playerRecords = new ArrayList<BattleRecord>();
		if (brs.m_records.get(player) == null)
			return playerRecords;
		for (final Entry<GUID, BattleRecord> entry : brs.m_records.get(player).entrySet())
		{
			playerRecords.add(entry.getValue());
		}
		return playerRecords;
	}
	
	public static Collection<BattleRecord> getRecordsForPlayers(final Collection<PlayerID> players, final BattleRecords brs)
	{
		final Collection<BattleRecord> playersRecords = new ArrayList<BattleRecord>();
		for (final PlayerID player : players)
		{
			if (brs.m_records.get(player) == null)
				continue;
			for (final Entry<GUID, BattleRecord> entry : brs.m_records.get(player).entrySet())
			{
				playersRecords.add(entry.getValue());
			}
		}
		return playersRecords;
	}
	
	public static int getLostTUVforBattleRecords(final Collection<BattleRecord> brs, final boolean attackerLostTUV, final boolean includeNullPlayer)
	{
		int totalLostTUV = 0;
		for (final BattleRecord br : brs)
		{
			if (!includeNullPlayer && (br.getDefender() == null || br.getAttacker() == null || br.getDefender().isNull() || br.getAttacker().isNull()))
				continue;
			if (attackerLostTUV)
				totalLostTUV += br.getAttackerLostTUV();
			else
				totalLostTUV += br.getDefenderLostTUV();
		}
		return totalLostTUV;
	}
	
	public static boolean getWereThereBattlesInTerritoriesMatching(final Collection<BattleRecord> brs, final PlayerID attacker, final PlayerID defender, final String battleType,
				final Collection<Territory> anyOfTheseTerritories)
	{
		for (final BattleRecord br : brs)
		{
			if (anyOfTheseTerritories.contains(br.getBattleSite()))
			{
				if (attacker != null && !attacker.equals(br.getAttacker()))
					continue;
				if (defender != null && !defender.equals(br.getDefender()))
					continue;
				if (!battleType.equalsIgnoreCase("any"))
					continue;
				return true;
				// TODO: do more types.... (maybe make a much better enum class that covers both WhoWon and BattleResultDescription in a single enum with multiple variables for each enum to cover the different tiers of detail (ie: won/lost/draw vs conquer/blitz/etc.)
			}
		}
		return false;
	}
	
	public void removeBattle(final PlayerID currentPlayer, final GUID battleID)
	{
		final HashMap<GUID, BattleRecord> current = m_records.get(currentPlayer);
		if (current == null)
			throw new IllegalStateException("Trying to remove info from battle records that do not exist");
		if (!current.containsKey(battleID))
			throw new IllegalStateException("Trying to remove a battle that does not exist");
		current.remove(battleID);
	}
	
	public void addRecord(final BattleRecords other)
	{
		for (final PlayerID p : m_records.keySet())
		{
			final HashMap<GUID, BattleRecord> otherRecord = other.m_records.get(p);
			if (otherRecord != null)
				throw new IllegalStateException("Should not be adding battle records for player " + p.getName() + " when they are already on the record");
		}
		for (final PlayerID p : other.m_records.keySet())
		{
			m_records.put(p, other.m_records.get(p));
		}
	}
	
	public void addBattle(final PlayerID currentPlayerAndAttacker, final GUID battleID, final Territory battleSite, final BattleType battleType, final GameData data)
	{
		HashMap<GUID, BattleRecord> current = m_records.get(currentPlayerAndAttacker);
		if (current == null)
			current = new HashMap<GUID, BattleRecord>();
		final BattleRecord initial = new BattleRecord(battleSite, currentPlayerAndAttacker, battleType, data);
		current.put(battleID, initial);
		m_records.put(currentPlayerAndAttacker, current);
	}
	
	public void addResultToBattle(final PlayerID currentPlayer, final GUID battleID, final PlayerID defender, final int attackerLostTUV, final int defenderLostTUV,
				final BattleResultDescription battleResultDescription, final BattleResults battleResults, final int bombingDamage)
	{
		final HashMap<GUID, BattleRecord> current = m_records.get(currentPlayer);
		if (current == null)
			throw new IllegalStateException("Trying to add info to battle records that do not exist");
		if (!current.containsKey(battleID))
			throw new IllegalStateException("Trying to add info to a battle that does not exist");
		final BattleRecord record = current.get(battleID);
		record.setResult(defender, attackerLostTUV, defenderLostTUV, battleResultDescription, battleResults, bombingDamage);
	}
	
	public void clear()
	{
		m_records.clear();
	}
	
	public boolean isEmpty()
	{
		return m_records.isEmpty();
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder("[");
		for (final Entry<PlayerID, HashMap<GUID, BattleRecord>> entry : m_records.entrySet())
		{
			sb.append(", ");
			sb.append(entry.getKey().getName());
			sb.append("={");
			final StringBuilder sb2 = new StringBuilder("");
			for (final Entry<GUID, BattleRecord> entry2 : entry.getValue().entrySet())
			{
				sb2.append(", ");
				final String guid = entry2.getKey().toString();
				sb2.append(guid.substring(Math.max(0, Math.min(guid.length(), 7 * guid.length() / 8)), guid.length()));
				sb2.append(":");
				sb2.append(entry2.getValue().toString());
			}
			sb.append(sb2.toString().replaceFirst(", ", ""));
			sb.append("}");
		}
		sb.append("]");
		return sb.toString().replaceFirst(", ", "");
	}
}
