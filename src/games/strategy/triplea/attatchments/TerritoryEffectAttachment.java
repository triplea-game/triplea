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
 * TerritoryEffectAttachment.java
 */
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 
 * @author Edwin van der Wal
 * @version 1.0
 */
@SuppressWarnings("serial")
public class TerritoryEffectAttachment extends DefaultAttachment
{
	private IntegerMap<UnitType> m_combatDefenseEffect = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_combatOffenseEffect = new IntegerMap<UnitType>();
	private ArrayList<UnitType> m_noBlitz = new ArrayList<UnitType>();
	
	/**
	 * Creates new TerritoryEffectAttachment
	 */
	public TerritoryEffectAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Convenience method.
	 * 
	 * @return TerritoryEffectAttachment belonging to the RelationshipType pr
	 */
	public static TerritoryEffectAttachment get(final TerritoryEffect te)
	{
		final TerritoryEffectAttachment rVal = (TerritoryEffectAttachment) te.getAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME);
		if (rVal == null)
			throw new IllegalStateException("No territoryEffect attachment for:" + te.getName());
		return rVal;
	}
	
	public static TerritoryEffectAttachment get(final TerritoryEffect te, final String nameOfAttachment)
	{
		final TerritoryEffectAttachment rVal = (TerritoryEffectAttachment) te.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No territoryEffect attachment for:" + te.getName() + " with name:" + nameOfAttachment);
		return rVal;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param combatDefenseEffect
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setCombatDefenseEffect(final String combatDefenseEffect) throws GameParseException
	{
		setCombatEffect(combatDefenseEffect, true);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCombatDefenseEffect(final IntegerMap<UnitType> value)
	{
		m_combatDefenseEffect = value;
	}
	
	public IntegerMap<UnitType> getCombatDefenseEffect()
	{
		return new IntegerMap<UnitType>(m_combatDefenseEffect);
	}
	
	public void clearCombatDefenseEffect()
	{
		m_combatDefenseEffect.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param combatOffenseEffect
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setCombatOffenseEffect(final String combatOffenseEffect) throws GameParseException
	{
		setCombatEffect(combatOffenseEffect, false);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCombatOffenseEffect(final IntegerMap<UnitType> value)
	{
		m_combatOffenseEffect = value;
	}
	
	public IntegerMap<UnitType> getCombatOffenseEffect()
	{
		return new IntegerMap<UnitType>(m_combatOffenseEffect);
	}
	
	public void clearCombatOffenseEffect()
	{
		m_combatOffenseEffect.clear();
	}
	
	@InternalDoNotExport
	private void setCombatEffect(final String combatEffect, final boolean defending) throws GameParseException
	{
		final String[] s = combatEffect.split(":");
		if (s.length < 2)
			throw new GameParseException("combatDefenseEffect and combatOffenseEffect must have a count and at least one unitType" + thisErrorMsg());
		final Iterator<String> iter = Arrays.asList(s).iterator();
		final int effect = getInt(iter.next());
		while (iter.hasNext())
		{
			final String unitTypeToProduce = iter.next();
			final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
			if (ut == null)
				throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
			if (defending)
				m_combatDefenseEffect.put(ut, effect);
			else
				m_combatOffenseEffect.put(ut, effect);
		}
	}
	
	public int getCombatEffect(final UnitType aType, final boolean defending)
	{
		if (defending)
		{
			return m_combatDefenseEffect.getInt(aType);
		}
		else
		{
			return m_combatOffenseEffect.getInt(aType);
		}
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param noBlitzUnitTypes
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setNoBlitz(final String noBlitzUnitTypes) throws GameParseException
	{
		final String[] s = noBlitzUnitTypes.split(":");
		if (s.length < 1)
			throw new GameParseException("noBlitz must have at least one unitType" + thisErrorMsg());
		for (final String unitTypeName : Arrays.asList(s))
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeName);
			if (ut == null)
				throw new GameParseException("No unit called:" + unitTypeName + thisErrorMsg());
			m_noBlitz.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setNoBlitz(final ArrayList<UnitType> value)
	{
		m_noBlitz = value;
	}
	
	public ArrayList<UnitType> getNoBlitz()
	{
		return new ArrayList<UnitType>(m_noBlitz);
	}
	
	public void clearNoBlitz()
	{
		m_noBlitz.clear();
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
