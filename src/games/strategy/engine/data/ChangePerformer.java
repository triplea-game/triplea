/*
 * ChangePerformer.java
 *
 * Created on January 1, 2002, 1:18 PM
 */

package games.strategy.engine.data;

/**
 *
 * @author  Sean Bridges
 * 
 * Allows changes to be performed outside of the data package.
 * Should not be created by non engine code.
 * Made this since I didnt want to unprotect the Change.perform method,
 * but didnt want to put everything that needed to 
 * perform a change in the data package.
 */
public class ChangePerformer 
{

	private GameData m_data;
	
	/** Creates a new instance of ChangePerformer */
    public ChangePerformer(GameData data) 
	{
		m_data = data;
    }
	
	public void perform(Change aChange)
	{
		aChange.perform(m_data);
		m_data.notifyGameDataChanged();
	}
}
