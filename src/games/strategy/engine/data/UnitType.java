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
 * UnitType.java
 *
 * Created on October 14, 2001, 7:51 AM
 */

package games.strategy.engine.data;

import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.ui.TooltipProperties;
import games.strategy.triplea.ui.UIContext;

import java.util.*;
import java.io.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A prototype for units.
 */
public class UnitType extends NamedAttachable implements Serializable
{

	private static final long serialVersionUID = 4885339076798905247L;

    public UnitType(String name, GameData data)
	{
		super(name, data);
    }

	public List<Unit> create(int quantity, PlayerID owner)
	{
		return create(quantity, owner, false);
	}
    
    public List<Unit> create(int quantity, PlayerID owner, boolean isTemp)
    {
        List<Unit> collection = new ArrayList<Unit>();
        for(int i = 0; i < quantity; i++)
        {
            collection.add(create(owner, isTemp));
        }
        return collection;
    }

    private Unit create(PlayerID owner, boolean isTemp)
    {
        Unit u = getData().getGameLoader().getUnitFactory().createUnit(this, owner, getData());
        if(!isTemp) 
        {
            getData().getUnits().put(u);
        }
        return u;
    }


	public Unit create(PlayerID owner)
	{
		return create(owner, false);
	}

    
    
	public boolean equals(Object o)
	{
		if(o == null)
			return false;
		if(! (o instanceof UnitType) )
			return false;
		return ((UnitType) o).getName().equals(this.getName());
	}

	public int hashCode()
	{
		return getName().hashCode();
	}
	
    public String getTooltip(PlayerID playerId, boolean useHTML)
    {
        if (TooltipProperties.getInstance().getToolTip(this, playerId) == null || TooltipProperties.getInstance().getToolTip(this, playerId).equals("")) {
			return UnitAttachment.get(this).toStringShortAndOnlyImportantDifferences(playerId, useHTML, false);
			/*for(IAttachment at:this.getAttachments().values()) {
				try {
					UnitAttachment ut = (UnitAttachment) at;
					return ut.toStringShortAndOnlyImportantDifferences(playerId);
				} catch (ClassCastException cce) {
					// this wasn't a UnitAttachment: just ignore
				}
			}
			return ""; //Apparently no unitattachments.*/
		} else {
            return TooltipProperties.getInstance().getToolTip(this, playerId);
		}
	}

}