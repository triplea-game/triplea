/*
 * CompositeMatchOr.java
 *
 * Created on November 10, 2001, 11:13 AM
 */

package games.strategy.util;

import java.util.List;

/**
 *
 * True if one match returns true.
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public class CompositeMatchOr extends CompositeMatch
{

	public CompositeMatchOr()
	{
	}
	
	/** Creates new CompositeMatchOr */
    public CompositeMatchOr(Match first, Match second) 
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
			if ( ((Match)matches.get(i)).match(o))
			{
				return true;
			}
		}
		return false;
	}
}
