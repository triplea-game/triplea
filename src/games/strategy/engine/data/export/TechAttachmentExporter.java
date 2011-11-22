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
 * TechAttachmentExporter.java
 * 
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */
package games.strategy.engine.data.export;

import games.strategy.engine.data.IAttachment;

import java.lang.reflect.Field;

public class TechAttachmentExporter extends DefaultAttachmentExporter
{
	@Override
	protected String printOption(final Field field, final IAttachment attachment) throws AttachmentExportException
	{
		final String fieldName = field.getName();
		if (fieldName.equals("m_GenericTech"))
			return ""; // GenericTech not set by XML
		// return mGenericTechHandler(field,attachment);
		return super.printOption(field, attachment);
	}
	
	@Override
	protected String printBooleanOption(final Field field, final String option, final IAttachment attachment) throws AttachmentExportException
	{
		return printBooleanOption(field, option, attachment, true);
	}
}
