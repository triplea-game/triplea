/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * PoliticsDelegate.java
 * 
 * Created on July 16, 2011
 */


package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;



/**
 * 
 * Responsible allowing players to perform politicalActions and showing political state
 * 
 * @author Edwin van der Wal
 * @version 1.0
 *  
 */
public class PoliticsDelegate implements IPoliticsDelegate
{
    
  

    private String m_name;
	private String m_displayName ;
	private boolean m_firstRun = true;
	private TripleADelegateBridge m_bridge;
	private PlayerID m_player;
	private GameData m_data;

	/** Creates new PoliticsDelegate */
    public PoliticsDelegate()
    {

    }
    
    /** run the first time only */
    private void firstRun() {
    	m_firstRun = false;
    }

    public void initialize(String name, String displayName)
    {
     m_name = name;
     m_displayName = displayName;
    }

    
    /**
     * Called before the delegate will run.
     */
	public void start(IDelegateBridge aBridge, GameData gameData) {
		// Run only the first time 
		if (m_firstRun)
	            firstRun();
        m_bridge = new TripleADelegateBridge(aBridge, gameData);
        m_player = aBridge.getPlayerID();
        m_data = gameData;
        
        if(games.strategy.triplea.Properties.getTriggers(m_data)) {
        	TriggerAttachment.triggerRelationshipChange(m_player,m_bridge,m_data); 
        }		 
	}
    
  
    public String getName()
    {

        return m_name;
    }

    public String getDisplayName()
    {

        return m_displayName;
    }


    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
    	

    }
  
    public Serializable saveState()
    {
    	PoliticsState state = new PoliticsState();
    	state.m_firstRun = m_firstRun;
        return state;
    }

    /*
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<IPoliticsDelegate> getRemoteType()
    {
        return IPoliticsDelegate.class;
    }

	public void loadState(Serializable aState) {
        PoliticsState state = (PoliticsState) aState;
        m_firstRun = state.m_firstRun;
		
	}
}

@SuppressWarnings("serial")
class PoliticsState implements Serializable
{
    public boolean m_firstRun = true;

}
