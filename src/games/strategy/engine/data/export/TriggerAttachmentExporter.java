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
 * TriggerAttachmentExporter.java
 * 
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */
package games.strategy.engine.data.export;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.IntegerMap;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TriggerAttachmentExporter extends DefaultAttachmentExporter
{
	@Override
	protected String printOption(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		final String fieldName = field.getName();
		if (fieldName.equals("m_trigger"))
			return mTriggerHandler(field, attachment);
		if (fieldName.equals("m_frontier"))
			return mFrontierHandler(field, attachment);
		if (fieldName.equals("m_tech"))
			return mTechHandler(field, attachment);
		if (fieldName.equals("m_players"))
			return mPlayersHandler(field, attachment);
		if (fieldName.equals("m_support"))
			return mSupportHandler(field, attachment);
		if (fieldName.equals("m_purchase"))
			return mPurchaseHandler(field, attachment);
		if (fieldName.equals("m_placement"))
			return mPlacementHandler(field, attachment);
		if (fieldName.equals("m_unitProperty"))
			return mUnitPropertyHandler(field, attachment);
		if (fieldName.equals("m_availableTechs"))
			return mAvailableTechsHandler(field, attachment);
		return super.printOption(field, attachment);
	}
	
	@SuppressWarnings("unchecked")
	private String mAvailableTechsHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			final Map<String, Map<TechAdvance, Boolean>> availableTechsCategoryMap = (Map<String, Map<TechAdvance, Boolean>>) field.get(attachment);
			String returnValue = "";
			if (availableTechsCategoryMap == null)
				return "";
			final Iterator<String> categories = availableTechsCategoryMap.keySet().iterator();
			while (categories.hasNext())
			{
				final String category = categories.next();
				final Map<TechAdvance, Boolean> availableTechMap = availableTechsCategoryMap.get(category);
				final Iterator<TechAdvance> techAdvances = availableTechMap.keySet().iterator();
				String tList = "";
				while (techAdvances.hasNext())
				{
					final TechAdvance techAdvance = techAdvances.next();
					final String add = availableTechMap.get(techAdvance).booleanValue() ? "" : "-";
					tList = tList + ":" + add + techAdvance.getName();
				}
				returnValue += super.printDefaultOption("availableTech", category + tList);
			}
			return returnValue;
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mAvailableTechsHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mAvailableTechsHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
	
	@SuppressWarnings("unchecked")
	private String mUnitPropertyHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			String returnValue = "";
			final List<String> unitPropertyList = (List<String>) field.get(attachment);
			if (unitPropertyList == null)
				return "";
			for (final String unitProperty : unitPropertyList)
			{
				final String[] s = unitProperty.split(":");
				returnValue += super.printCountOption("unitProperty", s[0], s[1]);
			}
			return returnValue;
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mUnitPropertyHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mUnitPropertyHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
	
	@SuppressWarnings("unchecked")
	private String mPlacementHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			String returnValue = "";
			final Map<Territory, IntegerMap<UnitType>> placements = (Map<Territory, IntegerMap<UnitType>>) field.get(attachment);
			if (placements == null)
				return "";
			final Iterator<Territory> territories = placements.keySet().iterator();
			while (territories.hasNext())
			{
				final Territory territory = territories.next();
				final IntegerMap<UnitType> unitMap = placements.get(territory);
				final Iterator<UnitType> unitsOnTerritory = unitMap.keySet().iterator();
				while (unitsOnTerritory.hasNext())
				{
					final UnitType unit = unitsOnTerritory.next();
					final int number = unitMap.getInt(unit);
					returnValue += super.printCountOption("placement", territory.getName() + ":" + unit.getName(), "" + number);
				}
			}
			return returnValue;
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mPlacementHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mPlacementHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
	
	private String mPurchaseHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		return printUnitIntegerMap(field, attachment);
	}
	
	@SuppressWarnings("unchecked")
	private String mSupportHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			final Map<UnitSupportAttachment, Boolean> unitSupportAttachmentMap = (Map<UnitSupportAttachment, Boolean>) field.get(attachment);
			if (unitSupportAttachmentMap == null)
				return "";
			final Iterator<UnitSupportAttachment> unitSupportAttachments = unitSupportAttachmentMap.keySet().iterator();
			String returnValue = "";
			while (unitSupportAttachments.hasNext())
			{
				final UnitSupportAttachment supportAttachment = unitSupportAttachments.next();
				final String add = (unitSupportAttachmentMap.get(supportAttachment)).booleanValue() ? "" : "-";
				if (returnValue.length() > 0)
					returnValue += ":";
				returnValue = returnValue + add + supportAttachment.getName();
			}
			return printDefaultOption("support", returnValue);
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mSupportHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mSupportHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
	
	@SuppressWarnings("unchecked")
	private String mPlayersHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			final List<PlayerID> playerIds = (List<PlayerID>) field.get(attachment);
			final Iterator<PlayerID> iplayerIds = playerIds.iterator();
			String returnValue = "";
			if (iplayerIds.hasNext())
				returnValue = iplayerIds.next().getName();
			while (iplayerIds.hasNext())
			{
				returnValue += ":" + iplayerIds.next().getName();
			}
			if (returnValue.length() == 0)
				return "";
			return printDefaultOption("players", returnValue);
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mPlayersHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mPlayersHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
	
	@SuppressWarnings("unchecked")
	private String mTechHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			final List<TechAdvance> techAdvanceList = (List<TechAdvance>) field.get(attachment);
			final Iterator<TechAdvance> iTechAdvances = techAdvanceList.iterator();
			String returnValue = "";
			if (iTechAdvances.hasNext())
				returnValue = iTechAdvances.next().getName();
			while (iTechAdvances.hasNext())
			{
				returnValue += ":" + iTechAdvances.next().getName();
			}
			if (returnValue.length() == 0)
				return "";
			return printDefaultOption("tech", returnValue);
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mTechHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mTechHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
	
	private String mFrontierHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			final ProductionFrontier frontier = (ProductionFrontier) field.get(attachment);
			if (frontier == null)
				return "";
			return super.printDefaultOption("frontier", frontier.getName());
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mFrontierHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mFrontierHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
	
	@SuppressWarnings("unchecked")
	private String mTriggerHandler(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			final List<RulesAttachment> ruleAttachmentList = (List<RulesAttachment>) field.get(attachment);
			final Iterator<RulesAttachment> rules = ruleAttachmentList.iterator();
			String returnValue = "";
			if (rules.hasNext())
				returnValue = rules.next().getName();
			while (rules.hasNext())
			{
				final RulesAttachment rule = rules.next();
				returnValue += ":" + rule.getName();
			}
			return printDefaultOption("trigger", returnValue);
		} catch (final IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mTriggerHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		} catch (final IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mTriggerHandler on field: " + field.getName() + " on Attachment: " + attachment.getName());
		}
	}
}
