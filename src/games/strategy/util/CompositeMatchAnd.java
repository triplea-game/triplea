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
 * @version 1.0
 *
 */
public class CompositeMatchAnd extends CompositeMatch
{

	public CompositeMatchAnd()
	{
	}
	
	/** Creates new CompositeMatchOr */
    public CompositeMatchAnd(Match first, Match second) 
	{
		super();
		add(first);
		add(second);
    }

	public boolean match(Object o) 
	{
		List matches = super.getMatches();
		for(int i = 0; i < matches.size(); i++)
		{
			if (!((Match) matches.get(i)).match(o))
			{
				return false;
			}
		}
		return true;
	}
}
