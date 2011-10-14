/**
 * 
 * @author Edwin van der Wal
 * @version 0.1
 * 
 *          A Type of Relationship between PlayerIDs
 */

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

package games.strategy.engine.data;

import games.strategy.triplea.attatchments.RelationshipTypeAttachment;

import java.io.Serializable;

public class RelationshipType extends NamedAttachable implements Serializable
{
	
	/**
	 * create new RelationshipType
	 * 
	 * @param name
	 *            name of the relationshipType
	 * @param data
	 *            GameData Object used for construction
	 */
	public RelationshipType(String name, GameData data)
	{
		super(name, data);
	}
	
	/**
	 * convenience method to get the relationshipTypeAttachment of this relationshipType
	 * 
	 * @return the relationshipTypeAttachment of this relationshipType
	 */
	public RelationshipTypeAttachment getRelationshipTypeAttachment()
	{
		return RelationshipTypeAttachment.get(this);
	}
	
	@Override
	public String toString()
	{
		return this.getName();
	}
	
}
