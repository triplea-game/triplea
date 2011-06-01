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
 * UnitAttachmentExporter.java
 *
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */

package games.strategy.engine.data.export;

import games.strategy.engine.data.IAttachment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class UnitAttachmentExporter extends DefaultAttachmentExporter {
	
	@Override
	protected String printOption(Field field, IAttachment attachment) throws AttachmentExportException {
		String fieldName = field.getName();
		if(fieldName.equals("m_requiresUnits"))
			return mRequiresUnitsHandler(field,attachment);
		if(fieldName.equals("m_canBeGivenByTerritoryTo"))
			return mCanBeGivenByTerritoryToHandler(field,attachment);
		if(fieldName.equals("m_destroyedWhenCapturedBy"))
			return mDestroyedWhenCapturedByHandler(field,attachment);
		if(fieldName.equals("m_givesMovement"))
			return mGivesMovementHandler(field,attachment);
		if(fieldName.equals("m_consumesUnits"))
			return consumesUnitsHandler(field,attachment);
		if(fieldName.equals("m_createsUnitsList"))
			return mCreatesUnitsListHandler(field,attachment);
		if(fieldName.equals("m_canBeCapturedOnEnteringBy"))
			return mCanBeCapturedOnEnteringByHandler(field,attachment);

		return super.printOption(field, attachment);
	}

	private String consumesUnitsHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		return printUnitIntegerMap(field,attachment);
	}

	private String mCanBeCapturedOnEnteringByHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		return printPlayerList(field,attachment);
	}

	private String mCreatesUnitsListHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		return printUnitIntegerMap(field,attachment);

	}

	private String mGivesMovementHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		return printUnitIntegerMap(field,attachment);

	}

	private String mDestroyedWhenCapturedByHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		return printPlayerList(field,attachment);
	}

	private String mCanBeGivenByTerritoryToHandler(Field field,	IAttachment attachment) throws AttachmentExportException {
		return printPlayerList(field,attachment);
	}

	@SuppressWarnings("unchecked")
	private String mRequiresUnitsHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		try {
			ArrayList<String[]> requiresUnitListList = (ArrayList<String[]>) field.get(attachment);
			Iterator<String[]> iRequiresListList = requiresUnitListList.iterator();
			String returnValue = "";
			while(iRequiresListList.hasNext()) {
				Iterator<String> iRequiresList = Arrays.asList(iRequiresListList.next()).iterator();
				String value = iRequiresList.next();
				while(iRequiresList.hasNext())
					value = value + ":" + iRequiresList.next();
				returnValue = returnValue + printDefaultOption("requiresUnits",value);
			}
			return returnValue;
		} catch (IllegalArgumentException e) {
			 throw new AttachmentExportException("e: "+e+" for mRequiresUnitsHandler on field: "+field.getName()+" on Attachment: "+attachment.getName());
		} catch (IllegalAccessException e) {
			 throw new AttachmentExportException("e: "+e+" for mRequiresUnitsHandler on field: "+field.getName()+" on Attachment: "+attachment.getName());
		}
	}


}
