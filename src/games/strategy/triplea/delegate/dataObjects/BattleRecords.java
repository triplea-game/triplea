package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.BattleRecords.BattleResultDescription;
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
public class BattleRecords implements Serializable
{
	/**
	 * BLITZED = conquered without a fight <br>
	 * CONQUERED = fought, won, and took over territory if land or convoy <br>
	 * WON_WITHOUT_CONQUERING = fought, won, did not take over territory (could be water, or could be air attackers) <br>
	 * WON_WITH_ENEMY_LEFT = fought, enemy either submerged or the battle is over with our objectives successful even though enemies are left <br>
	 * STALEMATE = have units left in the territory beside enemy defenders (like both sides have transports left) <br>
	 * LOST = either lost the battle, or retreated <br>
	 * BOMBED = Successfully bombed something <br>
	 * AIR_BATTLE_WON = Won an Air Battle with units surviving <br>
	 * AIR_BATTLE_LOST = Lost an Air Battle with enemy units surviving <br>
	 * AIR_BATTLE_STALEMATE = Neither side has air units left <br>
	 * NO_BATTLE = No battle was fought, possibly because the territory you were about to bomb was conquered before the bombing could begin, etc.<br>
	 * 
	 * @author veqryn
	 * 
	 */
	public enum BattleResultDescription
	{
		BLITZED, CONQUERED, WON_WITHOUT_CONQUERING, WON_WITH_ENEMY_LEFT, STALEMATE, LOST, BOMBED, AIR_BATTLE_WON, AIR_BATTLE_LOST, AIR_BATTLE_STALEMATE, NO_BATTLE
	}
	
	private static final long serialVersionUID = 1473664374777905497L;
	
	private final HashMap<PlayerID, HashMap<GUID, BattleRecord>> m_records = new HashMap<PlayerID, HashMap<GUID, BattleRecord>>();
	
	public BattleRecords()
	{
	}
	
	public BattleRecords(final BattleRecords records)
	{
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
	
	public void addBattle(final PlayerID currentPlayerAndAttacker, final GUID battleID, final Territory battleSite, final BattleType battleType)
	{
		HashMap<GUID, BattleRecord> current = m_records.get(currentPlayerAndAttacker);
		if (current == null)
			current = new HashMap<GUID, BattleRecord>();
		final BattleRecord initial = new BattleRecord(battleSite, currentPlayerAndAttacker, battleType);
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


class BattleRecord implements Serializable
{
	private static final long serialVersionUID = 3642216371483289106L;
	private Territory m_battleSite;
	private PlayerID m_attacker;
	private PlayerID m_defender;
	// private IntegerMap<PlayerID> m_lostTUV;
	private int m_attackerLostTUV = 0;
	private int m_defenderLostTUV = 0;
	private BattleResultDescription m_battleResultDescription;
	private int m_bombingDamage = 0;
	private BattleType m_battleType;
	private BattleResults m_battleResults;
	
	protected BattleRecord(final BattleRecord record)
	{
		m_battleSite = record.m_battleSite;
		m_attacker = record.m_attacker;
		m_defender = record.m_defender;
		// m_lostTUV = new IntegerMap<PlayerID>(record.m_lostTUV);
		m_attackerLostTUV = record.m_attackerLostTUV;
		m_defenderLostTUV = record.m_defenderLostTUV;
		m_battleResultDescription = record.m_battleResultDescription;
		m_bombingDamage = record.m_bombingDamage;
		m_battleType = record.m_battleType;
		m_battleResults = record.m_battleResults;
	}
	
	protected BattleRecord(final Territory battleSite, final PlayerID attacker, final PlayerID defender, final int attackerLostTUV,
				final int defenderLostTUV, final BattleResultDescription battleResultDescription, final BattleResults battleResults, final int bombingDamage, final BattleType battleType)
	{
		m_battleSite = battleSite;
		m_attacker = attacker;
		m_defender = defender;
		// m_lostTUV = lostTUV;
		m_attackerLostTUV = attackerLostTUV;
		m_defenderLostTUV = defenderLostTUV;
		m_battleResultDescription = battleResultDescription;
		m_battleResults = battleResults;
		m_bombingDamage = bombingDamage;
		m_battleType = battleType;
	}
	
	protected BattleRecord(final Territory battleSite, final PlayerID attacker, final BattleType battleType)
	{
		m_battleSite = battleSite;
		m_attacker = attacker;
		m_battleType = battleType;
	}
	
	protected void setResult(final PlayerID defender, final int attackerLostTUV, final int defenderLostTUV,
				final BattleResultDescription battleResultDescription, final BattleResults battleResults, final int bombingDamage)
	{
		m_defender = defender;
		// m_lostTUV = lostTUV;
		m_attackerLostTUV = attackerLostTUV;
		m_defenderLostTUV = defenderLostTUV;
		m_battleResultDescription = battleResultDescription;
		m_battleResults = battleResults;
		m_bombingDamage = bombingDamage;
	}
	
	protected Territory getBattleSite()
	{
		return m_battleSite;
	}
	
	protected void setBattleSite(final Territory battleSite)
	{
		this.m_battleSite = battleSite;
	}
	
	protected PlayerID getAttacker()
	{
		return m_attacker;
	}
	
	protected void setAttacker(final PlayerID attacker)
	{
		this.m_attacker = attacker;
	}
	
	protected PlayerID getDefender()
	{
		return m_defender;
	}
	
	protected void setDefenders(final PlayerID defender)
	{
		this.m_defender = defender;
	}
	
	/*protected IntegerMap<PlayerID> getLostTUV()
	{
		return m_lostTUV;
	}
	
	protected void setLostTUV(final IntegerMap<PlayerID> lostTUV)
	{
		this.m_lostTUV = lostTUV;
	}*/

	protected int getAttackerLostTUV()
	{
		return m_attackerLostTUV;
	}
	
	protected void setAttackerLostTUV(final int attackerLostTUV)
	{
		this.m_attackerLostTUV = attackerLostTUV;
	}
	
	protected int getDefenderLostTUV()
	{
		return m_defenderLostTUV;
	}
	
	protected void setDefenderLostTUV(final int defenderLostTUV)
	{
		this.m_defenderLostTUV = defenderLostTUV;
	}
	
	protected BattleResultDescription getBattleResultDescription()
	{
		return m_battleResultDescription;
	}
	
	protected void setBattleResultDescription(final BattleResultDescription battleResult)
	{
		this.m_battleResultDescription = battleResult;
	}
	
	protected int getBombingDamage()
	{
		return m_bombingDamage;
	}
	
	protected void setBombingDamage(final int bombingDamage)
	{
		this.m_bombingDamage = bombingDamage;
	}
	
	protected BattleType getBattleType()
	{
		return m_battleType;
	}
	
	protected void setBattleType(final BattleType battleType)
	{
		this.m_battleType = battleType;
	}
	
	@Override
	public int hashCode()
	{
		return m_battleSite.hashCode();
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o == null || !(o instanceof BattleRecord))
			return false;
		final BattleRecord other = (BattleRecord) o;
		return other.m_battleSite.equals(this.m_battleSite) && other.m_battleType.equals(this.m_battleType)
					&& other.m_attacker.equals(this.m_attacker);
	}
	
	@Override
	public String toString()
	{
		return m_battleType + " battle in " + m_battleSite;
	}
}
