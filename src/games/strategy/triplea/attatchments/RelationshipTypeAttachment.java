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
 * RelationshipTypeAttachment.java
 * 
 * Created on July 13th, 2011
 */
/**
 * 
 * 
 * 
 * @author Edwin van der Wal
 * @version 1.0
 * 
 */
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.Constants;

@SuppressWarnings("serial")
public class RelationshipTypeAttachment extends DefaultAttachment
{
	public static final String ARCHETYPE_NEUTRAL = Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL;
	public static final String ARCHETYPE_WAR = Constants.RELATIONSHIP_ARCHETYPE_WAR;
	public static final String ARCHETYPE_ALLIED = Constants.RELATIONSHIP_ARCHETYPE_ALLIED;
	public static final String UPKEEP_FLAT = "flat";
	public static final String UPKEEP_PERCENTAGE = "percentage";
	public static final String PROPERTY_DEFAULT = Constants.RELATIONSHIP_PROPERTY_DEFAULT;
	public static final String PROPERTY_TRUE = Constants.RELATIONSHIP_PROPERTY_TRUE;
	public static final String PROPERTY_FALSE = Constants.RELATIONSHIP_PROPERTY_FALSE;
	
	private String m_archeType = ARCHETYPE_WAR;
	private String m_helpsDefendAtSea = PROPERTY_DEFAULT;
	private String m_canMoveLandUnitsOverOwnedLand = PROPERTY_DEFAULT;
	private String m_canMoveAirUnitsOverOwnedLand = PROPERTY_DEFAULT;
	private String m_alliancesCanChainTogether = PROPERTY_DEFAULT;
	private String m_isDefaultWarPosition = PROPERTY_DEFAULT;
	private String m_upkeepCost = PROPERTY_DEFAULT;
	private String m_canLandAirUnitsOnOwnedLand = PROPERTY_DEFAULT;
	private String m_canTakeOverOwnedTerritory = PROPERTY_DEFAULT;
	private String m_givesBackOriginalTerritories = PROPERTY_DEFAULT;
	
	/**
	 * Creates new RelationshipTypeAttachment
	 * 
	 */
	public RelationshipTypeAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Convenience method.
	 * 
	 * @return RelationshipTypeAttachment belonging to the RelationshipType pr
	 */
	public static RelationshipTypeAttachment get(final RelationshipType pr)
	{
		final RelationshipTypeAttachment rVal = (RelationshipTypeAttachment) pr.getAttachment(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME);
		if (rVal == null)
			throw new IllegalStateException("No relationshipType attachment for:" + pr.getName());
		return rVal;
	}
	
	public static RelationshipTypeAttachment get(final RelationshipType pr, final String nameOfAttachment)
	{
		final RelationshipTypeAttachment rVal = (RelationshipTypeAttachment) pr.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("No relationshipType attachment for:" + pr.getName());
		return rVal;
	}
	
	/**
	 * This sets a ArcheType for this relationshipType, there are 3 different archeTypes: War, Allied and Neutral
	 * These archeTypes can be accessed by using the constants: WAR_ARCHETYPE, ALLIED_ARCHETYPE, NEUTRAL_ARCHETYPE
	 * These archeTypes determine the behavior of isAllied, isWar and isNeutral
	 * 
	 * These archeTyps determine the default behavior of the engine unless you override some option in this attachment;
	 * for example the RelationshipType ColdWar could be based on the WAR_ARCHETYPE but overrides options like "canInvade" "canAttackHomeTerritory"
	 * to not allow all-out invasion to mimic a not-all-out-war.
	 * Or you could base it on NEUTRAL_ARCHETYPE but override the options like "canAttackAtSea" and "canFireAA" to mimic a uneasy peace.
	 * 
	 * @param archeType
	 *            the template used to base this relationType on, can be war, allied or neutral, default archeType = WAR_ARCHETYPE
	 * @throws GameParseException
	 *             if archeType isn't set to war, allied or neutral
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setArcheType(final String archeType) throws GameParseException
	{
		if (archeType.toLowerCase().equals(ARCHETYPE_WAR))
			m_archeType = ARCHETYPE_WAR;
		else if (archeType.toLowerCase().equals(ARCHETYPE_ALLIED))
			m_archeType = ARCHETYPE_ALLIED;
		else if (archeType.toLowerCase().equals(ARCHETYPE_NEUTRAL))
			m_archeType = ARCHETYPE_NEUTRAL;
		else
			throw new GameParseException("RelationshipTypeAttachment: archeType must be " + ARCHETYPE_WAR + "," + ARCHETYPE_ALLIED + " or " + ARCHETYPE_NEUTRAL + " for "
						+ Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME + ": " + getName());
	}
	
	/**
	 * 
	 * @return the ArcheType of this relationshipType, this really shouldn't be called, typically you should call isNeutral, isAllied or isWar();
	 */
	public String getArcheType()
	{
		return m_archeType;
	}
	
