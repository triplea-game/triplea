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
 * TerritoryAttachmentExporter.java
 * 
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */

package games.strategy.engine.data.export;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attatchments.TerritoryAttachment;

import java.lang.reflect.Field;

public class TerritoryAttachmentExporter extends DefaultAttachmentExporter
{
	
	@Override
	protected String printOption(Field field, IAttachment attachment) throws AttachmentExportException
	{
		String fieldName = field.getName();
		if (fieldName.equals("m_changeUnitOwners"))
			return mChangeUnitOwnersHandler(field, attachment);
		if (fieldName.equals("m_captureUnitOnEnteringBy"))
			return mCaptureUnitOnEnteringByHandler(field, attachment);
		if (fieldName.equals("m_production")) // exception because gameParser breaks when production isn't set, even when it is default
			return super.printIntegerOption(field, "production", attachment, true);
		if (fieldName.equals("m_unitProduction")) // don't display unitProduction when production is the same.
			return mUnitProductionHandler(field, attachment);
		if (fieldName.equals("m_originalOwner")) // it doesn't matter if we look at m_occupiedTerrOf or m_originalOwner, as they are both the same thing
			return mOccupiedTerrOfHandler(field, attachment);
		if (fieldName.equals("m_occupiedTerrOf"))
			return ""; // we want to skip this, since we already check this when we look ad original owner.
			
		return super.printOption(field, attachment);
	}
	
	private String mOccupiedTerrOfHandler(Field field, IAttachment attachment)
	{
		TerritoryAttachment att = (TerritoryAttachment) attachment;
		PlayerID occupiedTerrOf = att.getOccupiedTerrOf();
		if (occupiedTerrOf != null)
			return printDefaultOption("occupiedTerrOf", occupiedTerrOf.getName()); // if we have a specific occupiedTerrOf then use it
			
		// otherwise, check to see if someone else has conquered the territory in the mean time. must check for neutrals too. neutrals can be either NULL or PlayerID.NULL_PLAYERID
		// we only want to add an occupiedTerrOf IF the current owner does not equal the original owner. Since any time they are equal, this is not needed.
		PlayerID originalOwner = att.getOriginalOwner();
		Territory t = (Territory) att.getAttatchedTo();
		if (originalOwner == null && (t.getOwner() != null && !t.getOwner().equals(PlayerID.NULL_PLAYERID)))
			return printDefaultOption("occupiedTerrOf", PlayerID.NULL_PLAYERID.getName());
		else if (originalOwner == null)
			return ""; // must be that original owner and current owner are both null
			
		if (!originalOwner.equals(t.getOwner()))
			return printDefaultOption("occupiedTerrOf", originalOwner.getName());
		
		return "";
	}
	
	private String mUnitProductionHandler(Field field, IAttachment attachment) throws AttachmentExportException
	{
		TerritoryAttachment att = (TerritoryAttachment) attachment;
		if (!(att.getProduction() == att.getUnitProduction()))
			return printIntegerOption(field, "unitProduction", attachment, true);
		return "";
	}
	
	private String mCaptureUnitOnEnteringByHandler(Field field, IAttachment attachment) throws AttachmentExportException
	{
		return printPlayerList(field, attachment);
	}
	
	private String mChangeUnitOwnersHandler(Field field, IAttachment attachment) throws AttachmentExportException
	{
		return printPlayerList(field, attachment);
	}
	
}
