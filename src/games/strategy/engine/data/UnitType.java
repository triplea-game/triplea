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
 * UnitType.java
 * 
 * Created on October 14, 2001, 7:51 AM
 */
package games.strategy.engine.data;

import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.ui.TooltipProperties;
import games.strategy.util.LocalizeHTML;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          A prototype for units.
 */
public class UnitType extends NamedAttachable implements Serializable
{
	private static final long serialVersionUID = 4885339076798905247L;
	
	public UnitType(final String name, final GameData data)
	{
		super(name, data);
	}
	
	public List<Unit> create(final int quantity, final PlayerID owner)
	{
		return create(quantity, owner, false);
	}
	
	public List<Unit> create(final int quantity, final PlayerID owner, final boolean isTemp)
	{
		return create(quantity, owner, false, 0, 0);
	}
	
	public List<Unit> create(final int quantity, final PlayerID owner, final boolean isTemp, final int hitsTaken, final int bombingUnitDamage)
	{
		final List<Unit> collection = new ArrayList<Unit>();
		for (int i = 0; i < quantity; i++)
		{
			collection.add(create(owner, isTemp, hitsTaken, bombingUnitDamage));
		}
		return collection;
	}
	
	private Unit create(final PlayerID owner, final boolean isTemp, final int hitsTaken, final int bombingUnitDamage)
	{
		final Unit u = getData().getGameLoader().getUnitFactory().createUnit(this, owner, getData());
		u.setHits(hitsTaken);
		if (u instanceof TripleAUnit)
		{
			((TripleAUnit) u).setUnitDamage(bombingUnitDamage);
		}
		if (!isTemp)
		{
			getData().getUnits().put(u);
		}
		return u;
	}
	
	private Unit create(final PlayerID owner, final boolean isTemp)
	{
		return create(owner, isTemp, 0, 0);
	}
	
	public Unit create(final PlayerID owner)
	{
		return create(owner, false);
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o == null)
			return false;
		if (!(o instanceof UnitType))
			return false;
		return ((UnitType) o).getName().equals(this.getName());
	}
	
	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}
	
	public String getTooltip(final PlayerID playerId, final boolean useHTML)
	{
		final String customTip = TooltipProperties.getInstance().getToolTip(this, playerId);
		if (customTip == null || customTip.trim().length() <= 0)
		{
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
		}
		else
		{
			return LocalizeHTML.localizeImgLinksInHTML(customTip.trim());
		}
	}
}
