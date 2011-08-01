/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
 * @author  Edwin van der Wal
 * @version 1.0
 *
 */

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.Constants;

@SuppressWarnings("serial")
public class RelationshipTypeAttachment extends DefaultAttachment {
	
	public static final String NEUTRAL_ARCHETYPE = "neutral";
	public static final String WAR_ARCHETYPE = "war";
	public static final String ALLIED_ARCHETYPE = "allied";
	
	private final String PROPERTY_DEFAULT = "default";
	private final String PROPERTY_TRUE = "true";
	private final String PROPERTY_FALSE = "false";



	private String m_archeType = WAR_ARCHETYPE;
	private String m_canFlyOver = PROPERTY_DEFAULT;
	private String m_helpsDefendAtSea = PROPERTY_DEFAULT;

    /**
     * Convenience method.
     * @return RelationshipTypeAttachment belonging to the RelationshipType pr
     */
    public static RelationshipTypeAttachment get(RelationshipType pr)
    {
        return (RelationshipTypeAttachment) pr.getAttachment(Constants.RELATIONSHIPTYPE_ATTATCHMENT_NAME);
    }

    /** Creates new RelationshipTypeAttachment
     * 
     */
    public RelationshipTypeAttachment() {	
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
     * @param archeType the template used to base this relationType on, can be war, allied or neutral, default archeType = WAR_ARCHETYPE
     * @throws GameParseException if archeType isn't set to war, allied or neutral 
     */
    public void setArcheType(String archeType) throws GameParseException {
    	if(archeType.toLowerCase().equals(WAR_ARCHETYPE)) 
    		m_archeType = WAR_ARCHETYPE;
    	else if(archeType.toLowerCase().equals(ALLIED_ARCHETYPE))
    		m_archeType = ALLIED_ARCHETYPE;
    	else if(archeType.toLowerCase().equals(NEUTRAL_ARCHETYPE))
    		m_archeType = NEUTRAL_ARCHETYPE;
    	else throw new GameParseException("archeType must be "+WAR_ARCHETYPE+","+ALLIED_ARCHETYPE+" or "+NEUTRAL_ARCHETYPE+" for "+Constants.RELATIONSHIPTYPE_ATTATCHMENT_NAME+": "+getName());
    }
    
    /**
     * 
     * @return the ArcheType of this relationshipType, this really shouldn't be called, typically you should call isNeutral, isAllied or isWar();
     */
    public String getArcheType() {
    	return m_archeType;
    }
    
    /** 
     * <strong> EXAMPLE</strong> method on how you could do finegrained autorisations instead of looking at isNeutral, isAllied or isWar();
     * Just for future reference, doesn't do anything right now.
     * @param canFlyOver should be "true", "false" or "default"
     */
    public void setCanFlyOver(String canFlyOver) { 
    	m_canFlyOver = canFlyOver;
    }
    
    /**
     * <strong> EXAMPLE</strong> method on how you could do finegrained autorisations instead of looking at isNeutral, isAllied or isWar();
     * Just for future reference, doesn't do anything right now.
     * @return whether in this relationshipType you can fly over other territories
     */
    public boolean canFlyOver() { // War: true, Allied: True, Neutral: false
    	if(m_canFlyOver.equals(PROPERTY_DEFAULT)) {
    		return isWar() || isAllied();
    	}
		return m_canFlyOver.equals(PROPERTY_TRUE);
    }
    
    /**
     * 
     * @return whether this relationship is based on the WAR_ARCHETYPE
     */
    public boolean isWar() {
		return m_archeType.equals(RelationshipTypeAttachment.WAR_ARCHETYPE);
	}

    /**
     * 
     * @return whether this relationship is based on the ALLIED_ARCHETYPE
     */
	public boolean isAllied() {
		return m_archeType.equals(RelationshipTypeAttachment.ALLIED_ARCHETYPE);
	}
	 /**
     * 
     * @return whether this relationship is based on the NEUTRAL_ARCHETYPE
     */
	public boolean isNeutral() {
		return m_archeType.equals(RelationshipTypeAttachment.NEUTRAL_ARCHETYPE);
	}
    /** 
     * <strong> EXAMPLE</strong> method on how you could do finegrained autorisations instead of looking at isNeutral, isAllied or isWar();
     * Just for future reference, doesn't do anything right now.
     * @param helpsDefendAtSea should be "true", "false" or "default"
     * @throws GameParseException 
     */
	public void setHelpsDefendAtSea(String helpsDefendAtSea) throws GameParseException { 
		if(helpsDefendAtSea.toLowerCase().equals(PROPERTY_TRUE)) 
			m_helpsDefendAtSea = PROPERTY_TRUE;
    	else if(helpsDefendAtSea.toLowerCase().equals(PROPERTY_FALSE))
    		m_helpsDefendAtSea = PROPERTY_FALSE;
    	else if(helpsDefendAtSea.toLowerCase().equals(PROPERTY_DEFAULT))
    		m_helpsDefendAtSea = PROPERTY_DEFAULT;
    	else throw new GameParseException("helpsDefendAtSea must be "+PROPERTY_TRUE+","+PROPERTY_FALSE+" or "+PROPERTY_DEFAULT+" for "+Constants.RELATIONSHIPTYPE_ATTATCHMENT_NAME+": "+getName());
     }
    /**
     * <strong> EXAMPLE</strong> method on how you could do finegrained autorisations instead of looking at isNeutral, isAllied or isWar();
     * Just for future reference, doesn't do anything right now.
     * @return whether in this relationshipType you help eachother defend at Sea
     */   
    public boolean helpsDefendAtSea() { // War: false, Allied: true, Neutral: false
    	if(m_helpsDefendAtSea.equals(PROPERTY_DEFAULT)) 
    		return isAllied();
    	return m_helpsDefendAtSea.equals(PROPERTY_TRUE);
    }
 }

