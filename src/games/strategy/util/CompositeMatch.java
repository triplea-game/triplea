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
public abstract class CompositeMatch extends Match
{
	private List m_matches = new ArrayList(4);
	
	/** Creates new CompositeMatch */
    public CompositeMatch() 
	{
    }
	
	/**
	 *  Add a match.
	 */
	public void add(Match match)
	{
		m_matches.add(match);
	}
	
	/** 
	 * Add the inverse of a match. Equivalant to add(new InverseMatch(aMatch))
	 */
	public void addInverse(Match aMatch)
	{
		add(new InverseMatch(aMatch));
	}
	
	/**
	 * Returns the matches, does not return a copy
	 * so be careful about modifying.  Also note this could 
	 * be regenerated when new matches are added.
	 */
	protected List getMatches()
	{
		return m_matches;
	}
}
