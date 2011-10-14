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
 * UnitSupportAttachmentExporter.java
 * 
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */

package games.strategy.engine.data.export;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.UnitType;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

public class UnitSupportAttachmentExporter extends DefaultAttachmentExporter
{
	@Override
	protected String printOption(Field field, IAttachment attachment) throws AttachmentExportException
	{
		String fieldName = field.getName();
		if (fieldName.equals("m_unitType"))
			return mUnitTypeHandler(field, attachment);
		if (fieldName.equals("m_players"))
			return mPlayersHandler(field, attachment);
		if (fieldName.equals("m_offence") || fieldName.equals("m_defence") || fieldName.equals("m_roll")
					|| fieldName.equals("m_strength") || fieldName.equals("m_allied") || fieldName.equals("m_enemy"))
			return "";
		return super.printOption(field, attachment);
	}
	
	private String mPlayersHandler(Field field, IAttachment attachment) throws AttachmentExportException
	{
		return printPlayerList(field, attachment);
	}
	
	@SuppressWarnings("unchecked")
	private String mUnitTypeHandler(Field field, IAttachment attachment) throws AttachmentExportException
	{
		try
		{
			Set<UnitType> unitTypes = (Set<UnitType>) field.get(attachment);
			Iterator<UnitType> iUnitTypes = unitTypes.iterator();
			String returnValue = "";
			if (iUnitTypes.hasNext())
				returnValue = iUnitTypes.next().getName();
			while (iUnitTypes.hasNext())
				returnValue = returnValue + ":" + iUnitTypes.next().getName();
			if (returnValue.length() > 0)
				return printDefaultOption("unitType", returnValue);
			return "";
		} catch (IllegalArgumentException e)
		{
			throw new AttachmentExportException("e: " + e + " for mUnitTypesHandler on field: " + field + "  on Attachment: " + attachment.getName());
		} catch (IllegalAccessException e)
		{
			throw new AttachmentExportException("e: " + e + " for mUnitTypesHandler on field: " + field + "  on Attachment: " + attachment.getName());
		}
		
	}
	
}
