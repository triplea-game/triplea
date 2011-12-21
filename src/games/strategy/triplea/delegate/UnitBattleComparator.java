package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;

import java.util.Comparator;

public class UnitBattleComparator implements Comparator<Unit>
{
	private final boolean m_defending;
	private final IntegerMap<UnitType> m_costs;
	private final GameData m_data;
	private final boolean m_bonus;
	
	public UnitBattleComparator(final boolean defending, final IntegerMap<UnitType> costs, final GameData data, final boolean bonus)
	{
		m_defending = defending;
		m_costs = costs;
		m_data = data;
		m_bonus = bonus;
	}
	
	public int compare(final Unit u1, final Unit u2)
	{
		if (u1.equals(u2))
			return 0;
		final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
		final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
		if (ua1 == ua2)
			return 0;
		int power1 = BattleCalculator.getUnitPowerForSorting(u1, m_defending, m_data);
		int power2 = BattleCalculator.getUnitPowerForSorting(u2, m_defending, m_data);
		if (m_bonus)
		{
			if ((Matches.UnitIsTransport.match(u1) && Matches.transportIsTransporting().match(u1)) || (Matches.UnitIsAir.match(u1)) || Matches.UnitIsTwoHit.match(u1)
						|| (Matches.UnitIsCarrier.match(u1)))
				power1++;
			if ((Matches.UnitIsTransport.match(u2) && Matches.transportIsTransporting().match(u2)) || (Matches.UnitIsAir.match(u2)) || Matches.UnitIsTwoHit.match(u2)
						|| (Matches.UnitIsCarrier.match(u2)))
				power2++;
		}
		if (power1 != power2)
		{
			return power1 - power2;
		}
		final int cost1 = m_costs.getInt(u1.getType());
		final int cost2 = m_costs.getInt(u2.getType());
		if (cost1 != cost2)
		{
			return cost1 - cost2;
		}
		int power1reverse = BattleCalculator.getUnitPowerForSorting(u1, !m_defending, m_data);
		int power2reverse = BattleCalculator.getUnitPowerForSorting(u2, !m_defending, m_data);
		if (m_bonus)
		{
			if ((Matches.UnitIsTransport.match(u1) && Matches.transportIsTransporting().match(u1)) || (Matches.UnitIsAir.match(u1)) || Matches.UnitIsTwoHit.match(u1)
						|| (Matches.UnitIsCarrier.match(u1)))
				power1reverse++;
			if ((Matches.UnitIsTransport.match(u2) && Matches.transportIsTransporting().match(u2)) || (Matches.UnitIsAir.match(u2)) || Matches.UnitIsTwoHit.match(u2)
						|| (Matches.UnitIsCarrier.match(u2)))
				power2reverse++;
		}
		if (power1reverse != power2reverse)
		{
			return power1reverse - power2reverse;
		}
		if (Matches.UnitIsTransport.match(u1) && (Matches.UnitIsNotTransport.match(u2) || Matches.transportIsNotTransporting().match(u2)) && Matches.transportIsTransporting().match(u1))
			return 1;
		if (Matches.UnitIsTransport.match(u2) && (Matches.UnitIsNotTransport.match(u1) || Matches.transportIsNotTransporting().match(u1)) && Matches.transportIsTransporting().match(u2))
			return -1;
		return 0;
	}
}
