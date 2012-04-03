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
/*
 * DelegateFinder.java
 * 
 * Created on November 28, 2001, 2:58 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class DelegateFinder
{
	private static final IDelegate findDelegate(final GameData data, final String delegate_name)
	{
		final IDelegate delegate = data.getDelegateList().getDelegate(delegate_name);
		if (delegate == null)
			throw new IllegalStateException(delegate_name + " delegate not found");
		return delegate;
	}
	
	public static final PoliticsDelegate politicsDelegate(final GameData data)
	{
		return (PoliticsDelegate) findDelegate(data, "politics");
	}
	
	public static final BattleDelegate battleDelegate(final GameData data)
	{
		return (BattleDelegate) findDelegate(data, "battle");
	}
	
	public static final MoveDelegate moveDelegate(final GameData data)
	{
		return (MoveDelegate) findDelegate(data, "move");
	}
	
	public static final AbstractPlaceDelegate placeDelegate(final GameData data)
	{
		return (AbstractPlaceDelegate) findDelegate(data, "place");
	}
	
	// TODO: this is a really shitty way of figuring out what step / delegate we are in....
	public static final BidPlaceDelegate bidPlaceDelegate(final GameData data)
	{
		return (BidPlaceDelegate) findDelegate(data, "placeBid");
	}
	
	public static final TechnologyDelegate techDelegate(final GameData data)
	{
		return (TechnologyDelegate) findDelegate(data, "tech");
	}
	
	public static final GivePUsDelegate givePUsDelegate(final GameData data)
	{
		return (GivePUsDelegate) findDelegate(data, "givePUs");
	}
}
