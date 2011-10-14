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
 * PlayerAttatchment.java
 * 
 * Created on August 29, 2005, 3:14 PM
 */

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * @author Adam Jette
 * @version 1.0
 */
public class PlayerAttachment extends DefaultAttachment
{
	/**
	 * Convenience method. can be null
	 */
	public static PlayerAttachment get(PlayerID p)
	{
		PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(Constants.PLAYER_ATTACHMENT_NAME);
		// allow null
		return rVal;
	}
	
	public static PlayerAttachment get(PlayerID p, String nameOfAttachment)
	{
		PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No player attachment for:" + p.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	private int m_vps = 0;
	private int m_captureVps = 0; // need to store some data during a turn
	private int m_retainCapitalNumber = 1; // number of capitals needed before we lose all our money
	private int m_retainCapitalProduceNumber = 1; // number of capitals needed before we lose ability to gain money and produce units
	private Collection<PlayerID> m_giveUnitControl = new ArrayList<PlayerID>();
	private Collection<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<PlayerID>();
	private boolean m_destroysPUs = false; // do we lose our money and have it disappear or is that money captured?
	
	/** Creates new PlayerAttachment */
	public PlayerAttachment()
	{
	}
	
	public void setVps(String value)
	{
		m_vps = getInt(value);
	}
	
	public String getVps()
	{
		return "" + m_vps;
	}
	
	public void setCaptureVps(String value)
	{
		m_captureVps = getInt(value);
	}
	
	public String getCaptureVps()
	{
		return "" + m_captureVps;
	}
	
	public void setRetainCapitalNumber(String value)
	{
		m_retainCapitalNumber = getInt(value);
	}
	
	public int getRetainCapitalNumber()
	{
		return m_retainCapitalNumber;
	}
	
	public void setRetainCapitalProduceNumber(String value)
	{
		m_retainCapitalProduceNumber = getInt(value);
	}
	
	public int getRetainCapitalProduceNumber()
	{
		return m_retainCapitalProduceNumber;
	}
	
	// setTakeUnitControl and getTakeUnitControl DO NOTHING. They are kept for backwards compatibility only, otherwise users get Java errors.
	public void setTakeUnitControl(String value)
	{
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	public void setGiveUnitControl(String value)
	{
		String[] temp = value.split(":");
		for (String name : temp)
		{
			PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_giveUnitControl.add(tempPlayer);
			else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false"))
				m_giveUnitControl.clear();
			else
				throw new IllegalStateException("Player Attachments: No player named: " + name);
		}
	}
	
	public Collection<PlayerID> getGiveUnitControl()
	{
		return m_giveUnitControl;
	}
	
	public void clearGiveUnitControl()
	{
		m_giveUnitControl.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	public void setCaptureUnitOnEnteringBy(String value)
	{
		String[] temp = value.split(":");
		for (String name : temp)
		{
			PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_captureUnitOnEnteringBy.add(tempPlayer);
			else
				throw new IllegalStateException("Player Attachments: No player named: " + name);
		}
	}
	
	public Collection<PlayerID> getCaptureUnitOnEnteringBy()
	{
		return m_captureUnitOnEnteringBy;
	}
	
	public void clearCaptureUnitOnEnteringBy()
	{
		m_captureUnitOnEnteringBy.clear();
	}
	
	public void setDestroysPUs(String value)
	{
		m_destroysPUs = getBool(value);
	}
	
	public boolean getDestroysPUs()
	{
		return m_destroysPUs;
	}
}
