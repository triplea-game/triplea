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
 * TerritoryEffectAttachment.java
 *
 */

package games.strategy.triplea.attatchments;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;



/**
 *
 * @author  Edwin van der Wal
 * @version 1.0
 */

@SuppressWarnings("serial")
public class TerritoryEffectAttachment extends DefaultAttachment {
	
	private IntegerMap<UnitType> m_combatDefenseEffect = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_combatOffenseEffect = new IntegerMap<UnitType>();
	
	   /**
     * Convenience method.
     * @return TerritoryEffectAttachment belonging to the RelationshipType pr
     */
    public static TerritoryEffectAttachment get(TerritoryEffect te)
    {
        return (TerritoryEffectAttachment) te.getAttachment(Constants.TERRITORYEFFECT_ATTACHMENT_NAME);
    }
    
    /** Creates new TerritoryEffectAttachment
     * 
     */
    public TerritoryEffectAttachment() {	
    }
    
    public void setCombatDefenseEffect(String combatDefenseEffect) throws GameParseException {
    	setCombatEffect(combatDefenseEffect, true);
    }
    
    public void setCombatOffenseEffect(String combatOffenseEffect) throws GameParseException {
    	setCombatEffect(combatOffenseEffect, false);
    }

	private void setCombatEffect(String combatEffect, boolean defending) throws GameParseException {
		String[] s = combatEffect.split(":");
		if (s.length < 2)
			throw new GameParseException("TerritoryEffect Attachments: combatDefenseEffect and combatOffenseEffect must have a count and at least one unitType");
		Iterator<String> iter = Arrays.asList(s).iterator();
		int effect = getInt(iter.next());
		while (iter.hasNext())
		{
			String unitTypeToProduce = iter.next();
			UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
			if (ut == null)
				throw new IllegalStateException("TerritoryEffect Attachments: No unit called:" + unitTypeToProduce);
			if (defending)
				m_combatDefenseEffect.put(ut, effect);
			else
				m_combatOffenseEffect.put(ut, effect);
		}
	}
	
	public int getCombatEffect(UnitType aType, boolean defending) {
		if(defending) {
			return m_combatDefenseEffect.getInt(aType);
		} else {
			return m_combatOffenseEffect.getInt(aType);
		}
	}
	
	public String toString()
	{
		return this.getName();
	}
	

}