	/**
	 * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of looking at isNeutral, isAllied or isWar();
	 * Just for future reference, doesn't do anything right now.
	 * 
	 * @param canFlyOver
	 *            should be "true", "false" or "default"
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanMoveAirUnitsOverOwnedLand(final String canFlyOver)
	{
		m_canMoveAirUnitsOverOwnedLand = canFlyOver;
	}
	
	/**
	 * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of looking at isNeutral, isAllied or isWar();
	 * Just for future reference, doesn't do anything right now.
	 * 
	 * @return whether in this relationshipType you can fly over other territories
	 */
	public boolean getCanMoveAirUnitsOverOwnedLand()
	{ // War: true, Allied: True, Neutral: false
		if (m_canMoveAirUnitsOverOwnedLand.equals(PROPERTY_DEFAULT))
		{
			return isWar() || isAllied();
		}
		return m_canMoveAirUnitsOverOwnedLand.equals(PROPERTY_TRUE);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanMoveLandUnitsOverOwnedLand(final String canFlyOver)
	{
		m_canMoveLandUnitsOverOwnedLand = canFlyOver;
	}
	
	public boolean getCanMoveLandUnitsOverOwnedLand()
	{ // War: true, Allied: True, Neutral: false
		if (m_canMoveLandUnitsOverOwnedLand.equals(PROPERTY_DEFAULT))
		{
			return isWar() || isAllied();
		}
		return m_canMoveLandUnitsOverOwnedLand.equals(PROPERTY_TRUE);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanLandAirUnitsOnOwnedLand(final String canLandAir)
	{
		m_canLandAirUnitsOnOwnedLand = canLandAir;
	}
	
	public boolean getCanLandAirUnitsOnOwnedLand()
	{
		// War: false, Allied: true, Neutral: false
		if (m_canLandAirUnitsOnOwnedLand.equals(PROPERTY_DEFAULT))
		{
			return isAllied();
		}
		return m_canLandAirUnitsOnOwnedLand.equals(PROPERTY_TRUE);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanTakeOverOwnedTerritory(final String canTakeOver)
	{
		m_canTakeOverOwnedTerritory = canTakeOver;
	}
	
	public boolean getCanTakeOverOwnedTerritory()
	{
		// War: true, Allied: false, Neutral: false
		if (m_canTakeOverOwnedTerritory.equals(PROPERTY_DEFAULT))
		{
			return isWar();
		}
		return m_canTakeOverOwnedTerritory.equals(PROPERTY_TRUE);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUpkeepCost(final String integerCost) throws GameParseException
	{
		if (integerCost.equals(PROPERTY_DEFAULT))
			m_upkeepCost = PROPERTY_DEFAULT;
		else
		{
			final String[] s = integerCost.split(":");
			if (s.length < 1 || s.length > 2)
				throw new GameParseException("RelationshipTypeAttachment: upkeepCost must have either 1 or 2 fields");
			final int cost = getInt(s[0]);
			if (s.length == 2)
			{
				if (s[1].equals(UPKEEP_FLAT))
				{
				}
				else if (s[1].equals(UPKEEP_PERCENTAGE))
				{
					if (cost > 100)
						throw new GameParseException("RelationshipTypeAttachment: upkeepCost may not have a percentage greater than 100");
				}
				else
				{
					throw new GameParseException("RelationshipTypeAttachment: upkeepCost must have either: " + UPKEEP_FLAT + " or " + UPKEEP_PERCENTAGE);
				}
			}
			m_upkeepCost = integerCost;
		}
	}
	
	public String getUpkeepCost()
	{
		if (m_upkeepCost.equals(PROPERTY_DEFAULT))
			return String.valueOf(0);
		return m_upkeepCost;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAlliancesCanChainTogether(final String value) throws GameParseException
	{
		if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE)))
			throw new GameParseException("RelationshipTypeAttachment: alliancesCanChainTogether must be either " + PROPERTY_DEFAULT + " or " + PROPERTY_FALSE + " or " + PROPERTY_TRUE);
		m_alliancesCanChainTogether = value;
	}
	
	public boolean getAlliancesCanChainTogether()
	{
		if (m_alliancesCanChainTogether.equals(PROPERTY_DEFAULT) || isWar() || isNeutral())
			return false;
		return m_alliancesCanChainTogether.equals(PROPERTY_TRUE);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIsDefaultWarPosition(final String value) throws GameParseException
	{
		if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE)))
			throw new GameParseException("RelationshipTypeAttachment: isDefaultWarPosition must be either " + PROPERTY_DEFAULT + " or " + PROPERTY_FALSE + " or " + PROPERTY_TRUE);
		m_isDefaultWarPosition = value;
	}
	
