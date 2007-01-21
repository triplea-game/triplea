/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * CompositeMatchAnd.java
 *
 * Created on November 10, 2001, 11:13 AM
 */

package games.strategy.util;

import java.util.List;

/**
 *
 * True if all matches return true.
 *
 * @author  Sean Bridges
 *
 */
public class CompositeMatchAnd<T> extends CompositeMatch<T>
{

	/** Creates new CompositeMatchOr */
    public CompositeMatchAnd(Match ...matches) 
	{
		super();
		for(Match<T> m : matches) 
        {
		    add(m);
        }
    }
    

	public boolean match(T o) 
	{
		List<Match<T>> matches = super.getMatches();
		for(int i = 0; i < matches.size(); i++)
		{
			if (!matches.get(i).match(o))
			{
				return false;
			}
		}
		return true;
	}
}
