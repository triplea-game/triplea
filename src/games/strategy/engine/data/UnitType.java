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
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.TooltipProperties;
import games.strategy.util.LocalizeHTML;

import java.awt.Image;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
		return create(quantity, owner, isTemp, 0, 0);
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
			return UnitAttachment.get(this).toStringShortAndOnlyImportantDifferences((playerId == null ? PlayerID.NULL_PLAYERID : playerId), useHTML, false);
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
	
	/**
	 * Will return a key of NULL for any units which we do not have art for.
	 */
	public static Map<PlayerID, List<UnitType>> getAllPlayerUnitsWithImages(final GameData data, final IUIContext uiContext, final boolean forceIncludeNeutralPlayer)
	{
		final LinkedHashMap<PlayerID, List<UnitType>> rVal = new LinkedHashMap<PlayerID, List<UnitType>>();
		data.acquireReadLock();
		try
		{
			for (final PlayerID p : data.getPlayerList().getPlayers())
			{
				rVal.put(p, getPlayerUnitsWithImages(p, data, uiContext));
			}
			final HashSet<UnitType> unitsSoFar = new HashSet<UnitType>();
			for (final List<UnitType> l : rVal.values())
			{
				unitsSoFar.addAll(l);
			}
			final Set<UnitType> all = data.getUnitTypeList().getAllUnitTypes();
			all.removeAll(unitsSoFar);
			if (forceIncludeNeutralPlayer || !all.isEmpty())
			{
				rVal.put(PlayerID.NULL_PLAYERID, getPlayerUnitsWithImages(PlayerID.NULL_PLAYERID, data, uiContext));
				unitsSoFar.addAll(rVal.get(PlayerID.NULL_PLAYERID));
				all.removeAll(unitsSoFar);
				if (!all.isEmpty())
				{
					rVal.put(null, new ArrayList<UnitType>(all));
				}
			}
		} finally
		{
			data.releaseReadLock();
		}
		return rVal;
	}
	
	public static List<UnitType> getPlayerUnitsWithImages(final PlayerID player, final GameData data, final IUIContext uiContext)
	{
		final ArrayList<UnitType> rVal = new ArrayList<UnitType>();
		data.acquireReadLock();
		try
		{
			// add first based on current production ability
			if (player.getProductionFrontier() != null)
			{
				for (final ProductionRule productionRule : player.getProductionFrontier())
				{
					for (final Entry<NamedAttachable, Integer> entry : productionRule.getResults().entrySet())
					{
						if (UnitType.class.isAssignableFrom(entry.getKey().getClass()))
						{
							final UnitType ut = (UnitType) entry.getKey();
							if (!rVal.contains(ut))
								rVal.add(ut);
						}
					}
				}
			}
			// this next part is purely to allow people to "add" neutral (null player) units to territories.
			// This is because the null player does not have a production frontier, and we also do not know what units we have art for, so only use the units on a map.
			for (final Territory t : data.getMap())
			{
				for (final Unit u : t.getUnits())
				{
					if (u.getOwner().equals(player))
					{
						final UnitType ut = u.getType();
						if (!rVal.contains(ut))
						{
							rVal.add(ut);
						}
					}
				}
			}
			// now check if we have the art for anything that is left
			for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes())
			{
				if (!rVal.contains(ut))
				{
					try
					{
						final UnitImageFactory imageFactory = uiContext.getUnitImageFactory();
						if (imageFactory != null)
						{
							final Image unitImage = imageFactory.getImage(ut, player, data, false, false);
							if (unitImage != null)
							{
								if (!rVal.contains(ut))
								{
									rVal.add(ut);
								}
							}
						}
					} catch (final Exception e)
					{ // ignore
					}
				}
			}
		} finally
		{
			data.releaseReadLock();
		}
		return rVal;
	}
}
