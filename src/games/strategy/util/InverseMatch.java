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
 * InverseMatch.java
 *
 * Created on November 10, 2001, 11:13 AM
 */

package games.strategy.util;

/**
 *
 * A match that returns the negation of the given match.
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public class InverseMatch<T> extends Match<T>
{
	private Match<T> m_match;
	
	/** Creates new CompositeMatchOr */
    public InverseMatch(Match<T> aMatch) 
	{
		m_match = aMatch;
		
    }

	public boolean match(T o) 
	{
		return !m_match.match(o);
	}
	
}
