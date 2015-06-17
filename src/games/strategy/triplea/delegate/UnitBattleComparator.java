package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;

public class UnitBattleComparator implements Comparator<Unit>
{
	private final boolean m_defending;
	private final IntegerMap<UnitType> m_costs;
	private final GameData m_data;
	private final boolean m_bonus;
	private final boolean m_ignorePrimaryPower;
	private final Collection<TerritoryEffect> m_territoryEffects;
	private final Collection<UnitType> m_multiHitpointCanRepair = new HashSet<UnitType>();
	
	public UnitBattleComparator(final boolean defending, final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean bonus,
				final boolean ignorePrimaryPower)
	{
		m_defending = defending;
		m_costs = costs;
		m_data = data;
		m_bonus = bonus;
		m_ignorePrimaryPower = ignorePrimaryPower;
		m_territoryEffects = territoryEffects;
		if (games.strategy.triplea.Properties.getBattleshipsRepairAtEndOfRound(data) || games.strategy.triplea.Properties.getBattleshipsRepairAtBeginningOfRound(data))
		{
			for (final UnitType ut : data.getUnitTypeList())
			{
				if (Matches.UnitTypeHasMoreThanOneHitPointTotal.match(ut))
				{
					m_multiHitpointCanRepair.add(ut);
				}
			}
			// TODO: check if there are units in the game that can repair this unit
		}
	}
	
	public int compare(final Unit u1, final Unit u2)
	{
		if (u1.equals(u2))
			return 0;
		final boolean transporting1 = Matches.transportIsTransporting().match(u1);
		final boolean transporting2 = Matches.transportIsTransporting().match(u2);
		final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
		final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
		if (ua1 == ua2 && u1.getOwner().equals(u2.getOwner()))
		{
			if (transporting1 && !transporting2)
				return 1;
			if (!transporting1 && transporting2)
				return -1;
			return 0;
		}
		final boolean airOrCarrierOrTransport1 = Matches.UnitIsAir.match(u1) || Matches.UnitIsCarrier.match(u1) || (transporting1 ? false : Matches.UnitIsTransport.match(u1));
		final boolean airOrCarrierOrTransport2 = Matches.UnitIsAir.match(u2) || Matches.UnitIsCarrier.match(u2) || (transporting2 ? false : Matches.UnitIsTransport.match(u2));
		final boolean subDestroyer1 = Matches.UnitIsSub.match(u1) || Matches.UnitIsDestroyer.match(u1);
		final boolean subDestroyer2 = Matches.UnitIsSub.match(u2) || Matches.UnitIsDestroyer.match(u2);
		final boolean multiHpCanRepair1 = m_multiHitpointCanRepair.contains(u1.getType());
		final boolean multiHpCanRepair2 = m_multiHitpointCanRepair.contains(u2.getType());
		if (!m_ignorePrimaryPower)
		{
			int power1 = 8 * BattleCalculator.getUnitPowerForSorting(u1, m_defending, m_data, m_territoryEffects);
			int power2 = 8 * BattleCalculator.getUnitPowerForSorting(u2, m_defending, m_data, m_territoryEffects);
			if (m_bonus)
			{
				if (subDestroyer1 && !subDestroyer2)
					power1 += 4;
				else if (!subDestroyer1 && subDestroyer2)
					power2 += 4;
				if (multiHpCanRepair1 && !multiHpCanRepair2)
					power1++;
				else if (!multiHpCanRepair1 && multiHpCanRepair2)
					power2++;
				if (transporting1 && !transporting2)
					power1++;
				else if (!transporting1 && transporting2)
					power2++;
				if (airOrCarrierOrTransport1 && !airOrCarrierOrTransport2)
					power1++;
				else if (!airOrCarrierOrTransport1 && airOrCarrierOrTransport2)
					power2++;
			}
			if (power1 != power2)
				return power1 - power2;
		}
		{
			final int cost1 = m_costs.getInt(u1.getType());
			final int cost2 = m_costs.getInt(u2.getType());
			if (cost1 != cost2)
				return cost1 - cost2;
		}
		{
			int power1reverse = 8 * BattleCalculator.getUnitPowerForSorting(u1, !m_defending, m_data, m_territoryEffects);
			int power2reverse = 8 * BattleCalculator.getUnitPowerForSorting(u2, !m_defending, m_data, m_territoryEffects);
			if (m_bonus)
			{
				if (subDestroyer1 && !subDestroyer2)
					power1reverse += 4;
				else if (!subDestroyer1 && subDestroyer2)
					power2reverse += 4;
				if (multiHpCanRepair1 && !multiHpCanRepair2)
					power1reverse++;
				else if (!multiHpCanRepair1 && multiHpCanRepair2)
					power2reverse++;
				if (transporting1 && !transporting2)
					power1reverse++;
				else if (!transporting1 && transporting2)
					power2reverse++;
				if (airOrCarrierOrTransport1 && !airOrCarrierOrTransport2)
					power1reverse++;
				else if (!airOrCarrierOrTransport1 && airOrCarrierOrTransport2)
					power2reverse++;
			}
			if (power1reverse != power2reverse)
				return power1reverse - power2reverse;
		}
		if (subDestroyer1 && !subDestroyer2)
			return 1;
		else if (!subDestroyer1 && subDestroyer2)
			return -1;
		if (multiHpCanRepair1 && !multiHpCanRepair2)
			return 1;
		else if (!multiHpCanRepair1 && multiHpCanRepair2)
			return -1;
		if (transporting1 && !transporting2)
			return 1;
		else if (!transporting1 && transporting2)
			return -1;
		if (airOrCarrierOrTransport1 && !airOrCarrierOrTransport2)
			return 1;
		else if (!airOrCarrierOrTransport1 && airOrCarrierOrTransport2)
			return -1;
		return ua1.getMovement(u1.getOwner()) - ua2.getMovement(u2.getOwner());
	}
}
