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
 * CompositeMatch.java
 *
 * Created on November 22, 2001, 3:32 PM
 */

package games.strategy.util;

import java.util.*;

/**
 *
 * Base class for composite matches.<br>  
 * Can add a match, or an inverse match. <br>
 * Subclasses must override match, and can call getMatches() to get a list of
 * matches added.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public abstract class CompositeMatch<T> extends Match<T>
{
	private List<Match<T>> m_matches = new ArrayList<Match<T>>(4);
	
	/** Creates new CompositeMatch */
    public CompositeMatch() 
	{
    }
	
	/**
	 *  Add a match.
	 */
	public void add(Match<T> match)
	{
		m_matches.add(match);
	}
	
	/** 
	 * Add the inverse of a match. Equivalant to add(new InverseMatch(aMatch))
	 */
	public void addInverse(Match<T> aMatch)
	{
		add(new InverseMatch<T>(aMatch));
	}
	
	/**
	 * Returns the matches, does not return a copy
	 * so be careful about modifying.  Also note this could 
	 * be regenerated when new matches are added.
	 */
	protected List<Match<T>> getMatches()
	{
		return m_matches;
	}
}
