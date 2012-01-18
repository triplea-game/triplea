/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/**
 * Compares two Unit types
 */
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;

import java.util.Comparator;

public class UnitTypeComparator implements Comparator<UnitType>
{
	public int compare(final UnitType o1, final UnitType o2)
	{
		final UnitType u1 = o1;
		final UnitType u2 = o2;
		final UnitAttachment ua1 = UnitAttachment.get(u1);
		final UnitAttachment ua2 = UnitAttachment.get(u2);
		if (ua1 == null)
			throw new IllegalStateException("No unit type attachment for unit type : " + u1.getName());
		if (ua2 == null)
			throw new IllegalStateException("No unit type attachment for unit type : " + u2.getName());
		if (ua1.getIsFactory() && !ua2.getIsFactory())
			return 1;
		if (ua2.getIsFactory() && !ua1.getIsFactory())
			return -1;
		if (Matches.UnitTypeIsAAforAnything.match(u1) && !Matches.UnitTypeIsAAforAnything.match(u2))
			return 1;
		if (!Matches.UnitTypeIsAAforAnything.match(u1) && Matches.UnitTypeIsAAforAnything.match(u2))
			return -1;
		if (ua1.getIsAir() && !ua2.getIsAir())
			return 1;
		if (ua2.getIsAir() && !ua1.getIsAir())
			return -1;
		if (ua1.getIsSea() && !ua2.getIsSea())
			return 1;
		if (ua2.getIsSea() && !ua1.getIsSea())
			return -1;
		if (ua1.getRawAttack() != ua2.getRawAttack())
			return ua1.getRawAttack() - ua2.getRawAttack();
		return u1.getName().compareTo(u2.getName());
	}
}
