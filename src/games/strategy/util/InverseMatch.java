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
public class InverseMatch extends Match
{
	private Match m_match;
	
	/** Creates new CompositeMatchOr */
    public InverseMatch(Match aMatch) 
	{
		m_match = aMatch;
		
    }

	public boolean match(Object o) 
	{
		return !m_match.match(o);
	}
	
}
