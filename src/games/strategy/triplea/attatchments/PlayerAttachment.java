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

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Adam Jette, and Mark Christopher Duncan
 * 
 * @version 1.0
 */
@SuppressWarnings("serial")
public class PlayerAttachment extends DefaultAttachment
{
	/**
	 * Convenience method. can be null
	 */
	public static PlayerAttachment get(final PlayerID p)
	{
		final PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(Constants.PLAYER_ATTACHMENT_NAME);
		// allow null
		return rVal;
	}
	
	public static PlayerAttachment get(final PlayerID p, final String nameOfAttachment)
	{
		final PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No player attachment for:" + p.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	private int m_vps = 0;
	private int m_captureVps = 0; // need to store some data during a turn
	private int m_retainCapitalNumber = 1; // number of capitals needed before we lose all our money
	private int m_retainCapitalProduceNumber = 1; // number of capitals needed before we lose ability to gain money and produce units
	private final Collection<PlayerID> m_giveUnitControl = new ArrayList<PlayerID>();
	private final Collection<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<PlayerID>();
	private boolean m_destroysPUs = false; // do we lose our money and have it disappear or is that money captured?
	private final IntegerMap<Resource> m_suicideAttackResources = new IntegerMap<Resource>();
	private Set<UnitType> m_suicideAttackTargets = null;
	
	/** Creates new PlayerAttachment */
	public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	public void setSuicideAttackTargets(final String value) throws GameParseException
	{
		if (value == null)
		{
			m_suicideAttackTargets = null;
			return;
		}
		if (m_suicideAttackTargets == null)
			m_suicideAttackTargets = new HashSet<UnitType>();
		final String[] s = value.split(":");
		for (final String u : s)
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(u);
			if (ut == null)
				throw new GameParseException("Player Attachment: suicideAttackTargets: no such unit called " + u);
			m_suicideAttackTargets.add(ut);
		}
	}
	
	public Set<UnitType> getSuicideAttackTargets()
	{
		return m_suicideAttackTargets;
	}
	
	public void clearSuicideAttackTargets()
	{
		m_suicideAttackTargets.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 * @throws GameParseException
	 */
	public void setSuicideAttackResources(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("Player Attachment: suicideAttackResources must have exactly 2 fields");
		final int attackValue = getInt(s[0]);
		if (attackValue < 0)
			throw new GameParseException("Player Attachment: suicideAttackResources attack value must be positive");
		final Resource r = getData().getResourceList().getResource(s[1]);
		if (r == null)
			throw new GameParseException("Player Attachment: no such resource: " + s[1]);
		m_suicideAttackResources.put(r, attackValue);
	}
	
	public IntegerMap<Resource> getSuicideAttackResources()
	{
		return m_suicideAttackResources;
	}
	
	public void clearSuicideAttackResources()
	{
		m_suicideAttackResources.clear();
	}
	
	public void setVps(final String value)
	{
		m_vps = getInt(value);
	}
	
	public String getVps()
	{
		return "" + m_vps;
	}
	
	public void setCaptureVps(final String value)
	{
		m_captureVps = getInt(value);
	}
	
	public String getCaptureVps()
	{
		return "" + m_captureVps;
	}
	
	public void setRetainCapitalNumber(final String value)
	{
		m_retainCapitalNumber = getInt(value);
	}
	
	public int getRetainCapitalNumber()
	{
		return m_retainCapitalNumber;
	}
	
	public void setRetainCapitalProduceNumber(final String value)
	{
		m_retainCapitalProduceNumber = getInt(value);
	}
	
	public int getRetainCapitalProduceNumber()
	{
		return m_retainCapitalProduceNumber;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	public void setGiveUnitControl(final String value)
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
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
	public void setCaptureUnitOnEnteringBy(final String value)
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
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
	
	public void setDestroysPUs(final String value)
	{
		m_destroysPUs = getBool(value);
	}
	
	public boolean getDestroysPUs()
	{
		return m_destroysPUs;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
	
	/** setTakeUnitControl (and getTakeUnitControl) DO NOTHING. They are kept for backwards compatibility only, otherwise users get Java errors. */
	@Deprecated
	public void setTakeUnitControl(final String value)
	{
	}
}
