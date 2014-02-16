package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;

import java.util.Collection;
import java.util.Comparator;

public class UnitBattleComparator implements Comparator<Unit>
{
	private final boolean m_defending;
	private final IntegerMap<UnitType> m_costs;
	private final GameData m_data;
	private final boolean m_bonus;
	private final Collection<TerritoryEffect> m_territoryEffects;
	
	public UnitBattleComparator(final boolean defending, final IntegerMap<UnitType> costs, final Collection<TerritoryEffect> territoryEffects, final GameData data, final boolean bonus)
	{
		m_defending = defending;
		m_costs = costs;
		m_data = data;
		m_bonus = bonus;
		m_territoryEffects = territoryEffects;
	}
	
	public int compare(final Unit u1, final Unit u2)
	{
		if (u1.equals(u2))
			return 0;
		final boolean transporting1 = Matches.transportIsTransporting().match(u1);
		final boolean transporting2 = Matches.transportIsTransporting().match(u2);
		final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
		final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
		if (ua1 == ua2)
		{
			if (transporting1 && !transporting2)
				return 1;
			if (!transporting1 && transporting2)
				return -1;
			return 0;
		}
		final boolean airOrCarrierOrTwoHitOrTransport1 = Matches.UnitIsAir.match(u1) || Matches.UnitHasMoreThanOneHitPointTotal.match(u1) || Matches.UnitIsCarrier.match(u1)
					|| (transporting1 ? false : Matches.UnitIsTransport.match(u1));
		final boolean airOrCarrierOrTwoHitOrTransport2 = Matches.UnitIsAir.match(u2) || Matches.UnitHasMoreThanOneHitPointTotal.match(u2) || Matches.UnitIsCarrier.match(u2)
					|| (transporting2 ? false : Matches.UnitIsTransport.match(u2));
		final boolean subDestroyer1 = Matches.UnitIsSub.match(u1) || Matches.UnitIsDestroyer.match(u1);
		final boolean subDestroyer2 = Matches.UnitIsSub.match(u2) || Matches.UnitIsDestroyer.match(u2);
		{
			int power1 = 5 * BattleCalculator.getUnitPowerForSorting(u1, m_defending, m_data, m_territoryEffects);
			int power2 = 5 * BattleCalculator.getUnitPowerForSorting(u2, m_defending, m_data, m_territoryEffects);
			if (m_bonus)
			{
				if (subDestroyer1 && !subDestroyer2)
					power1 += 3;
				else if (!subDestroyer1 && subDestroyer2)
					power2 += 3;
				if (transporting1 && !transporting2)
					power1++;
				else if (!transporting1 && transporting2)
					power2++;
				if (airOrCarrierOrTwoHitOrTransport1 && !airOrCarrierOrTwoHitOrTransport2)
					power1++;
				else if (!airOrCarrierOrTwoHitOrTransport1 && airOrCarrierOrTwoHitOrTransport2)
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
			int power1reverse = 5 * BattleCalculator.getUnitPowerForSorting(u1, !m_defending, m_data, m_territoryEffects);
			int power2reverse = 5 * BattleCalculator.getUnitPowerForSorting(u2, !m_defending, m_data, m_territoryEffects);
			if (m_bonus)
			{
				if (subDestroyer1 && !subDestroyer2)
					power1reverse += 3;
				else if (!subDestroyer1 && subDestroyer2)
					power2reverse += 3;
				if (transporting1 && !transporting2)
					power1reverse++;
				else if (!transporting1 && transporting2)
					power2reverse++;
				if (airOrCarrierOrTwoHitOrTransport1 && !airOrCarrierOrTwoHitOrTransport2)
					power1reverse++;
				else if (!airOrCarrierOrTwoHitOrTransport1 && airOrCarrierOrTwoHitOrTransport2)
					power2reverse++;
			}
			if (power1reverse != power2reverse)
				return power1reverse - power2reverse;
		}
		if (transporting1 && !transporting2)
			return 1;
		else if (!transporting1 && transporting2)
			return -1;
		return 0;
	}
}