	public boolean getIsDefaultWarPosition()
	{
		if (m_isDefaultWarPosition.equals(PROPERTY_DEFAULT) || isAllied() || isNeutral())
			return false;
		return m_isDefaultWarPosition.equals(PROPERTY_TRUE);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setGivesBackOriginalTerritories(final String value) throws GameParseException
	{
		if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE)))
			throw new GameParseException("RelationshipTypeAttachment: givesBackOriginalTerritories must be either " + PROPERTY_DEFAULT + " or " + PROPERTY_FALSE + " or " + PROPERTY_TRUE);
		m_givesBackOriginalTerritories = value;
	}
	
	public boolean getGivesBackOriginalTerritories()
	{
		if (m_givesBackOriginalTerritories.equals(PROPERTY_DEFAULT))
			return false;
		return m_givesBackOriginalTerritories.equals(PROPERTY_TRUE);
	}
	
	/**
	 * 
	 * @return whether this relationship is based on the WAR_ARCHETYPE
	 */
	public boolean isWar()
	{
		return m_archeType.equals(RelationshipTypeAttachment.ARCHETYPE_WAR);
	}
	
	/**
	 * 
	 * @return whether this relationship is based on the ALLIED_ARCHETYPE
	 */
	public boolean isAllied()
	{
		return m_archeType.equals(RelationshipTypeAttachment.ARCHETYPE_ALLIED);
	}
	
	/**
	 * 
	 * @return whether this relationship is based on the NEUTRAL_ARCHETYPE
	 */
	public boolean isNeutral()
	{
		return m_archeType.equals(RelationshipTypeAttachment.ARCHETYPE_NEUTRAL);
	}
	
	/**
	 * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of looking at isNeutral, isAllied or isWar();
	 * Just for future reference, doesn't do anything right now.
	 * 
	 * @param helpsDefendAtSea
	 *            should be "true", "false" or "default"
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setHelpsDefendAtSea(final String helpsDefendAtSea) throws GameParseException
	{
		if (helpsDefendAtSea.toLowerCase().equals(PROPERTY_TRUE))
			m_helpsDefendAtSea = PROPERTY_TRUE;
		else if (helpsDefendAtSea.toLowerCase().equals(PROPERTY_FALSE))
			m_helpsDefendAtSea = PROPERTY_FALSE;
		else if (helpsDefendAtSea.toLowerCase().equals(PROPERTY_DEFAULT))
			m_helpsDefendAtSea = PROPERTY_DEFAULT;
		else
			throw new GameParseException("helpsDefendAtSea must be " + PROPERTY_TRUE + "," + PROPERTY_FALSE + " or " + PROPERTY_DEFAULT + " for " + Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME + ": "
						+ getName());
	}
	
	/**
	 * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of looking at isNeutral, isAllied or isWar();
	 * Just for future reference, doesn't do anything right now.
	 * 
	 * @return whether in this relationshipType you help each other defend at Sea
	 */
	public boolean getHelpsDefendAtSea()
	{ // War: false, Allied: true, Neutral: false
		if (m_helpsDefendAtSea.equals(PROPERTY_DEFAULT))
			return isAllied();
		return m_helpsDefendAtSea.equals(PROPERTY_TRUE);
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
