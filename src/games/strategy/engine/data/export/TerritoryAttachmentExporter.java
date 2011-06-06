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
 * TerritoryAttachmentExporter.java
 *
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */

package games.strategy.engine.data.export;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import java.lang.reflect.Field;


public class TerritoryAttachmentExporter extends DefaultAttachmentExporter {	
	@Override
	protected String printOption(Field field, IAttachment attachment) throws AttachmentExportException {
		String fieldName = field.getName();
		if(fieldName.equals("m_changeUnitOwners"))
			return mChangeUnitOwnersHandler(field,attachment);
		if(fieldName.equals("m_captureUnitOnEnteringBy"))
			return mCaptureUnitOnEnteringByHandler(field,attachment);
		if(fieldName.equals("m_production")) // exception because gameParser breaks when production isn't set, even when it is default
			return super.printIntegerOption(field, "production", attachment, true);
		if(fieldName.equals("m_unitProduction")) // don't display unitProduction when production is the same.
			return mUnitProductionHandler(field,attachment);
		if(fieldName.equals("m_occupiedTerrOf"))
		return mOccupiedTerrOfHandler(field,attachment);
		if(fieldName.equals("m_originalOwner"))
			return "";
		
		return super.printOption(field, attachment);
	}

	
	/*private String mOriginalOwnerHandler(Field field, IAttachment attachment) {
		TerritoryAttachment att = (TerritoryAttachment) attachment;
		PlayerID originalOwner = att.getOriginalOwner();
		if(originalOwner == null)
			return "";
		return printDefaultOption("originalOwner", originalOwner.getName());
	}*/
	
	private String mOccupiedTerrOfHandler(Field field, IAttachment attachment) {
		TerritoryAttachment att = (TerritoryAttachment) attachment;
		PlayerID occupiedTerrOf = att.getOccupiedTerrOf();
		if(occupiedTerrOf == null)
			return "";
		return printDefaultOption("occupiedTerrOf", occupiedTerrOf.getName());
	}


	private String mUnitProductionHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		TerritoryAttachment att = (TerritoryAttachment) attachment;
		if(!(att.getProduction() == att.getUnitProduction()))
			return printIntegerOption(field, "unitProduction", attachment);
		return "";
	}

	private String mCaptureUnitOnEnteringByHandler(Field field, IAttachment attachment) throws AttachmentExportException {
			return printPlayerList(field,attachment);
	}
	
	private String mChangeUnitOwnersHandler(Field field, IAttachment attachment) throws AttachmentExportException {
		return printPlayerList(field,attachment);
	}

}
