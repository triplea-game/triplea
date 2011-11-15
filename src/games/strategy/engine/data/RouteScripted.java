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
 * Route.java
 * 
 * Created on October 12, 2001, 5:23 PM
 */

package games.strategy.engine.data;

/**
 * A scripted or cheating Route, designed for use with Triggers and with units stranded in enemy territory, or other situations where you want the "end" to not be null.
 * If the Route only has a start, it will return the start when you call .end(), and it will return a length of 1 if the length is really zero.
 * 
 * @author Chris Duncan
 * @version 1.0
 * 
 */
public class RouteScripted extends Route
{
	private static final long serialVersionUID = 604474811874966546L;
	
	public RouteScripted()
	{}
	
	/**
	 * Shameless cheating. Making a fake route, so as to handle battles properly without breaking battleTracker protected status or duplicating a zillion lines of code.
	 * The End will return the Start, and the Length will be 1.
	 */
	public RouteScripted(Territory terr)
	{
		super(terr);
	}
	
	public RouteScripted(Territory start, Territory... route)
	{
		super(start, route);
	}
	
	@Override
	public void add(Territory t)
	{
		// maybe we don't check for loops?
		super.add(t);
	}
	
	@Override
	public int getLength()
	{
		if (super.getLength() < 1)
			return 1;
		return super.getLength();
	}
	
	@Override
	public Territory getEnd()
	{
		if (super.getEnd() == null)
			return super.getStart();
		return super.getEnd();
	}
	
	@Override
	public Territory at(int i)
	{
		try
		{
			if (super.getEnd() == null || super.at(i) == null)
				return super.getStart();
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return super.getStart();
		}
		return super.at(i);
	}
}
