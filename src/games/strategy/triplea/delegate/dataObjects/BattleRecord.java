package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;

/**
 * The Purpose of this class is to record various information about combat,
 * in order to use it for conditions and other things later.
 * 
 * @author Veqryn [Mark Christopher Duncan]
 * 
 */
public class BattleRecord extends GameDataComponent
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
	
	private static final long serialVersionUID = 3642216371483289106L;
	private Territory m_battleSite;
	private PlayerID m_attacker;
	private PlayerID m_defender;
	private int m_attackerLostTUV = 0;
	private int m_defenderLostTUV = 0;
	private BattleResultDescription m_battleResultDescription;
	private int m_bombingDamage = 0;
	private BattleType m_battleType;
	private BattleResults m_battleResults;
	
	// Something in IBattle (formerly part of BattleResults) can not be Serialized, which can causing MAJOR problems. So the IBattle should never be part of BattleResults or BattleRecord.
	
	// Create copy
	protected BattleRecord(final BattleRecord record)
	{
		super(record.getData());
		m_battleSite = record.m_battleSite;
		m_attacker = record.m_attacker;
		m_defender = record.m_defender;
		m_attackerLostTUV = record.m_attackerLostTUV;
		m_defenderLostTUV = record.m_defenderLostTUV;
		m_battleResultDescription = record.m_battleResultDescription;
		m_bombingDamage = record.m_bombingDamage;
		m_battleType = record.m_battleType;
		m_battleResults = record.m_battleResults;
	}
	
	/*// Create full Record
	protected BattleRecord(final Territory battleSite, final PlayerID attacker, final PlayerID defender, final int attackerLostTUV, final int defenderLostTUV,
				final BattleResultDescription battleResultDescription, final BattleResults battleResults, final int bombingDamage, final BattleType battleType, final GameData data)
	{
		super(data);
		m_battleSite = battleSite;
		m_attacker = attacker;
		m_defender = defender;
		m_attackerLostTUV = attackerLostTUV;
		m_defenderLostTUV = defenderLostTUV;
		m_battleResultDescription = battleResultDescription;
		m_battleResults = battleResults;
		m_bombingDamage = bombingDamage;
		m_battleType = battleType;
	}*/

	protected BattleRecord(final Territory battleSite, final PlayerID attacker, final BattleType battleType, final GameData data)
	{
		super(data);
		m_battleSite = battleSite;
		m_attacker = attacker;
		m_battleType = battleType;
	}
	
	protected void setResult(final PlayerID defender, final int attackerLostTUV, final int defenderLostTUV,
				final BattleResultDescription battleResultDescription, final BattleResults battleResults, final int bombingDamage)
	{
		m_defender = defender;
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
	
	protected BattleResults getBattleResults()
	{
		return m_battleResults;
	}
	
	protected void setBattleResults(final BattleResults battleResults)
	{
		m_battleResults = battleResults;
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
